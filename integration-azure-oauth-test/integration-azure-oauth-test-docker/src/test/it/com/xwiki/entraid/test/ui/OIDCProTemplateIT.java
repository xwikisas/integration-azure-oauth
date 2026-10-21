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
package com.xwiki.entraid.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.entraid.test.po.EntraIDConfigurationViewPage;
import com.xwiki.entraid.test.po.OIDCProEntraIDConfigurationEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the Entra ID template provided for OIDC Pro.
 *
 * @version $Id$
 */
@UITest
class OIDCProTemplateIT
{
    private static final String OIDC_CONFIGURATION_CLASSNAME = "XWiki.OIDC.ClientConfigurationClass";

    private static final String TENANT_ID = "oidcpro_tenant";

    private static DocumentReference configurationReference;

    @Order(1)
    @Test
    void createConfigurationFromTemplate(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        ViewPage viewPage = OIDCProEntraIDConfigurationEditPage.createConfiguration()
            .setTenantId(TENANT_ID)
            .addUserMapping("first_name", "given_name")
            .setConfigurationName("oidcpro")
            .setClientId("oidcpro_clientId")
            .clickSaveAndView();
        configurationReference =
            new DocumentReference(testUtils.resolveDocumentReference(viewPage.getMetaDataValue("reference")));

        // The Entra ID configuration object is created along with the OIDC client configuration, so the endpoints
        // are derived from its tenant.
        assertEquals(String.format("https://login.microsoftonline.com/%s/v2.0", TENANT_ID),
            getOIDCConfigurationProperty(testUtils, "provider"));
        assertEquals("first_name=given_name", getOIDCConfigurationProperty(testUtils, "userMapping"));
    }

    @Order(2)
    @Test
    void syncUsersOfConfiguration(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        testUtils.gotoPage(configurationReference);
        EntraIDConfigurationViewPage confViewPage = new EntraIDConfigurationViewPage();
        confViewPage.getSyncButton().click();
        assertEquals("Sync in progress...", confViewPage.getNotification().getText());
        // The job of the configuration is started, but it fails since the tenant doesn't exist.
        assertEquals("There was an error during the sync.", confViewPage.getErrorNotification().getText());
    }

    private String getOIDCConfigurationProperty(TestUtils testUtils, String propertyName) throws Exception
    {
        Property property = testUtils.rest().get(new ObjectPropertyReference(propertyName,
            new ObjectReference(OIDC_CONFIGURATION_CLASSNAME + "[0]", configurationReference)));
        return property.getValue();
    }
}
