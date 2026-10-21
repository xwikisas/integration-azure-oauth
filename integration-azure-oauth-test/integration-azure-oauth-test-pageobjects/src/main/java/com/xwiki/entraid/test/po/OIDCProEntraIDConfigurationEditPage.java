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

import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.xwiki.oidcpro.test.po.OIDCProAdministrationPage;
import com.xwiki.oidcpro.test.po.OIDCProConfigurationEditPage;

/**
 * Represents the edit form of an OIDC Pro client configuration created from the Entra ID template.
 *
 * @version $Id$
 * @since 2.3.0
 */
public class OIDCProEntraIDConfigurationEditPage extends OIDCProConfigurationEditPage
{
    private static final String TEMPLATE_ID = "entraid";

    /**
     * Create an OIDC Pro client configuration from the Entra ID template, using the OIDC Pro administration section.
     * The edit form is opened in a new window, which becomes the current one.
     *
     * @return the edit form of the new configuration
     */
    public static OIDCProEntraIDConfigurationEditPage createConfiguration()
    {
        OIDCProAdministrationPage.gotoPage();
        Set<String> windows = getUtil().getDriver().getWindowHandles();
        WebElement createMenu =
            getUtil().getDriver().findElement(By.cssSelector("button.oidcpro-config-create")).findElement(By.xpath(".."));
        createMenu.findElement(By.className("dropdown-toggle")).click();
        WebElement template = createMenu.findElement(
            By.cssSelector(String.format("li.oidcpro-config-create[data-templateid='%s'] a", TEMPLATE_ID)));
        getUtil().getDriver().waitUntilCondition(driver -> template.isDisplayed());
        template.click();
        String newWindow = getUtil().getDriver().waitUntilCondition(driver -> driver.getWindowHandles().stream()
            .filter(handle -> !windows.contains(handle)).findFirst().orElse(null));
        getUtil().getDriver().switchTo().window(newWindow);
        return new OIDCProEntraIDConfigurationEditPage();
    }

    /**
     * @param tenantId the Entra ID tenant from which the OIDC endpoints are derived
     * @return this page
     */
    public OIDCProEntraIDConfigurationEditPage setTenantId(String tenantId)
    {
        WebElement field = getDriver().findElement(By.id("EntraID.Code.EntraIDConfigurationClass_0_tenantId"));
        field.clear();
        field.sendKeys(tenantId);
        return this;
    }

    /**
     * Add an entry to the user mapping of the configuration, using the properties editor of the template.
     *
     * @param xwikiProperty the XWiki user property
     * @param providerProperty the provider property mapped to the XWiki user property
     * @return this page
     */
    public OIDCProEntraIDConfigurationEditPage addUserMapping(String xwikiProperty, String providerProperty)
    {
        // The template uses the same properties editor as the administration section.
        new EntraIDConfigurationViewPage().addUserMapping(xwikiProperty, providerProperty);
        return this;
    }
}
