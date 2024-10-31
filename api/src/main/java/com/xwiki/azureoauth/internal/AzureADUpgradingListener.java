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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionUpgradingEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.azureoauth.configuration.AzureConfiguration;
import com.xwiki.azureoauth.internal.oldConfiguration.OldOAuthAzureConfiguration;

/**
 * Checks the current installation version and transfers the old configuration from Identity OAuth to the new
 * configuration class from OIDC.
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
    private InstalledExtensionRepository installedRepository;

    /**
     * Default constructor.
     */
    public AzureADUpgradingListener()
    {
        super(HINT, new ExtensionUpgradingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {

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
                azureClientConfigurationProvider.get().setConfiguration(generateNewConfigurationMap());
            }
        } catch (Exception e) {
            String rootCause = ExceptionUtils.getRootCauseMessage(e);
            logger.error("There was an error while trying to migrate the old Identity OAuth configuration to OIDC "
                + "configuration. Root cause is: [{}]", rootCause);
            throw new InitializationException(rootCause, e);
        }
    }

    private Map<String, Object> generateNewConfigurationMap()
    {
        Map<String, Object> newConfig = new HashMap<>();
        AzureConfiguration oauthConfiguration = oauthConfigurationProvider.get();
        String tenantID = oauthConfiguration.getTenantID();
        newConfig.put("authorizationEndpoint", getFormattedEndpoint(tenantID, "authorize"));
        newConfig.put("tokenEndpoint", getFormattedEndpoint(tenantID, "token"));
        newConfig.put("logoutEndpoint", getFormattedEndpoint(tenantID, "logout"));
        newConfig.put("clientId", oauthConfiguration.getClientID());
        newConfig.put("clientSecret", oauthConfiguration.getSecret());
        newConfig.put("skipped", !oauthConfiguration.isActive());
        return newConfig;
    }

    private String getFormattedEndpoint(String tenantID, String scope)
    {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/%s", tenantID, scope);
    }
}
