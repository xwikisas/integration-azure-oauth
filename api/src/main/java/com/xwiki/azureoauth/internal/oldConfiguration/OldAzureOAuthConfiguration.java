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
package com.xwiki.azureoauth.internal.oldConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

import com.xwiki.azureoauth.configuration.AzureOldConfiguration;

/**
 * Old AzureAD configuration properties from Identity OAuth integration.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Singleton
@Named(OldAzureOAuthConfiguration.HINT)
@Unstable
public class OldAzureOAuthConfiguration implements AzureOldConfiguration
{
    /**
     * Component hint.
     */
    public static final String HINT = "AZURE_OAUTH_OLD_CONFIGURATION";

    @Inject
    @Named(OldAzureConfigurationSource.HINT)
    private ConfigurationSource azureConfiguration;

    @Inject
    @Named(OldIdentityOAuthConfigurationSource.HINT)
    private ConfigurationSource oauthConfiguration;

    @Override
    public String getTenantID()
    {
        return azureConfiguration.getProperty("tenantid", "");
    }

    @Override
    public String getClientID()
    {
        return oauthConfiguration.getProperty("clientid", "");
    }

    @Override
    public String getSecret()
    {
        return oauthConfiguration.getProperty("secret", "");
    }

    @Override
    public String getScope()
    {
        return oauthConfiguration.getProperty("scope", "openid,User.Read");
    }

    @Override
    public boolean isActive()
    {
        return oauthConfiguration.getProperty("active", true);
    }
}
