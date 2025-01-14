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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.entraid.test.po.AuthServiceViewPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
class EntraIdIT
{
    private static final String FIRST_USER_NAME = "JonSnow";

    private static final String SECOND_USER_NAME = "ElsaIce";

    private static final DocumentReference ENTRAID_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("EntraID", "Code"), "EntraOIDCClientConfiguration");

    private static final DocumentReference ENTRAID_WEBHOME_REFERENCE =
        new DocumentReference("xwiki", "AzureAD", "WebHome");

    private static final String ENTRAID_CONFIGURATION_CLASSNAME = "EntraID.Code.EntraIDConfigurationClass";

    private static final String OIDC_CONFIGURATION_CLASSNAME = "XWiki.OIDC.ClientConfigurationClass";

    private static final String PASSWORD = "pass";

    @BeforeAll
    static void setUp(TestUtils setup)
    {
        setup.createUser(FIRST_USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Jon", "last_name",
            "Snow");

        setup.createUser(SECOND_USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Elsa", "last_name",
            "Ice");

        // By default the minimal distribution used for the tests doesn't have any rights setup. Let's create an Admin
        // user part of the Admin Group and make sure that this Admin Group has admin rights in the wiki. We could also
        // have given that Admin user the admin right directly but the solution we chose is closer to the XS
        // distribution.
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createAdminUser();
        setup.loginAsAdmin();
    }

    @Test
    void authenticate(TestUtils testUtils)
    {
        AuthServiceViewPage authServiceViewPage = new AuthServiceViewPage();
        authServiceViewPage.goToPage();
        authServiceViewPage.switchToOIDCAuthenticationService();
        assertTrue(authServiceViewPage.getAuthServiceUI().getText().contains(
            "OpenID Connect Authenticator (org.xwiki.contrib.oidc.auth.internal.OIDCAuthService) Allow authenticating"
                + " through an OpenID Connect provider"));
    }
}
