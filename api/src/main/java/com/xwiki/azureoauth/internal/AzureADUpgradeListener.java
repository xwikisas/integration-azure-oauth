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
import org.xwiki.extension.event.ExtensionUpgradingEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xwiki.azureoauth.configuration.AzureConfiguration;
import com.xwiki.azureoauth.internal.oldConfiguration.DefaultOldOAuthAzureConfiguration;

@Component
@Named(AzureADUpgradeListener.HINT)
@Singleton
public class AzureADUpgradeListener extends AbstractEventListener implements Initializable
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "AzureADUpgradeListener";

    @Inject
    private Logger logger;

    @Inject
    private Provider<AzureConfiguration> azureClientConfigurationProvider;

    @Inject
    @Named(DefaultOldOAuthAzureConfiguration.HINT)
    private Provider<AzureConfiguration> oauthConfigurationProvider;

    public AzureADUpgradeListener()
    {
        super(HINT, new ExtensionUpgradingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {

    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            azureClientConfigurationProvider.get().setConfiguration(generateNewConfigurationMap());
        } catch (Exception e) {
            throw new InitializationException(ExceptionUtils.getRootCauseMessage(e), e);
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
        newConfig.put("scope", oauthConfiguration.getScope().replace(" ", ","));
        newConfig.put("skipped", !oauthConfiguration.isActive());
        return newConfig;
    }

    private String getFormattedEndpoint(String tenantID, String scope)
    {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/%s", tenantID, scope);
    }
}
