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
package com.xwiki.azureoauth.internal.syncJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.internal.EntraQueryManager;
import com.xwiki.azureoauth.internal.network.EntraIDNetworkManager;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;

/**
 * Manager class handling the sync between Entra ID users and XWiki users.
 *
 * @version $Id$
 * @since 2.1
 */
@Component(roles = EntraSyncManager.class)
@Singleton
public class EntraSyncManager
{
    private static final String USER_CLASS = "XWiki.XWikiUsers";

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private EntraQueryManager entraQueryManager;

    @Inject
    private EntraIDNetworkManager entraIDNetworkManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Sync XWiki users that have the OIDC user class with the ones from Entra ID.
     *
     * @param disable {@code true} if the sync should also sync disabled users, or {@code false} otherwise
     * @param remove {@code true} if the sync should also sync removed users, or {@code false} otherwise
     * @throws Exception if any error occurs during the sync
     */
    public void syncUsers(boolean disable, boolean remove) throws Exception
    {
        Map<String, XWikiDocument> usersMap = getAzureUsersMap();
        Map<String, Boolean> jsonMap = entraIDNetworkManager.getEntraUsersJsonMap();
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        for (Map.Entry<String, XWikiDocument> entry : usersMap.entrySet()) {
            XWikiDocument userDoc = entry.getValue();
            if (!jsonMap.containsKey(entry.getKey())) {
                if (remove) {
                    wiki.deleteDocument(userDoc, wikiContext);
                }
                continue;
            }
            boolean jsonActive = jsonMap.get(entry.getKey());
            if (disable && !jsonActive) {
                BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(USER_CLASS));
                oidcObj.set("active", 0, wikiContext);
                wiki.saveDocument(userDoc, wikiContext);
            }
        }
    }

    private Map<String, XWikiDocument> getAzureUsersMap() throws XWikiException, QueryException
    {
        List<XWikiDocument> users = entraQueryManager.getAzureUsers();
        Map<String, XWikiDocument> userMap = new HashMap<>();

        for (XWikiDocument userDoc : users) {
            BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS));
            String subject = oidcObj.getField("subject").toFormString();
            userMap.put(subject, userDoc);
        }
        return userMap;
    }
}
