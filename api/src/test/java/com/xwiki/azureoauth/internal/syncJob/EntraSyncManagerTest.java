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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xwiki.azureoauth.internal.EntraQueryManager;
import com.xwiki.azureoauth.internal.network.EntraIDNetworkManager;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraSyncManagerTest}
 *
 * @version $Id$
 */
@ComponentTest
class EntraSyncManagerTest
{
    @InjectMockComponents
    private EntraSyncManager syncManager;

    @MockComponent
    private EntraIDNetworkManager entraIDNetworkManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @MockComponent
    private EntraQueryManager entraQueryManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private DocumentReference oidcUserClassDocRef;

    @Mock
    private DocumentReference userClassDocRef;

    @Mock
    private XWikiDocument userDoc1;

    @Mock
    private XWikiDocument userDoc2;

    @Mock
    private XWikiDocument userDoc3;

    @Mock
    private BaseObject object1;

    @Mock
    private BaseObject object2;

    @Mock
    private BaseObject objectUserClass2;

    @Mock
    private BaseObject object3;

    @Mock
    private PropertyInterface property1;

    @Mock
    private PropertyInterface property2;

    @Mock
    private PropertyInterface property3;

    @BeforeEach
    void setUp() throws Exception
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(wikiManager.getCurrentWikiId()).thenReturn("testWiki");
        when(entraQueryManager.getAzureUsers()).thenReturn(List.of(userDoc1, userDoc2, userDoc3));
        when(documentReferenceResolver.resolve(OIDC_USER_CLASS)).thenReturn(oidcUserClassDocRef);
        when(userDoc1.getXObject(oidcUserClassDocRef)).thenReturn(object1);
        when(userDoc2.getXObject(oidcUserClassDocRef)).thenReturn(object2);
        when(userDoc3.getXObject(oidcUserClassDocRef)).thenReturn(object3);
        when(object1.getField("subject")).thenReturn(property1);
        when(object2.getField("subject")).thenReturn(property2);
        when(object3.getField("subject")).thenReturn(property3);
        when(property1.toFormString()).thenReturn("subject1");
        when(property2.toFormString()).thenReturn("subject2");
        when(property3.toFormString()).thenReturn("subject3");
        Map<String, Boolean> jsonMap = Map.of("subject1", true, "subject2", false);
        when(entraIDNetworkManager.getEntraUsersJsonMap()).thenReturn(jsonMap);

        when(documentReferenceResolver.resolve("XWiki.XWikiUsers")).thenReturn(userClassDocRef);
        when(userDoc2.getXObject(userClassDocRef)).thenReturn(objectUserClass2);
    }

    @Test
    void syncUsersTestDisableRemove() throws Exception
    {
        syncManager.syncUsers(true, true);
        verify(wiki, times(1)).deleteDocument(userDoc3, wikiContext);
        verify(objectUserClass2, times(1)).set("active", 0, wikiContext);
    }

    @Test
    void syncUsersTestDisable() throws Exception
    {
        syncManager.syncUsers(true, false);
        verify(wiki, times(0)).deleteDocument(userDoc3, wikiContext);
        verify(objectUserClass2, times(1)).set("active", 0, wikiContext);
    }

    @Test
    void syncUsersTestRemove() throws Exception
    {
        syncManager.syncUsers(false, true);
        verify(wiki, times(1)).deleteDocument(userDoc3, wikiContext);
        verify(objectUserClass2, times(0)).set("active", 0, wikiContext);
    }
}
