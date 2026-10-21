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

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.oidc.auth.store.OIDCClientConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

import static com.xwiki.azureoauth.internal.configuration.EntraIDConfigurationSource.CONFIG_CLASS;
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

    private static final EntityReference CLASS_MATCHER =
        BaseObjectReference.any("EntraID.Code" + ".EntraIDConfigurationClass");

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
        super(HINT, new XObjectAddedEvent(CLASS_MATCHER), new XObjectUpdatedEvent(CLASS_MATCHER));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        if (document == null) {
            return;
        }
        if (isEntraIDConfigObject(document)) {
            if (event instanceof XObjectUpdatedEvent) {
                updateDefaultConfiguration();
            }
        } else {
            updateClientConfiguration(document);
        }
    }

    private void updateDefaultConfiguration()
    {
        try {
            EntraIDConfiguration entraIDConfiguration = entraIDConfigurationProvider.get();
            String oldTenantID = entraIDConfiguration.getOIDCTenantID();
            String tenantID = entraIDConfiguration.getTenantID();
            if (!tenantID.equals(oldTenantID)) {
                entraIDConfiguration.setOIDCConfiguration(azureOIDCMigratorProvider.get().getEndpoints(tenantID));
            }
        } catch (ConfigurationSaveException e) {
            logger.error("There was an error while trying to update OIDC endpoints. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * Any document holding both an Entra ID configuration object and an OIDC client configuration object is an Entra
     * ID client configuration, whose endpoints are derived from its tenant.
     */
    private void updateClientConfiguration(XWikiDocument document)
    {
        BaseObject entraIDObject = document.getXObject(CONFIG_CLASS);
        BaseObject oidcObject = document.getXObject(OIDCClientConfiguration.CLASS_REFERENCE);
        if (entraIDObject == null || oidcObject == null) {
            return;
        }
        String tenantID = entraIDObject.getStringValue("tenantId");
        if (StringUtils.isBlank(tenantID)) {
            return;
        }
        Map<String, Object> endpoints = azureOIDCMigratorProvider.get().getEndpoints(tenantID);
        if (Objects.equals(endpoints.get(OIDCClientConfiguration.FIELD_PROVIDER),
            oidcObject.getStringValue(OIDCClientConfiguration.FIELD_PROVIDER)))
        {
            return;
        }
        XWikiContext xcontext = xcontextProvider.get();
        try {
            // The event source must not be modified, so the stored document is updated instead.
            XWikiDocument documentToSave =
                xcontext.getWiki().getDocument(document.getDocumentReference(), xcontext).clone();
            BaseObject oidcObjectToSave = documentToSave.getXObject(OIDCClientConfiguration.CLASS_REFERENCE);
            endpoints.forEach((property, value) -> oidcObjectToSave.set(property, value, xcontext));
            xcontext.getWiki().saveDocument(documentToSave, "Updated the OIDC endpoints of the Entra ID tenant.",
                true, xcontext);
        } catch (XWikiException e) {
            logger.error("Failed to update the OIDC endpoints of the Entra ID client configuration [{}]. "
                + "Root cause is: [{}]", document.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            AzureADOIDCMigrator azureOIDCMigrator = azureOIDCMigratorProvider.get();
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
}
