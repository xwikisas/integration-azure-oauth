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

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xwiki.azureoauth.internal.EntraIDNetworkManager;
import com.xwiki.azureoauth.internal.EntraIdUserQueryService;
import com.xwiki.azureoauth.user.ExternalUser;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultEntraIdUserService}
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultEntraIdUserServiceTest
{
    @InjectMockComponents
    private DefaultEntraIdUserService defaultEntraIdUserService;

    @MockComponent
    private EntraIDNetworkManager entraIDNetworkManager;

    @MockComponent
    private EntraIdUserQueryService userQueryService;

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

    @Test
    void getExternalUsersTest() throws Exception
    {
        String jsonBody =
            "[{ \"id\": \"user1\", \"accountEnabled\": true },{ \"id\": \"user2\", \"accountEnabled\": false },"
                + "{ \"id\": \"user3\", \"accountEnabled\": true }]";

        JSONArray jsonArray = new JSONArray(jsonBody);
        when(entraIDNetworkManager.getEntraUsersJson()).thenReturn(jsonArray);
        List<ExternalUser> externalUsers = defaultEntraIdUserService.getExternalUsers();
        assertEquals("user1", externalUsers.get(0).getId());
        assertFalse(externalUsers.get(1).isEnabled());
        assertEquals("user3", externalUsers.get(2).getId());
    }

    @Test
    void getInternalUsersTest() throws Exception
    {
        when(userQueryService.getEntraIdUsers()).thenReturn(List.of(userDoc1, userDoc2, userDoc3));
        when(documentReferenceResolver.resolve(OIDC_USER_CLASS)).thenReturn(oidcUserClassDocRef);
        when(userDoc1.getXObject(oidcUserClassDocRef)).thenReturn(object1);
        when(userDoc2.getXObject(oidcUserClassDocRef)).thenReturn(object2);
        when(userDoc3.getXObject(oidcUserClassDocRef)).thenReturn(object3);
        when(object1.getField("subject")).thenReturn(property1);
        when(object2.getField("subject")).thenReturn(property2);
        when(object3.getField("subject")).thenReturn(property3);
        when(object1.getField("issuer")).thenReturn(property4);
        when(object2.getField("issuer")).thenReturn(property5);
        when(object3.getField("issuer")).thenReturn(property6);
        when(property1.toFormString()).thenReturn("subject1");
        when(property2.toFormString()).thenReturn("subject2");
        when(property3.toFormString()).thenReturn("subject3");
        when(property4.toFormString()).thenReturn("https://login.microsoftonline.com/etc");
        when(property5.toFormString()).thenReturn("issuer2");
        when(property6.toFormString()).thenReturn("https://login.microsoftonline.com/etc");

        Map<String, XWikiDocument> map = Map.of("subject1", userDoc1, "subject3", userDoc3);
        Map<String, XWikiDocument> resultMap = defaultEntraIdUserService.getInternalUsers();
        assertEquals(map, resultMap);
    }
}
