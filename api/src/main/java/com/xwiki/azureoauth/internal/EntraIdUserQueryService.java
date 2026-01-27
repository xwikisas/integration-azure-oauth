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
package com.xwiki.azureoauth.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;

/**
 * Handles query calls needed by the Entra ID application.
 *
 * @version $Id$
 * @since 2.1
 */
@Component(roles = EntraIdUserQueryService.class)
@Singleton
public class EntraIdUserQueryService
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("document")
    private QueryFilter documentReferenceFilter;

    /**
     * Get the users that have the "XWiki.OIDC.UserClass" object from the current wiki.
     *
     * @return a {@link List} of the user documents
     * @throws XWikiException if there is any error while retrieving the documents
     * @throws QueryException if the query execution fails
     */
    public List<XWikiDocument> getEntraIdUsers() throws XWikiException, QueryException
    {
        List<XWikiDocument> azureUsers = new ArrayList<>();
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();

        List<DocumentReference> results = this.queryManager.createQuery(
                ", BaseObject as obj where doc.fullName = obj.name and obj.className = :className", Query.HQL)
            .setWiki(this.wikiManager.getCurrentWikiId()).bindValue("className", OIDC_USER_CLASS)
            .addFilter(documentReferenceFilter).execute();

        for (DocumentReference userRef : results) {
            XWikiDocument userDoc = wiki.getDocument(userRef, wikiContext);
            azureUsers.add(userDoc);
        }
        return azureUsers;
    }
}
