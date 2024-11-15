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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * AzureAD old configuration.
 *
 * @version $Id$
 * @since 2.0
 */
@Role
@Unstable
public interface AzureOldConfiguration
{
    /**
     * Return the tenant ID.
     *
     * @return the tenant ID.
     */
    String getTenantID();

    /**
     * Return the client ID.
     *
     * @return the client ID.
     */
    String getClientID();

    /**
     * Return the authentication secret.
     *
     * @return the authentication secret.
     */
    String getSecret();

    /**
     * Return the provider scope.
     *
     * @return the provider scope.
     */
    String getScope();

    /**
     * See if the provider configuration is active.
     *
     * @return {@code true} if the configuration is active, or {@code false} otherwise.
     */
    boolean isActive();
}
