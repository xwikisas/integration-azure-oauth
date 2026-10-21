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
package com.xwiki.azureoauth.user.sync;

import java.util.List;

import org.xwiki.job.AbstractRequest;
import org.xwiki.stability.Unstable;

/**
 * Represents a request to start a EntraID users sync job.
 *
 * @version $Id$
 * @since 2.1
 */
@Unstable
public class EntraIDUsersSyncJobRequest extends AbstractRequest
{
    private boolean disable;

    private boolean remove;

    private String configurationName;

    /**
     * Default constructor.
     */
    public EntraIDUsersSyncJobRequest()
    {
        setDefaultId();
    }

    /**
     * Creates a specific request for users sync job.
     *
     * @param jobId the ID of the request.
     * @param disable {@code true} if the sync should also sync disabled users, or {@code false} otherwise
     * @param remove {@code true} if the sync should also sync removed users, or {@code false} otherwise
     */
    public EntraIDUsersSyncJobRequest(List<String> jobId, boolean disable, boolean remove)
    {
        setId(jobId);
        this.disable = disable;
        this.remove = remove;
    }

    /**
     * Creates a request for syncing the users of a specific Entra ID client configuration.
     *
     * @param jobId the ID of the request.
     * @param configurationName the name of the Entra ID client configuration whose users are synced, or {@code null}
     *     for the default configuration
     * @param disable {@code true} if the sync should also sync disabled users, or {@code false} otherwise
     * @param remove {@code true} if the sync should also sync removed users, or {@code false} otherwise
     * @since 2.3.0
     */
    public EntraIDUsersSyncJobRequest(List<String> jobId, String configurationName, boolean disable, boolean remove)
    {
        this(jobId, disable, remove);
        this.configurationName = configurationName;
    }

    /**
     * @return the name of the Entra ID client configuration whose users are synced, or {@code null} for the default
     *     configuration
     * @since 2.3.0
     */
    public String getConfigurationName()
    {
        return configurationName;
    }

    /**
     * @return {@code true} if disabled users from Entra ID should be synced, or {@code false} otherwise.
     */
    public boolean shouldDisable()
    {
        return disable;
    }

    /**
     * @return {@code true} if removed users from Entra ID should be synced, or {@code false} otherwise.
     */
    public boolean shouldRemove()
    {
        return remove;
    }

    private void setDefaultId()
    {
        setId(List.of("entra", "users", "sync"));
    }
}
