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
package com.xwiki.azureoauth.internal.configuration;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

/**
 * Default implementation of {@link EntraIDConfiguration}.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Singleton
@Unstable
public class DefaultEntraIDConfiguration implements EntraIDConfiguration
{
    /**
     * OIDC user class reference.
     */
    public static final String OIDC_USER_CLASS = "XWiki.OIDC.UserClass";

    @Inject
    @Named(OIDCClientConfigurationSource.HINT)
    private ConfigurationSource oidcConfiguration;

    @Inject
    @Named(EntraIDConfigurationSource.HINT)
    private ConfigurationSource entraIDConfiguration;

    @Override
    public void setOIDCConfiguration(Map<String, Object> properties) throws ConfigurationSaveException
    {
        this.oidcConfiguration.setProperties(properties);
    }

    @Override
    public void setEntraIDConfiguration(Map<String, Object> properties) throws ConfigurationSaveException
    {
        this.entraIDConfiguration.setProperties(properties);
    }

    @Override
    public String getClientID()
    {
        return this.oidcConfiguration.getProperty("clientId", "");
    }

    @Override
    public String getSecret()
    {
        return this.oidcConfiguration.getProperty("clientSecret", "");
    }

    @Override
    public String getScope()
    {
        return this.oidcConfiguration.getProperty("scope", "");
    }

    @Override
    public boolean isActive()
    {
        return !this.oidcConfiguration.getProperty("skipped", false);
    }

    @Override
    public String getOIDCTenantID()
    {
        // In the OIDC Client Configuration for Entra ID, the tenant ID is embedded in the 'authorizationEndpoint' URL.
        // To extract it, a regex is needed to select only the specific part of the URL that corresponds to the
        // tenant ID.
        String endpoint = this.oidcConfiguration.getProperty("authorizationEndpoint", "");
        if (!endpoint.isEmpty()) {
            Pattern pattern = Pattern.compile("com/([^/]+)/oauth2/v2");
            Matcher matcher = pattern.matcher(endpoint);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    @Override
    public boolean isXWikiLoginGlobalEnabled()
    {
        return this.entraIDConfiguration.getProperty("enableXWikiLoginGlobal", true);
    }

    @Override
    public String getTenantID()
    {
        return this.entraIDConfiguration.getProperty("tenantId", "");
    }

    @Override
    public String getXWikiLoginGroups()
    {
        return this.entraIDConfiguration.getProperty("xwikiLoginGroups", "");
    }
}
