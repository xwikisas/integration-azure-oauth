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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.LocalDocumentReference;
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
    private static final List<String> AUTH_SPACE = List.of("XWiki", "Authentication");

    private static final LocalDocumentReference AUTH_DOC = new LocalDocumentReference(AUTH_SPACE, "Administration");

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
        navigateToAuthenticationAdmin();
        WebElement authService = getDriver().findElement(By.id("xwikicontent"));
        return authService.getText().contains("org.xwiki.contrib.oidc.auth.internal.OIDCAuthService");
    }

    private void navigateToAuthenticationAdmin()
    {
        getUtil().gotoPage(AUTH_DOC);
    }
}
