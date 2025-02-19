/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.azureoauth.internal;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.security.authservice.XWikiAuthServiceComponent;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

import static com.xwiki.azureoauth.internal.configuration.EntraIDConfigurationSource.CONFIG_DOC;

/**
 * Checks the current installation version and transfers the old configuration from Identity OAuth to the new
 * configuration class from OIDC. Listens to the modification made on the Entra ID configuration to update the OIDC
 * client configuration.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named(EntraIDObjectUpdateListener.HINT)
@Singleton
@Unstable
public class EntraIDObjectUpdateListener extends AbstractEventListener implements Initializable
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "EntraIDObjectUpdateListener";

    private static final EntityReference CLASS_MATCHER = BaseObjectReference.any("EntraID.Code"
        + ".EntraIDConfigurationClass");

    private static final String DEFAULT_ID = "default";

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private Logger logger;

    @Inject
    @Named("default")
    private Provider<EntraIDConfiguration> entraIDConfigurationProvider;

    @Inject
    private Provider<AzureADOIDCMigrator> azureOIDCMigratorProvider;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public EntraIDObjectUpdateListener()
    {
        super(HINT, new XObjectUpdatedEvent(CLASS_MATCHER));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof XObjectUpdatedEvent) {
            XWikiDocument document = (XWikiDocument) source;
            if (document != null && isEntraIDConfigObject(document)) {
                try {
                    EntraIDConfiguration entraIDConfiguration = entraIDConfigurationProvider.get();
                    String oldTenantID = entraIDConfiguration.getOIDCTenantID();
                    String tenantID = entraIDConfiguration.getTenantID();
                    if (!tenantID.equals(oldTenantID)) {
                        entraIDConfiguration.setOIDCConfiguration(
                            azureOIDCMigratorProvider.get().getEndpoints(tenantID));
                    }
                } catch (ConfigurationSaveException e) {
                    logger.error("There was an error while trying to update OIDC endpoints. Root cause is: [{}]",
                        ExceptionUtils.getRootCauseMessage(e));
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            AzureADOIDCMigrator azureOIDCMigrator = azureOIDCMigratorProvider.get();
            XWiki xwiki = getXWiki();

            // XWiki might not be fully initialized yet in which case it means we are not installing or reloading the
            // extension. To be removed when upgrading the XWiki parent to a version >= 15.3.
            if (xwiki != null) {
                resetAuthService(xwiki);
            }
            azureOIDCMigrator.initializeOIDCConfiguration();
            azureOIDCMigrator.refactorOIDCIssuer();
        } catch (XWikiException | QueryException e) {
            String rootCause = ExceptionUtils.getRootCauseMessage(e);
            logger.error(
                "There was an error while trying to refactor the OIDC class for old AzureAD users. Root cause is: [{}]",
                rootCause);
            throw new InitializationException(rootCause, e);
        } catch (ConfigurationSaveException e) {
            String rootCause = ExceptionUtils.getRootCauseMessage(e);
            logger.error("There was an error while trying to migrate the old Identity OAuth configuration to OIDC "
                + "configuration. Root cause is: [{}]", rootCause);
            throw new InitializationException(rootCause, e);
        } catch (Exception e) {
            String rootCause = ExceptionUtils.getRootCauseMessage(e);
            logger.error("There was an error while initializing the listener. Root cause is: [{}]", rootCause);
            throw new InitializationException(rootCause, e);
        }
    }

    private boolean isEntraIDConfigObject(XWikiDocument doc)
    {
        DocumentReference configReference = new DocumentReference(CONFIG_DOC, this.getCurrentWikiReference());
        return Objects.equals(doc.getDocumentReference(), configReference);
    }

    private WikiReference getCurrentWikiReference()
    {
        return new WikiReference(this.wikiManager.getCurrentWikiId());
    }

    /**
     * Prior to XWiki 15.3, to be able to select the Authentication service by using the AuthService Backport, the
     * default XWikiAuthServiceComponent needs to be selected. To do this, if the selected authentication service is
     * externally set, we reset it to default one, with the exception if the authentication service is set in xwiki.cfg.
     * To be removed when upgrading the parent to a version >= 15.3. https://jira.xwiki.org/browse/XWIKI-20548
     */
    private void resetAuthService(XWiki xwiki) throws ComponentLookupException
    {
        // Check if an authenticator class is explicitly set (in which case we don't want to override it).
        String authServiceClass = this.xwikicfg.getProperty("xwiki.authentication.authclass");
        if (authServiceClass == null) {
            if (xwiki.getAuthService() instanceof XWikiAuthServiceComponent) {
                XWikiAuthServiceComponent currentAuth = (XWikiAuthServiceComponent) xwiki.getAuthService();
                if (Objects.equals(currentAuth.getId(), "oidc") || Objects.equals(currentAuth.getId(), DEFAULT_ID)) {
                    return;
                }
            }
            registerDefaultService(xwiki);
        }
    }

    /**
     * To be removed when upgrading the XWiki parent to a version >= 15.3.
     */
    private void registerDefaultService(XWiki xwiki) throws ComponentLookupException
    {
        // Reset the cached auth service so that it's released next time.
        xwiki.setAuthService(null);

        boolean hasDefaultAuthService = componentManagerProvider.get().hasComponent(XWikiAuthServiceComponent.class);
        if (hasDefaultAuthService) {
            List<XWikiAuthServiceComponent> authServicesList =
                this.componentManagerProvider.get().getInstanceList(XWikiAuthServiceComponent.class);
            for (XWikiAuthServiceComponent authServiceComponent : authServicesList) {
                // Register the bridge as authenticator.
                if (authServiceComponent.getId().equals(DEFAULT_ID)) {
                    xwiki.setAuthService(authServiceComponent);
                    break;
                }
            }
        }
    }

    private XWiki getXWiki()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext != null ? xcontext.getWiki() : null;
    }
}
