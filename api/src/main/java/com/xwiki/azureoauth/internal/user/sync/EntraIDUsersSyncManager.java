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
package com.xwiki.azureoauth.internal.user.sync;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.user.EntraIDUsersManager;
import com.xwiki.azureoauth.user.ExternalUser;

/**
 * Manage the sync between Entra ID users and XWiki users.
 *
 * @version $Id$
 * @since 2.1
 */
@Component(roles = EntraIDUsersSyncManager.class)
@Singleton
public class EntraIDUsersSyncManager
{
    private static final String USER_CLASS = "XWiki.XWikiUsers";

    private static final String SAVE_MESSAGE = "Disable user during EntraID user synchronization";

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntraIDUsersManager usersManager;

    /**
     * Synchronize the state of the XWiki users created from Entra ID with their state from the Microsoft server.
     *
     * @param disable {@code true} if the sync should also sync disabled users, or {@code false} otherwise
     * @param remove {@code true} if the sync should also sync removed users, or {@code false} otherwise
     * @throws Exception if any error occurs during the sync
     */
    public void syncUsers(boolean disable, boolean remove) throws Exception
    {
        Map<String, XWikiDocument> usersMap = usersManager.getXWikiUsersMap();
        List<ExternalUser> externalUsers = usersManager.getEntraServerUsers();
        // We index the values in a map to improve performance.
        Map<String, ExternalUser> externalUsersById = externalUsers.stream()
            .collect(Collectors.toMap(ExternalUser::getId, Function.identity()));
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        for (Map.Entry<String, XWikiDocument> entry : usersMap.entrySet()) {
            XWikiDocument userDoc = entry.getValue();
            String subject = entry.getKey();
            ExternalUser externalUser = externalUsersById.get(subject);
            if (externalUser == null) {
                if (remove) {
                    wiki.deleteDocument(userDoc, wikiContext);
                }
                continue;
            }
            if (disable && !externalUser.isEnabled()) {
                BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(USER_CLASS));
                oidcObj.set("active", 0, wikiContext);
                wiki.saveDocument(userDoc, SAVE_MESSAGE, wikiContext);
            }
        }
    }
}
