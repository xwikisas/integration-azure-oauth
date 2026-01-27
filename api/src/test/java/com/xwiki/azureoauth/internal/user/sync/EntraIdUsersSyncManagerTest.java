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
import com.xwiki.azureoauth.user.EntraIdUserService;
import com.xwiki.azureoauth.user.ExternalUser;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraIdUsersSyncManagerTest}
 *
 * @version $Id$
 */
@ComponentTest
class EntraIdUsersSyncManagerTest
{
    @InjectMockComponents
    private EntraIdUsersSyncManager syncManager;

    @MockComponent
    private EntraIdUserService entraIdUserService;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private DocumentReference userClassDocRef;

    @Mock
    private XWikiDocument userDoc1;

    @Mock
    private XWikiDocument userDoc2;

    @Mock
    private XWikiDocument userDoc3;

    @Mock
    private BaseObject objectUserClass2;

    @BeforeEach
    void setUp() throws Exception
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(wikiManager.getCurrentWikiId()).thenReturn("testWiki");
        when(entraIdUserService.getInternalUsers()).thenReturn(
            Map.of("subject1", userDoc1, "subject2", userDoc2, "subject3", userDoc3));
        List<ExternalUser> externalUsers =
            List.of(new ExternalUser("subject1", true), new ExternalUser("subject2", false));
        when(entraIdUserService.getExternalUsers()).thenReturn(externalUsers);

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
