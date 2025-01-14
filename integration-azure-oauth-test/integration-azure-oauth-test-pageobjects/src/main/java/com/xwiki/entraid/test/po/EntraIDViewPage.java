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
import org.xwiki.test.ui.po.ViewPage;

public class EntraIDViewPage extends ViewPage
{
    @FindBy(xpath = "//a[@id='tmLogin']")
    private WebElement loginLink;

    @FindBy(xpath = "//a[@id='tmLogin-bypass']")
    private WebElement bypassLogin;

    @FindBy(xpath = "//a[@id='tmLogout']")
    private WebElement logoutLink;

    @FindBy(xpath = "//a[@id='tmLogout-bypass']")
    private WebElement bypassLogout;

    public void clickLogin()
    {
        toggleDrawer();
        this.loginLink.click();
    }

    public void clickLoginBypass()
    {
        toggleDrawer();
        this.bypassLogin.click();
    }
}
