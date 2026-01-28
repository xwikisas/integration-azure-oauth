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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
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
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xwiki.azureoauth.internal.EntraIDApiClient;
import com.xwiki.azureoauth.user.ExternalUser;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultEntraIDUserService}
 *
 * @version $Id$
 */
@ComponentTest
class DefaultEntraIDUserServiceTest
{
    @InjectMockComponents
    private DefaultEntraIDUserService defaultEntraIdUserService;

    @MockComponent
    private EntraIDApiClient entraIDApiClient;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiDocument userDoc1;

    @Mock
    private XWikiDocument userDoc2;

    @Mock
    private XWikiDocument userDoc3;

    @Mock
    private PropertyInterface property1;

    @Mock
    private PropertyInterface property2;

    @Mock
    private PropertyInterface property3;

    @Mock
    private PropertyInterface property4;

    @Mock
    private PropertyInterface property5;

    @Mock
    private PropertyInterface property6;

    @Mock
    private BaseObject object1;

    @Mock
    private BaseObject object2;

    @Mock
    private BaseObject object3;

    @Mock
    private DocumentReference oidcUserClassDocRef;

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
    private DocumentReference documentReference3;

    @Mock
    private XWikiDocument wikiDocument1;

    @Mock
    private XWikiDocument wikiDocument2;

    @Mock
    private Query query;

    @Mock
    private XWiki wiki;

    @BeforeEach
    void setup() throws XWikiException, QueryException
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
        when(query.execute()).thenReturn(List.of(documentReference1, documentReference2, documentReference3));
        when(wikiContext.getWiki()).thenReturn(wiki);

        when(wiki.getDocument(documentReference1, wikiContext)).thenReturn(userDoc1);
        when(wiki.getDocument(documentReference2, wikiContext)).thenReturn(userDoc2);
        when(wiki.getDocument(documentReference3, wikiContext)).thenReturn(userDoc3);

        when(documentReferenceResolver.resolve(OIDC_USER_CLASS)).thenReturn(oidcUserClassDocRef);
        when(userDoc1.getXObject(oidcUserClassDocRef)).thenReturn(object1);
        when(userDoc2.getXObject(oidcUserClassDocRef)).thenReturn(object2);
        when(userDoc3.getXObject(oidcUserClassDocRef)).thenReturn(object3);
        when(object1.getField("issuer")).thenReturn(property4);
        when(object2.getField("issuer")).thenReturn(property5);
        when(object3.getField("issuer")).thenReturn(property6);
        when(property4.toFormString()).thenReturn("https://login.microsoftonline.com/etc");
        when(property5.toFormString()).thenReturn("issuer2");
        when(property6.toFormString()).thenReturn("https://login.microsoftonline.com/etc");
    }

    @Test
    void getEntraServerUsersTest() throws Exception
    {
        String jsonBody =
            "[{ \"id\": \"user1\", \"accountEnabled\": true },{ \"id\": \"user2\", \"accountEnabled\": false },"
                + "{ \"id\": \"user3\", \"accountEnabled\": true }]";

        JSONArray jsonArray = new JSONArray(jsonBody);
        when(entraIDApiClient.getUsers()).thenReturn(jsonArray);
        List<ExternalUser> externalUsers = defaultEntraIdUserService.getEntraServerUsers();
        assertEquals("user1", externalUsers.get(0).getId());
        assertFalse(externalUsers.get(1).isEnabled());
        assertEquals("user3", externalUsers.get(2).getId());
    }

    @Test
    void getEntraUsersMapTest() throws Exception
    {

        when(object1.getField("subject")).thenReturn(property1);
        when(object2.getField("subject")).thenReturn(property2);
        when(object3.getField("subject")).thenReturn(property3);
        when(property1.toFormString()).thenReturn("subject1");
        when(property2.toFormString()).thenReturn("subject2");
        when(property3.toFormString()).thenReturn("subject3");

        Map<String, XWikiDocument> map = Map.of("subject1", userDoc1, "subject3", userDoc3);
        Map<String, XWikiDocument> resultMap = defaultEntraIdUserService.getEntraUsersMap();
        assertEquals(map, resultMap);
    }

    @Test
    void getEntraUsersTest() throws QueryException, XWikiException
    {
        assertEquals(List.of(userDoc1, userDoc3), defaultEntraIdUserService.getEntraUsers());
    }
}
