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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.Select;
import org.xwiki.test.ui.po.ViewPage;

public class AuthServiceViewPage extends ViewPage
{
    @FindBy(id = "authServiceId")
    private WebElement authServiceSelectElement;

    @FindBy(name = "setauthservice")
    private WebElement saveButton;

    @FindBy(className = "codeToExecute")
    private WebElement authService;

    public void goToPage()
    {
        getUtil().gotoPage(
            "http://localhost:8080/xwiki/bin/admin/XWiki/XWikiPreferences?editor=globaladmin&section=Authentication");
    }

    public void switchToOIDCAuthenticationService()
    {
        Select authServiceSelect = new Select(authServiceSelectElement);
        authServiceSelect.selectByValue("oidc");
        saveButton.click();
    }

    public WebElement getAuthServiceUI()
    {
        return authService;
    }
}
