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
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the UI added by EntraID OIDC provider.
 *
 * @version $Id$
 * @since 2.0
 */
public class EntraIDViewPage extends ViewPage
{
    /**
     * Go to XWiki Main WebHome.
     */
    public void goToHomePage()
    {
        getUtil().gotoPage("Main", "WebHome");
    }

    public WebElement getLoginButton()
    {
        toggleDrawer();
        return getDriver().findElement(By.id("tmLogin"));
    }

    public Optional<WebElement> getBypassLoginButton()
    {
        toggleDrawer();
        List<WebElement> elements = getDriver().findElements(By.id("tmLogin-bypass"));
        return elements.stream().findFirst();
    }

    public boolean isSwitchUserDisplayed()
    {
        toggleDrawer();
        List<WebElement> webElements = getDriver().findElements(By.id("tmLogout-xwiki-switch"));
        return !webElements.isEmpty();
    }
}
