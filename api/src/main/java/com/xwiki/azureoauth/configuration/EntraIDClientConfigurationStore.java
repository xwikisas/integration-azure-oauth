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
package com.xwiki.azureoauth.configuration;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Retrieve the Entra ID client configurations of the current wiki. Besides the default configuration, provided by the
 * application, any document holding both an OIDC client configuration object and an Entra ID configuration object is
 * an Entra ID client configuration.
 *
 * @version $Id$
 * @since 2.2.5
 */
@Role
@Unstable
public interface EntraIDClientConfigurationStore
{
    /**
     * @return the configuration provided by the application, which exists even if it was never filled
     * @throws Exception if the configuration document can't be loaded
     */
    EntraIDClientConfiguration getDefaultConfiguration() throws Exception;

    /**
     * Only the document that the OIDC authenticator uses for the given configuration name is taken into account, since
     * it is the one trusted to provide that configuration.
     *
     * @param configurationName the name of the OIDC client configuration
     * @return the Entra ID client configuration with the given name, or {@code null} if there is no such OIDC client
     *     configuration or if it doesn't have an Entra ID configuration object
     * @throws Exception if the configuration can't be retrieved
     */
    EntraIDClientConfiguration getConfiguration(String configurationName) throws Exception;

    /**
     * @return the Entra ID client configurations of the current wiki
     * @throws Exception if the configurations can't be retrieved
     */
    List<EntraIDClientConfiguration> getConfigurations() throws Exception;
}
