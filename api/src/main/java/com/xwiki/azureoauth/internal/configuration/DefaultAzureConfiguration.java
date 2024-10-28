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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

import com.xwiki.azureoauth.configuration.AzureConfiguration;

@Component
@Singleton
@Unstable
public class DefaultAzureConfiguration implements AzureConfiguration
{
    @Inject
    @Named(OIDCAzureClientConfigurationSource.HINT)
    private ConfigurationSource mainConfiguration;

    @Override
    public void setConfiguration(Map<String, Object> properties) throws ConfigurationSaveException
    {
        this.mainConfiguration.setProperties(properties);
    }

    @Override
    public String getTenantID()
    {
        return "";
    }

    @Override
    public String getClientID()
    {
        return this.mainConfiguration.getProperty("clientId", "");
    }

    @Override
    public String getSecret()
    {
        return this.mainConfiguration.getProperty("clientSecret", "");
    }

    @Override
    public String getScope()
    {
        return this.mainConfiguration.getProperty("scope", "");
    }

    @Override
    public boolean isActive()
    {
        return !this.mainConfiguration.getProperty("skipped", false);
    }
}
