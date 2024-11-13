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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.azureoauth.configuration.AzureConfiguration;
import com.xwiki.azureoauth.internal.configuration.AzureADConfigurationSource;
import com.xwiki.azureoauth.internal.oldConfiguration.OldOAuthAzureConfiguration;

import static com.xwiki.azureoauth.internal.configuration.AzureADConfigurationSource.CONFIG_DOC;

/**
 * Checks the current installation version and transfers the old configuration from Identity OAuth to the new
 * configuration class from OIDC. Listens to the modification made on the Azure AD configuration to update the OIDC
 * client configuration.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named(AzureADUpgradingListener.HINT)
@Singleton
public class AzureADUpgradingListener extends AbstractEventListener implements Initializable
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "AzureADUpgradingListener";

    @Inject
    private Logger logger;

    @Inject
    private Provider<AzureConfiguration> azureClientConfigurationProvider;

    @Inject
    @Named(OldOAuthAzureConfiguration.HINT)
    private Provider<AzureConfiguration> oauthConfigurationProvider;

    @Inject
    @Named(AzureADConfigurationSource.HINT)
    private ConfigurationSource azureConfiguration;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private InstalledExtensionRepository installedRepository;

    /**
     * Default constructor.
     */
    public AzureADUpgradingListener()
    {
        super(HINT, new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentUpdatedEvent || event instanceof DocumentDeletedEvent) {
            XWikiDocument document = (XWikiDocument) source;
            if (document != null && isAzureConfigObject(document)) {
                try {
                    String oldTenantID = azureClientConfigurationProvider.get().getTenantID();
                    String tenantID = azureConfiguration.getProperty("tenantID", "");
                    if (!tenantID.equals(oldTenantID)) {
                        azureClientConfigurationProvider.get().setOIDCConfiguration(getEndpointsMap(tenantID));
                    }
                } catch (ConfigurationSaveException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * If the new version of the API module of Integration Azure consists with the first version of OIDC integration,
     * transfer the old configurations to the new OIDC client configuration class.
     *
     * @throws InitializationException if any error occurs.
     */
    @Override
    public void initialize() throws InitializationException
    {
        try {
            InstalledExtension apiModule = installedRepository.getInstalledExtension(
                new ExtensionId("com.xwiki.integration-azure-oauth:integration-azure-oauth-api", "2.0"));
            if (apiModule != null) {
                azureClientConfigurationProvider.get().setOIDCConfiguration(generateNewConfigurationMap());
            }
        } catch (Exception e) {
            String rootCause = ExceptionUtils.getRootCauseMessage(e);
            logger.error("There was an error while trying to migrate the old Identity OAuth configuration to OIDC "
                + "configuration. Root cause is: [{}]", rootCause);
            throw new InitializationException(rootCause, e);
        }
    }

    private boolean isAzureConfigObject(XWikiDocument doc)
    {
        DocumentReference configReference = new DocumentReference(CONFIG_DOC, this.getCurrentWikiReference());
        return Objects.equals(doc.getDocumentReference(), configReference);
    }

    private WikiReference getCurrentWikiReference()
    {
        return new WikiReference(this.wikiManager.getCurrentWikiId());
    }

    private Map<String, Object> generateNewConfigurationMap()
    {
        AzureConfiguration oauthConfiguration = oauthConfigurationProvider.get();
        String tenantID = oauthConfiguration.getTenantID();
        Map<String, Object> newConfig = new HashMap<>(getEndpointsMap(tenantID));
        newConfig.put("clientId", oauthConfiguration.getClientID());
        newConfig.put("clientSecret", oauthConfiguration.getSecret());
        newConfig.put("skipped", !oauthConfiguration.isActive());
        return newConfig;
    }

    private Map<String, Object> getEndpointsMap(String tenantID)
    {
        return Map.of("authorizationEndpoint", getFormattedEndpoint(tenantID, "authorize"), "tokenEndpoint",
            getFormattedEndpoint(tenantID, "token"), "logoutEndpoint", getFormattedEndpoint(tenantID, "logout"));
    }

    private String getFormattedEndpoint(String tenantID, String scope)
    {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/%s", tenantID, scope);
    }
}
