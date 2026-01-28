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

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Expose the EntraID users which are already present on the XWiki instance, or the list which is provided by the
 * EntraID server.
 *
 * @version $Id$
 * @since 2.1
 */
@Role
@Unstable
public interface EntraIDUsersManager
{
    /**
     * Get a {@link Map} of internal users that are created from EntraId, with the user id (subject) as the key.
     *
     * @return the internal users that are created from EntraId
     * @throws Exception if there are any errors while getting the users.
     */
    Map<String, XWikiDocument> getXWikiUsersMap() throws Exception;

    /**
     * Get a {@link List} of internal users that are created from EntraId.
     *
     * @return a {@link List} of the user documents
     * @throws XWikiException if there is any error while retrieving the documents
     * @throws QueryException if the query execution fails
     */
    List<XWikiDocument> getXWikiUsers() throws XWikiException, QueryException;

    /**
     * Get a {@link List} of external users from EntraId.
     *
     * @return the external users info stored in {@link ExternalUser}
     * @throws Exception if there are any errors while getting the users.
     */
    List<ExternalUser> getEntraServerUsers() throws Exception;
}
