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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xwiki.azureoauth.configuration.AzureOldConfiguration;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;
import com.xwiki.azureoauth.internal.oldConfiguration.OldAzureOAuthConfiguration;
import com.xwiki.azureoauth.user.EntraIDUsersManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AzureADOIDCMigrator}
 *
 * @version $Id$
 */
@ComponentTest
class AzureADOIDCMigratorTest
{
    private static final String BASE_ENDPOINT = "https://login.microsoftonline.com/%s/oauth2/v2.0/%s";

    @InjectMockComponents
    private AzureADOIDCMigrator azureADOIDCMigrator;

    @MockComponent
    @Named(OldAzureOAuthConfiguration.HINT)
    private Provider<AzureOldConfiguration> oauthConfigurationProvider;

    @MockComponent
    @Named(OldAzureOAuthConfiguration.HINT)
    private AzureOldConfiguration oauthConfiguration;

    @MockComponent
    private Provider<EntraIDConfiguration> entraIDConfigurationProvider;

    @MockComponent
    private EntraIDConfiguration entraIDConfiguration;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

    @MockComponent
    private EntraIDUsersManager usersManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument wikiDocument1;

    @Mock
    private XWikiDocument wikiDocument2;

    @Mock
    private DocumentReference classReference;

    @Mock
    private BaseObject baseObject1;

    @Mock
    private BaseObject baseObject2;

    @Mock
    private PropertyInterface propertyInterface1;

    @Mock
    private PropertyInterface propertyInterface2;

    private Map<String, Object> endpoints =
        Map.of("authorizationEndpoint", String.format(BASE_ENDPOINT, "tenant_id", "authorize"), "tokenEndpoint",
            String.format(BASE_ENDPOINT, "tenant_id", "token"), "logoutEndpoint",
            String.format(BASE_ENDPOINT, "tenant_id", "logout"));

    @BeforeEach
    void setUp()
    {
        when(oauthConfigurationProvider.get()).thenReturn(oauthConfiguration);
        when(entraIDConfigurationProvider.get()).thenReturn(entraIDConfiguration);
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);

        when(oauthConfiguration.getClientID()).thenReturn("client_id");
        when(oauthConfiguration.getScope()).thenReturn("scope1,scope2");
        when(oauthConfiguration.getSecret()).thenReturn("secret");
        when(oauthConfiguration.getTenantID()).thenReturn("tenant_id");
        when(oauthConfiguration.isActive()).thenReturn(true);
    }

    @Test
    void initializeConfigurationWrongVersionTest() throws ConfigurationSaveException
    {
        when(entraIDConfiguration.getClientID()).thenReturn("client_id");
        when(entraIDConfiguration.getScope()).thenReturn("scope1,scope2");
        when(entraIDConfiguration.getSecret()).thenReturn("secret");
        when(entraIDConfiguration.getTenantID()).thenReturn("tenant_id");

        azureADOIDCMigrator.initializeOIDCConfiguration();
        verify(entraIDConfiguration, Mockito.times(0)).setOIDCConfiguration(anyMap());
    }

    @Test
    void initializeConfigurationTest() throws ConfigurationSaveException
    {
        when(entraIDConfiguration.getClientID()).thenReturn("");
        when(entraIDConfiguration.getScope()).thenReturn("");
        when(entraIDConfiguration.getSecret()).thenReturn("");
        when(entraIDConfiguration.getTenantID()).thenReturn("");

        azureADOIDCMigrator.initializeOIDCConfiguration();
        Map<String, Object> configMap = new HashMap<>(endpoints);
        configMap.put("clientId", "client_id");
        configMap.put("clientSecret", "secret");
        verify(entraIDConfiguration, Mockito.times(1)).setOIDCConfiguration(configMap);
    }

    @Test
    void refactorOIDCIssuerTest() throws QueryException, XWikiException
    {
        when(documentReferenceResolver.resolve("XWiki.OIDC.UserClass")).thenReturn(classReference);
        when(usersManager.getXWikiUsers()).thenReturn(List.of(wikiDocument1, wikiDocument2));
        when(wikiDocument1.getXObject(classReference)).thenReturn(baseObject1);
        when(wikiDocument2.getXObject(classReference)).thenReturn(baseObject2);
        when(baseObject1.getField("issuer")).thenReturn(propertyInterface1);
        when(baseObject2.getField("issuer")).thenReturn(propertyInterface2);
        when(propertyInterface1.toFormString()).thenReturn("http.something.com/tenantId/2.0");
        when(propertyInterface2.toFormString()).thenReturn("http.something.com/tenantId/v2.0");

        azureADOIDCMigrator.refactorOIDCIssuer();
        verify(wiki, Mockito.times(1)).saveDocument(wikiDocument1,
            "Refactored OIDC issuer to the right format used by Entra ID.", wikiContext);
        verify(wiki, Mockito.times(0)).saveDocument(wikiDocument2,
            "Refactored OIDC issuer to the right format used by Entra ID.", wikiContext);
    }

    @Test
    void getEndpointsTest()
    {
        assertEquals(endpoints, azureADOIDCMigrator.getEndpoints("tenant_id"));
    }
}
