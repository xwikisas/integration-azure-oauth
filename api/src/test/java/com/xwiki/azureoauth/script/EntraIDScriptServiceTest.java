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
package com.xwiki.azureoauth.script;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;
import com.xwiki.azureoauth.configuration.EntraIDClientConfiguration;
import com.xwiki.azureoauth.configuration.EntraIDClientConfigurationStore;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraIDScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class EntraIDScriptServiceTest
{
    private static final String ISSUER = "https://login.microsoftonline.com/tenant/v2.0";

    @InjectMockComponents
    private EntraIDScriptService scriptService;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    @Named("default")
    private EntraIDConfiguration entraIDConfiguration;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntraIDClientConfigurationStore clientConfigurationStore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument userDoc;

    @Mock
    private User user;

    private final DocumentReference userReference = new DocumentReference("wiki", "XWiki", "User");

    private final DocumentReference oidcUserClassReference = new DocumentReference("wiki", "XWiki", "OIDC");

    @BeforeComponent
    void registerComponents(MockitoComponentManager componentManager) throws Exception
    {
        // The User API resolves a component when its class is initialized.
        componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        Utils.setComponentManager(componentManager);
    }

    @BeforeEach
    void setUp() throws XWikiException
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(wikiContext.getUserReference()).thenReturn(userReference);
        when(wiki.getDocument(userReference, wikiContext)).thenReturn(userDoc);
        when(wiki.getUser(userReference, wikiContext)).thenReturn(user);
        when(documentReferenceResolver.resolve("XWiki.OIDC.UserClass")).thenReturn(oidcUserClassReference);
    }

    @Test
    void isXWikiLoginEnabledByDefaultConfiguration()
    {
        when(entraIDConfiguration.isXWikiLoginGlobalEnabled()).thenReturn(true);

        assertTrue(scriptService.isXWikiLoginEnabled());
    }

    @Test
    void isXWikiLoginEnabledByClientConfiguration() throws Exception
    {
        EntraIDClientConfiguration inactive = mockConfiguration(false, true);
        EntraIDClientConfiguration active = mockConfiguration(true, true);
        when(clientConfigurationStore.getConfigurations()).thenReturn(List.of(inactive));
        assertFalse(scriptService.isXWikiLoginEnabled());

        when(clientConfigurationStore.getConfigurations()).thenReturn(List.of(inactive, active));
        assertTrue(scriptService.isXWikiLoginEnabled());
    }

    @Test
    void isXWikiLoginEnabledWhenConfigurationsFail() throws Exception
    {
        when(clientConfigurationStore.getConfigurations()).thenThrow(new RuntimeException("query error"));

        assertFalse(scriptService.isXWikiLoginEnabled());
        assertEquals("Failed to retrieve the Entra ID client configurations. Root cause is: "
            + "[RuntimeException: query error]", logCapture.getMessage(0));
    }

    @Test
    void shouldDisplayXWikiLoginForGuest() throws XWikiException
    {
        when(wikiContext.getUserReference()).thenReturn(null);

        assertFalse(scriptService.shouldDisplayXWikiLogin());
    }

    @Test
    void shouldDisplayXWikiLoginForNonEntraIDUser() throws XWikiException
    {
        assertFalse(scriptService.shouldDisplayXWikiLogin());
    }

    @Test
    void shouldDisplayXWikiLoginWithUserConfiguration() throws Exception
    {
        mockOIDCUser();
        EntraIDClientConfiguration otherTenant = mock(EntraIDClientConfiguration.class);
        when(otherTenant.getXWikiLoginGroups()).thenReturn(List.of("XWiki.OtherGroup"));
        EntraIDClientConfiguration userTenant = mock(EntraIDClientConfiguration.class);
        when(userTenant.matchesIssuer(ISSUER)).thenReturn(true);
        when(userTenant.getXWikiLoginGroups()).thenReturn(List.of("XWiki.TenantGroup"));
        when(clientConfigurationStore.getConfigurations()).thenReturn(List.of(otherTenant, userTenant));
        when(entraIDConfiguration.getXWikiLoginGroups()).thenReturn("XWiki.DefaultGroup");
        when(user.isUserInGroup("XWiki.OtherGroup")).thenReturn(true);
        when(user.isUserInGroup("XWiki.DefaultGroup")).thenReturn(true);

        assertFalse(scriptService.shouldDisplayXWikiLogin());

        when(user.isUserInGroup("XWiki.TenantGroup")).thenReturn(true);
        assertTrue(scriptService.shouldDisplayXWikiLogin());
    }

    @Test
    void shouldDisplayXWikiLoginWithDefaultConfiguration() throws Exception
    {
        mockOIDCUser();
        when(entraIDConfiguration.getXWikiLoginGroups()).thenReturn("XWiki.GroupA,XWiki.GroupB");
        when(user.isUserInGroup("XWiki.GroupB")).thenReturn(true);

        assertTrue(scriptService.shouldDisplayXWikiLogin());
    }

    private void mockOIDCUser()
    {
        BaseObject oidcUserObject = new BaseObject();
        oidcUserObject.setStringValue("issuer", ISSUER);
        when(userDoc.getXObject(oidcUserClassReference)).thenReturn(oidcUserObject);
    }

    private EntraIDClientConfiguration mockConfiguration(boolean active, boolean xwikiLoginEnabled)
    {
        EntraIDClientConfiguration configuration = mock(EntraIDClientConfiguration.class);
        when(configuration.isActive()).thenReturn(active);
        when(configuration.isXWikiLoginGlobalEnabled()).thenReturn(xwikiLoginEnabled);
        return configuration;
    }
}
