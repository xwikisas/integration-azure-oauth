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
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.internal.EntraIDNetworkManager;
import com.xwiki.azureoauth.internal.EntraIdUserQueryService;
import com.xwiki.azureoauth.user.EntraIdUserService;
import com.xwiki.azureoauth.user.ExternalUser;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;

/**
 * Default implementation of {@link EntraIdUserService}.
 *
 * @version $Id$
 * @since 2.1
 */
@Component
@Singleton
public class DefaultEntraIdUserService implements EntraIdUserService
{
    private static final String ENTRA_ISSUER = "login.microsoftonline.com";

    @Inject
    private EntraIDNetworkManager networkManager;

    @Inject
    private EntraIdUserQueryService userQueryService;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public Map<String, XWikiDocument> getInternalUsers() throws QueryException, XWikiException
    {
        List<XWikiDocument> users = userQueryService.getEntraIdUsers();
        Map<String, XWikiDocument> userMap = new HashMap<>();

        for (XWikiDocument userDoc : users) {
            BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS));
            String issuer = oidcObj.getField("issuer").toFormString();
            if (issuer.contains(ENTRA_ISSUER)) {
                String subject = oidcObj.getField("subject").toFormString();
                userMap.put(subject, userDoc);
            }
        }
        return userMap;
    }

    @Override
    public List<ExternalUser> getExternalUsers() throws Exception
    {
        JSONArray users = networkManager.getEntraUsersJson();
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
