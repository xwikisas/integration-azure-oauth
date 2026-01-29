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
package com.xwiki.azureoauth.internal.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.internal.EntraIDApiClient;
import com.xwiki.azureoauth.user.EntraIDUsersManager;
import com.xwiki.azureoauth.user.ExternalUser;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;

/**
 * Default implementation of {@link EntraIDUsersManager}.
 *
 * @version $Id$
 * @since 2.1
 */
@Component
@Singleton
public class DefaultEntraIDUsersManager implements EntraIDUsersManager
{
    private static final String ENTRA_ISSUER = "login.microsoftonline.com";

    @Inject
    private EntraIDApiClient entraIDApiClient;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("document")
    private QueryFilter documentReferenceFilter;

    @Override
    public Map<String, XWikiDocument> getXWikiUsersMap() throws QueryException, XWikiException
    {
        List<XWikiDocument> users = getXWikiUsers();
        Map<String, XWikiDocument> userMap = new HashMap<>();

        for (XWikiDocument userDoc : users) {
            BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS));
            String subject = oidcObj.getField("subject").toFormString();
            userMap.put(subject, userDoc);
        }
        return userMap;
    }

    @Override
    public List<XWikiDocument> getXWikiUsers() throws XWikiException, QueryException
    {
        List<XWikiDocument> xwikiEntraUsers = new ArrayList<>();
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();

        List<DocumentReference> results = this.queryManager.createQuery(
                ", BaseObject as obj where doc.fullName = obj.name and obj.className = :className", Query.HQL)
            .setWiki(this.wikiManager.getCurrentWikiId()).bindValue("className", OIDC_USER_CLASS)
            .addFilter(documentReferenceFilter).execute();

        for (DocumentReference userRef : results) {
            XWikiDocument userDoc = wiki.getDocument(userRef, wikiContext);
            BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS));
            String issuer = oidcObj.getField("issuer").toFormString();
            if (issuer.contains(ENTRA_ISSUER)) {
                xwikiEntraUsers.add(userDoc);
            }
        }
        return xwikiEntraUsers;
    }

    @Override
    public List<ExternalUser> getEntraServerUsers() throws Exception
    {
        JSONArray users = entraIDApiClient.getUsers();
        List<ExternalUser> externalUsers = new ArrayList<>();
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            String id = user.optString("id");
            Boolean enabled = user.optBoolean("accountEnabled");
            externalUsers.add(new ExternalUser(id, enabled));
        }
        return externalUsers;
    }
}
