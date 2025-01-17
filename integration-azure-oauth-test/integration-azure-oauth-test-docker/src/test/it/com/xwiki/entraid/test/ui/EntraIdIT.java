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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.EditGroupModal;
import org.xwiki.administration.test.po.GroupsPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.entraid.test.po.AuthServiceViewPage;
import com.xwiki.entraid.test.po.EntraIDViewPage;
import com.xwiki.entraid.test.po.XWikiLoginViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// add dependencies overwritten by backport
@UITest(properties = {
    // Add the RightsManagerPlugin needed by the test
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin",
    // Programming rights are required to disable/enable user profiles (cf. XWIKI-21238)
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.XWikiUserProfileSheet" })
class EntraIdIT
{
    private static final String FIRST_USER_NAME = "JonSnow";

    private static final String SECOND_USER_NAME = "ElsaIce";

    private static final String GROUP_NAME = "XWiki_devs";

    private static final DocumentReference ENTRAID_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("EntraID", "Code"), "EntraOIDCClientConfiguration");

    private static final DocumentReference ENTRAID_WEBHOME_REFERENCE =
        new DocumentReference("xwiki", "AzureAD", "WebHome");

    private static final String ENTRAID_CONFIGURATION_CLASSNAME = "EntraID.Code.EntraIDConfigurationClass";

    private static final String PASSWORD = "pass";

    @BeforeAll
    static void setUp(TestUtils setup) throws Exception
    {
        setup.createUser(FIRST_USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Jon", "last_name",
            "Snow");

        setup.createUser(SECOND_USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Elsa", "last_name",
            "Ice");

        // Create a group and add both users.
        setup.loginAsSuperAdmin();
        GroupsPage groupsPage = GroupsPage.gotoPage();
        groupsPage.addNewGroup(GROUP_NAME);
        EditGroupModal devsGroupModal = groupsPage.clickEditGroup(GROUP_NAME);
        devsGroupModal.addUsers(FIRST_USER_NAME, SECOND_USER_NAME);
        groupsPage = GroupsPage.gotoPage();
        assertEquals("2", groupsPage.getMemberCount(GROUP_NAME));

        // By default the minimal distribution used for the tests doesn't have any rights setup. Let's create an Admin
        // user part of the Admin Group and make sure that this Admin Group has admin rights in the wiki. We could also
        // have given that Admin user the admin right directly but the solution we chose is closer to the XS
        // distribution.
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createAdminUser();
        setup.loginAsAdmin();

        // Modify the tenant ID to update the OIDC configuration so that the Microsoft login page is displayed.
        setup.updateObject(ENTRAID_CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0, "tenantId",
            "test_tenantId");

        // Configure the OIDC user class for the second user tobe able to test the switch to XWiki user functionality.
        Object userObject =
            setup.rest().object(new LocalDocumentReference("XWiki", SECOND_USER_NAME), "XWiki.OIDC.UserClass");
        userObject.withProperties(TestUtils.RestTestUtils.property("issuer", "some_issuer"));
        setup.rest().add(userObject);
        setup.forceGuestUser();
    }

    @Order(1)
    @Test
    void authenticate(TestUtils testUtils)
    {
        // Switch the authentication service to OIDC.
        EntraIDViewPage entraIDViewPage = new EntraIDViewPage();
        testUtils.loginAsAdmin();
        entraIDViewPage.goToHomePage();
        AuthServiceViewPage authServiceViewPage = new AuthServiceViewPage();
        authServiceViewPage.navigateToAuthenticationAdmin();
        authServiceViewPage.switchToOIDCAuthenticationService();
        assertTrue(authServiceViewPage.isOIDCSelected());
        testUtils.forceGuestUser();

        // Check that the login functionality correctly redirect the guest to Microsoft login page, respectively that
        // the OIDC bypass login is working.
        entraIDViewPage.goToHomePage();
        entraIDViewPage.clickLogin();
        testUtils.getDriver().waitUntilElementIsVisible(By.id("exceptionMessageContainer"));
        WebElement loginPageMessage = entraIDViewPage.getMicrosoftContainer();
        assertTrue(loginPageMessage.getText().contains(
            "Specified tenant identifier 'test_tenantid' is neither a valid DNS name, nor a valid external domain."));
        entraIDViewPage.goToHomePage();
        entraIDViewPage.clickLoginBypass();
        XWikiLoginViewPage xwikiLoginViewPage = new XWikiLoginViewPage();
        assertTrue(xwikiLoginViewPage.getXwikiLoginContainer().isDisplayed());

        // Disable the OIDC login bypass and check that is no longer displayed.
        entraIDViewPage.goToHomePage();
        testUtils.loginAsAdmin();
        testUtils.updateObject(ENTRAID_CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0,
            "enableXWikiLoginGlobal", 0);
        testUtils.forceGuestUser();
        entraIDViewPage.goToHomePage();
        assertFalse(entraIDViewPage.canBypassLogin());
    }

    @Order(2)
    @Test
    void SwitchUserTest(TestUtils testUtils)
    {
        // Check that the Entra ID user (the user with the OIDC user class) does not have the possibility to switch
        // to a XWiki user before adding the group to configuration.
        EntraIDViewPage entraIDViewPage = new EntraIDViewPage();
        entraIDViewPage.goToHomePage();
        testUtils.login(SECOND_USER_NAME, PASSWORD);
        entraIDViewPage.goToHomePage();
        assertEquals(SECOND_USER_NAME, testUtils.getLoggedInUserName());
        assertFalse(entraIDViewPage.isSwitchUserDisplayed());

        // Check that the switch option is available to an EntraID user after adding the group to the configuration,
        // but not available to an XWiki user from the same group.
        testUtils.loginAsAdmin();
        testUtils.updateObject(ENTRAID_CONFIGURATION_REFERENCE, ENTRAID_CONFIGURATION_CLASSNAME, 0, "xwikiLoginGroups",
            String.format("XWiki.%s", GROUP_NAME));
        testUtils.login(SECOND_USER_NAME, PASSWORD);
        entraIDViewPage.goToHomePage();
        assertTrue(entraIDViewPage.isSwitchUserDisplayed());
        testUtils.login(FIRST_USER_NAME, PASSWORD);
        entraIDViewPage.goToHomePage();
        assertFalse(entraIDViewPage.isSwitchUserDisplayed());
    }
}
