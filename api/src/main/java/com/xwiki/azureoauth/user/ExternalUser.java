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
package com.xwiki.azureoauth.user;

import org.xwiki.stability.Unstable;

/**
 * Object class used to store Entra Id users info.
 *
 * @version $Id$
 * @since 2.1
 */
@Unstable
public class ExternalUser
{
    private String id;

    private boolean isEnabled;

    /**
     * Parameters constructor.
     *
     * @param id the id of the external user
     * @param isEnabled {@code true} if the external user is enabled, or {@code false} otherwise
     */
    public ExternalUser(String id, boolean isEnabled)
    {
        this.id = id;
        this.isEnabled = isEnabled;
    }

    /**
     * Get the user id.
     *
     * @return the user id
     */
    public String getId()
    {
        return id;
    }

    /**
     * {@link #getId()}.
     *
     * @param id the user id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Check if the user is enabled or not.
     *
     * @return {@code true} if the external user is enabled, or {@code false} otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * {@link #isEnabled()}.
     *
     * @param enabled {@code true} if the external user is enabled, or {@code false} otherwise
     */
    public void setEnabled(boolean enabled)
    {
        isEnabled = enabled;
    }
}
