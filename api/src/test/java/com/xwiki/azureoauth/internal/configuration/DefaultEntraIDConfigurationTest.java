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
package com.xwiki.azureoauth.internal.configuration;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultEntraIDConfiguration}
 *
 * @version $Id$
 */
@ComponentTest
class DefaultEntraIDConfigurationTest
{
    @InjectMockComponents
    private DefaultEntraIDConfiguration defaultEntraIDConfiguration;

    @MockComponent
    @Named(EntraIDConfigurationSource.HINT)
    private ConfigurationSource entraIDConfig;

    @MockComponent
    @Named(OIDCClientConfigurationSource.HINT)
    private ConfigurationSource oidcConfig;

    @Test
    void getOIDCTenantIDTest()
    {
        when(oidcConfig.getProperty("authorizationEndpoint", "")).thenReturn("http.test.com/test_value/oauth2/v2");
        assertEquals("test_value", defaultEntraIDConfiguration.getOIDCTenantID());
    }

    @Test
    void getClientIDTest()
    {
        when(oidcConfig.getProperty("clientId", "")).thenReturn("client_id");
        assertEquals("client_id", defaultEntraIDConfiguration.getClientID());
    }

    @Test
    void getSecretTest()
    {
        when(oidcConfig.getProperty("clientSecret", "")).thenReturn("client_secret");
        assertEquals("client_secret", defaultEntraIDConfiguration.getSecret());
    }

    @Test
    void getScopeTest()
    {
        when(oidcConfig.getProperty("scope", "")).thenReturn("app_scope");
        assertEquals("app_scope", defaultEntraIDConfiguration.getScope());
    }

    @Test
    void isActiveTest()
    {
        when(oidcConfig.getProperty("skipped", false)).thenReturn(true);
        assertFalse(defaultEntraIDConfiguration.isActive());
    }

    @Test
    void isXWikiLoginGlobalEnabledTest()
    {
        when(entraIDConfig.getProperty("enableXWikiLoginGlobal", true)).thenReturn(false);
        assertFalse(defaultEntraIDConfiguration.isXWikiLoginGlobalEnabled());
    }

    @Test
    void getTenantIDTest()
    {
        when(entraIDConfig.getProperty("tenantId", "")).thenReturn("tenant_id");
        assertEquals("tenant_id", defaultEntraIDConfiguration.getTenantID());
    }

    @Test
    void getXWikiLoginGroupsTest()
    {
        when(entraIDConfig.getProperty("xwikiLoginGroups", "")).thenReturn("wiki_group1,wiki_gr2");
        assertEquals("wiki_group1,wiki_gr2", defaultEntraIDConfiguration.getXWikiLoginGroups());
    }

    @Test
    void getTokenEndpointTest()
    {
        when(oidcConfig.getProperty("tokenEndpoint", "")).thenReturn("token endpoint");
        assertEquals("token endpoint", defaultEntraIDConfiguration.getTokenEndpoint());
    }
}
