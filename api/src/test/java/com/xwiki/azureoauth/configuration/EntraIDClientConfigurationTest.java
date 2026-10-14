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
package com.xwiki.azureoauth.configuration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link EntraIDClientConfiguration}.
 *
 * @version $Id$
 */
class EntraIDClientConfigurationTest
{
    private static final DocumentReference DOCUMENT = new DocumentReference("wiki", "OIDCPro", "Tenant");

    private static final String ISSUER = "https://login.microsoftonline.com/%s/v2.0";

    @Test
    void readObjects()
    {
        BaseObject oidcObject = new BaseObject();
        oidcObject.setStringValue("configurationName", "tenant");
        oidcObject.setStringValue("clientId", "client");
        oidcObject.setStringValue("clientSecret", "secret");
        oidcObject.setStringValue("tokenEndpoint", "token");
        oidcObject.setIntValue("skipped", 1);
        BaseObject entraIDObject = new BaseObject();
        entraIDObject.setStringValue("tenantId", "tenantID");
        entraIDObject.setIntValue("enableXWikiLoginGlobal", 0);
        entraIDObject.setLargeStringValue("xwikiLoginGroups", "XWiki.GroupA,XWiki.GroupB");

        EntraIDClientConfiguration configuration =
            new EntraIDClientConfiguration(DOCUMENT, oidcObject, entraIDObject);

        assertEquals(DOCUMENT, configuration.getDocumentReference());
        assertEquals("tenant", configuration.getConfigurationName());
        assertEquals("client", configuration.getClientID());
        assertEquals("secret", configuration.getSecret());
        assertEquals("token", configuration.getTokenEndpoint());
        assertEquals("tenantID", configuration.getTenantID());
        assertFalse(configuration.isActive());
        assertFalse(configuration.isXWikiLoginGlobalEnabled());
        assertEquals(List.of("XWiki.GroupA", "XWiki.GroupB"), configuration.getXWikiLoginGroups());
    }

    @Test
    void missingObjects()
    {
        EntraIDClientConfiguration configuration = new EntraIDClientConfiguration(DOCUMENT, null, null);

        assertEquals("", configuration.getConfigurationName());
        assertEquals("", configuration.getTenantID());
        assertTrue(configuration.isActive());
        assertTrue(configuration.isXWikiLoginGlobalEnabled());
        assertEquals(List.of(), configuration.getXWikiLoginGroups());
    }

    @Test
    void matchesIssuer()
    {
        BaseObject entraIDObject = new BaseObject();
        entraIDObject.setStringValue("tenantId", "tenantA");
        EntraIDClientConfiguration configuration = new EntraIDClientConfiguration(DOCUMENT, null, entraIDObject);

        assertTrue(configuration.matchesIssuer(String.format(ISSUER, "tenantA")));
        assertFalse(configuration.matchesIssuer(String.format(ISSUER, "tenantAB")));
        assertFalse(configuration.matchesIssuer(String.format(ISSUER, "tenantB")));
        assertFalse(configuration.matchesIssuer("https://accounts.example.com/tenantA/"));
        assertFalse(configuration.matchesIssuer(null));
    }

    @Test
    void matchesIssuerWithMultiTenantAlias()
    {
        BaseObject entraIDObject = new BaseObject();
        entraIDObject.setStringValue("tenantId", "common");
        EntraIDClientConfiguration configuration = new EntraIDClientConfiguration(DOCUMENT, null, entraIDObject);

        assertTrue(configuration.matchesIssuer(String.format(ISSUER, "anyTenant")));
        assertFalse(configuration.matchesIssuer("https://accounts.example.com/"));
    }
}
