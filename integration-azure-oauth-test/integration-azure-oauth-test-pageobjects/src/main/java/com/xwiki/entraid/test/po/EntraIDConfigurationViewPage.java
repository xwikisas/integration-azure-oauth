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
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the administration configuration of Entra ID application.
 *
 * @version $Id$
 * @since 2.1
 */
public class EntraIDConfigurationViewPage extends ViewPage
{
    /**
     * Go to XWiki Main WebHome.
     */
    public WebElement getSyncButton()
    {
        return getDriver().findElement(By.className("entra-sync-action"));
    }

    public WebElement getDisableCheck()
    {
        return getDriver().findElement(By.cssSelector("#entra-sync input[name='disable']"));
    }

    public WebElement getRemoveCheck()
    {
        return getDriver().findElement(By.cssSelector("#entra-sync input[name='remove']"));
    }

    public WebElement getNotification()
    {
        return getDriver().findElement(By.className("xnotification"));
    }

    public WebElement getErrorNotification()
    {
        By locator = By.className("xnotification-error");
        getDriver().waitUntilElementIsVisible(locator);
        return getDriver().findElement(locator);
    }

    /**
     * Add an entry to the user mapping of the configuration.
     *
     * @param xwikiProperty the XWiki user property
     * @param providerProperty the provider property mapped to the XWiki user property
     * @since 2.2.5
     */
    public void addUserMapping(String xwikiProperty, String providerProperty)
    {
        WebElement entry = addMappingEntry("userMapping");
        entry.findElement(By.cssSelector("input.key")).sendKeys(xwikiProperty);
        entry.findElement(By.cssSelector("input.value")).sendKeys(providerProperty);
    }

    /**
     * Add an entry to the groups mapping of the configuration, selecting the XWiki group with the group picker.
     *
     * @param groupName the name of the XWiki group to look for
     * @param groupObjectID the Entra ID group object ID mapped to the XWiki group
     * @since 2.2.5
     */
    public void addGroupMapping(String groupName, String groupObjectID)
    {
        WebElement entry = addMappingEntry("groupsMapping");
        new SuggestInputElement(entry.findElement(By.cssSelector("input.key"))).sendKeys(groupName)
            .waitForSuggestions().selectByIndex(0);
        entry.findElement(By.cssSelector("input.value")).sendKeys(groupObjectID);
    }

    /**
     * @param propertyName the name of a map property of the OIDC client configuration (e.g. {@code userMapping})
     * @return the entries displayed for the given property, as {@code key=value}
     * @since 2.2.5
     */
    public List<String> getMappings(String propertyName)
    {
        return getMappingEntries(propertyName).stream()
            .map(entry -> entry.findElement(By.cssSelector("input.key")).getAttribute("value") + '='
                + entry.findElement(By.cssSelector("input.value")).getAttribute("value"))
            .filter(mapping -> !"=".equals(mapping)).collect(Collectors.toList());
    }

    /**
     * Add a group to the groups allowed to authenticate.
     *
     * @param group the group to add
     * @since 2.2.5
     */
    public void addAllowedGroup(String group)
    {
        new SuggestInputElement(getPropertyInput("allowedGroups")).sendKeys(group).selectTypedText();
    }

    /**
     * Save the configuration form and wait for the administration page to be reloaded.
     *
     * @since 2.2.5
     */
    public void saveConfiguration()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.cssSelector("form.xform input[name='formactionsac']")).click();
        getDriver().waitUntilPageIsReloaded();
    }

    private WebElement addMappingEntry(String propertyName)
    {
        WebElement container = getPropertyInput(propertyName).findElement(By.xpath(".."));
        int entriesCount = getMappingEntries(propertyName).size();
        container.findElement(By.className("addProperty")).click();
        getDriver().waitUntilCondition(driver -> getMappingEntries(propertyName).size() > entriesCount);
        List<WebElement> entries = getMappingEntries(propertyName);
        return entries.get(entries.size() - 1);
    }

    private List<WebElement> getMappingEntries(String propertyName)
    {
        return getPropertyInput(propertyName).findElement(By.xpath(".."))
            .findElements(By.cssSelector(".properties .property"));
    }

    private WebElement getPropertyInput(String propertyName)
    {
        return getDriver().findElement(By.id("XWiki.OIDC.ClientConfigurationClass_0_" + propertyName));
    }
}
