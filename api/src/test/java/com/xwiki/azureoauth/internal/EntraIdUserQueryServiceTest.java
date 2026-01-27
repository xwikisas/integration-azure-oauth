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

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraIdUserQueryServiceTest}
 *
 * @version $Id$
 */
@ComponentTest
class EntraIdUserQueryServiceTest
{
    @InjectMockComponents
    private EntraIdUserQueryService entraIdUserQueryService;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

    @MockComponent
    @Named("document")
    private QueryFilter documentReferenceFilter;

    @Mock
    private DocumentReference documentReference1;

    @Mock
    private DocumentReference documentReference2;

    @Mock
    private XWikiDocument wikiDocument1;

    @Mock
    private XWikiDocument wikiDocument2;

    @Mock
    private Query query;

    @Mock
    private XWiki wiki;

    @Test
    void getAzureUsers() throws QueryException, XWikiException
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(
            queryManager.createQuery(", BaseObject as obj where doc.fullName = obj.name and obj.className = :className",
                Query.HQL)).thenReturn(query);
        when(wikiManager.getCurrentWikiId()).thenReturn("wiki_id");
        when(query.setWiki("wiki_id")).thenReturn(query);
        when(query.bindValue("className", "XWiki.OIDC.UserClass")).thenReturn(query);
        when(query.addFilter(documentReferenceFilter)).thenReturn(query);
        when(query.execute()).thenReturn(List.of(documentReference1, documentReference2));
        when(wikiContext.getWiki()).thenReturn(wiki);

        when(wiki.getDocument(documentReference1, wikiContext)).thenReturn(wikiDocument1);
        when(wiki.getDocument(documentReference2, wikiContext)).thenReturn(wikiDocument2);

        assertEquals(List.of(wikiDocument1, wikiDocument2), entraIdUserQueryService.getEntraIdUsers());
    }
}
