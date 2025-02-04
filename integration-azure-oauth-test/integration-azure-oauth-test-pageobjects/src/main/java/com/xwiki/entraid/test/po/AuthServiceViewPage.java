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
package com.xwiki.entraid.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the view page for Authentication Backport contrib application which doesn't have a PO created so far. The
 * application might suffer changes that could break the tests.
 *
 * @version $Id$
 * @since 2.0
 */
public class AuthServiceViewPage extends ViewPage
{
    public void switchToOIDCAuthenticationService()
    {
        navigateToAuthenticationAdmin();

        WebElement authServiceSelectElement = getDriver().findElement(By.id("authServiceId"));
        WebElement saveButton = getDriver().findElement(By.className("btn-danger"));

        Select authServiceSelect = new Select(authServiceSelectElement);
        authServiceSelect.selectByValue("oidc");
        saveButton.click();
    }

    public boolean isOIDCSelected()
    {
        WebElement authService = getDriver().findElement(By.className("codeToExecute"));
        return authService.getText().contains("OpenID Connect Authenticator");
    }

    private void navigateToAuthenticationAdmin()
    {
        toggleDrawer();
        WebElement adminHyperlink = getDriver().findElement(By.id("tmAdminWiki"));
        adminHyperlink.click();
        WebElement panelHeadingOther = getDriver().findElement(By.id("panel-heading-other"));
        panelHeadingOther.click();
        getDriver().findElement(By.cssSelector("a[data-id='Authentication']")).click();
    }
}
