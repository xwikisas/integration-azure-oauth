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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.entraid.test.po.EntraIDViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Entra ID client configurations created outside the application, i.e. any document holding both an OIDC
 * client configuration object and an Entra ID configuration object.
 *
 * @version $Id$
 */
@UITest
class EntraIDClientConfigurationsIT
{
    private static final String CONFIGURATION_NAME = "second";

    private static final String TENANT_ID = "second_tenant";

    private static final String ISSUER = String.format("https://login.microsoftonline.com/%s/v2.0", TENANT_ID);

    private static final DocumentReference CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", "Sandbox", "SecondEntraConfiguration");

    private static final DocumentReference DEFAULT_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("EntraID", "Code"), "EntraOIDCClientConfiguration");

    private static final String ENTRAID_CONFIGURATION_CLASSNAME = "EntraID.Code.EntraIDConfigurationClass";

    private static final String OIDC_CONFIGURATION_CLASSNAME = "XWiki.OIDC.ClientConfigurationClass";

    private static final String TENANT_USER_NAME = "TenantUser";

    private static final String TENANT_GROUP_NAME = "SecondTenantGroup";

    private static final String PASSWORD = "pass";

    @BeforeAll
    static void setup(TestUtils setup) throws Exception
    {
        setup.createUser(TENANT_USER_NAME, PASSWORD, setup.getURLToNonExistentPage());

        setup.loginAsSuperAdmin();
        // A user created from the tenant of the second configuration, member of a group allowed to switch to the XWiki
        // login by the second configuration only.
        Object userObject =
            setup.rest().object(new LocalDocumentReference("XWiki", TENANT_USER_NAME), "XWiki.OIDC.UserClass");
        userObject.withProperties(TestUtils.RestTestUtils.property("issuer", ISSUER));
        setup.rest().add(userObject);
        setup.addObject(new LocalDocumentReference("XWiki", TENANT_GROUP_NAME), "XWiki.XWikiGroups", "member",
            "XWiki." + TENANT_USER_NAME);

        // Only the second configuration may enable the XWiki login.
        setup.updateObject(DEFAULT_CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0,
            "enableXWikiLoginGlobal", 0);
    }

    @Order(1)
    @Test
    void endpointsDerivedFromTenant(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.rest().savePage(CONFIGURATION_REFERENCE);
        Object oidcObject = testUtils.rest().object(CONFIGURATION_REFERENCE, OIDC_CONFIGURATION_CLASSNAME);
        oidcObject.withProperties(TestUtils.RestTestUtils.property("configurationName", CONFIGURATION_NAME),
            TestUtils.RestTestUtils.property("clientId", "second_clientId"));
        testUtils.rest().add(oidcObject);
        // The endpoints are derived when the Entra ID configuration object is added.
        Object entraIDObject = testUtils.rest().object(CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME);
        entraIDObject.withProperties(TestUtils.RestTestUtils.property("tenantId", "first_tenant"),
            TestUtils.RestTestUtils.property("enableXWikiLoginGlobal", 0));
        testUtils.rest().add(entraIDObject);
        assertEquals("https://login.microsoftonline.com/first_tenant/v2.0", getProvider(testUtils));

        // And again when the tenant is updated.
        testUtils.updateObject(CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0, "tenantId", TENANT_ID);
        assertEquals(ISSUER, getProvider(testUtils));
    }

    @Order(2)
    @Test
    void syncUsersOfConfiguration(TestUtils testUtils) throws Exception
    {
        assertEquals(404, postSync(testUtils, "missing"));
        // The job is started, even if it fails afterwards because the tenant doesn't exist.
        assertEquals(201, postSync(testUtils, CONFIGURATION_NAME));
    }

    @Order(3)
    @Test
    void xwikiLoginEnabledByConfiguration(TestUtils testUtils)
    {
        EntraIDViewPage entraIDViewPage = new EntraIDViewPage();
        testUtils.forceGuestUser();
        entraIDViewPage.goToHomePage();
        assertFalse(entraIDViewPage.isBypassLoginButtonDisplayed());

        testUtils.loginAsSuperAdmin();
        testUtils.updateObject(CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0, "enableXWikiLoginGlobal",
            1);
        testUtils.forceGuestUser();
        entraIDViewPage.goToHomePage();
        assertTrue(entraIDViewPage.isBypassLoginButtonDisplayed());

        // A disabled configuration doesn't enable the XWiki login.
        testUtils.loginAsSuperAdmin();
        testUtils.updateObject(CONFIGURATION_REFERENCE, OIDC_CONFIGURATION_CLASSNAME, 0, "skipped", 1);
        testUtils.forceGuestUser();
        entraIDViewPage.goToHomePage();
        assertFalse(entraIDViewPage.isBypassLoginButtonDisplayed());

        testUtils.loginAsSuperAdmin();
        testUtils.updateObject(CONFIGURATION_REFERENCE, OIDC_CONFIGURATION_CLASSNAME, 0, "skipped", 0);
    }

    @Order(4)
    @Test
    void switchUserToXWikiLoginByConfiguration(TestUtils testUtils)
    {
        EntraIDViewPage entraIDViewPage = new EntraIDViewPage();
        testUtils.login(TENANT_USER_NAME, PASSWORD);
        entraIDViewPage.goToHomePage();
        assertFalse(entraIDViewPage.isSwitchUserDisplayed());

        // The groups allowed to switch are taken from the configuration of the user's tenant.
        testUtils.loginAsSuperAdmin();
        testUtils.updateObject(CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0, "xwikiLoginGroups",
            "XWiki." + TENANT_GROUP_NAME);
        testUtils.login(TENANT_USER_NAME, PASSWORD);
        entraIDViewPage.goToHomePage();
        assertTrue(entraIDViewPage.isSwitchUserDisplayed());
    }

    private String getProvider(TestUtils testUtils) throws Exception
    {
        Property provider = testUtils.rest().get(new ObjectPropertyReference("provider",
            new ObjectReference(OIDC_CONFIGURATION_CLASSNAME + "[0]", CONFIGURATION_REFERENCE)));
        return provider.getValue();
    }

    private int postSync(TestUtils testUtils, String configuration) throws Exception
    {
        String credentials = Base64.getEncoder().encodeToString((TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName() + ':'
            + TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword()).getBytes(StandardCharsets.UTF_8));
        String baseURL = testUtils.rest().getBaseURL().replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format("%s/entraid/user/sync?disable=true&remove=true&configuration=%s", baseURL,
                configuration)))
            .header("Authorization", "Basic " + credentials)
            .POST(HttpRequest.BodyPublishers.noBody()).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
