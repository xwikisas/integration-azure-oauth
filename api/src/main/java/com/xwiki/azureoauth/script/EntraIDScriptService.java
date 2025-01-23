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
package com.xwiki.azureoauth.script;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;

/**
 * Entra ID script services.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named("entraid")
@Singleton
@Unstable
public class EntraIDScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("default")
    private EntraIDConfiguration entraIDConfiguration;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Check if XWiki log in is enabled for guest users.
     *
     * @return {@code true} if global log in is enabled and {@code false} otherwise.
     */
    public boolean isXWikiLoginEnabled()
    {
        return entraIDConfiguration.isXWikiLoginGlobalEnabled();
    }

    /**
     * Check if the XWiki log in switch option should be displayed.
     *
     * @return {@code true} if the user is an Azure user and member of the allowed groups and {@code false} otherwise.
     * @throws XWikiException
     */
    public boolean shouldDisplayXWikiLogin() throws XWikiException
    {
        return isAzureUser() && isUserInGroups();
    }

    private boolean isAzureUser() throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki xwiki = wikiContext.getWiki();
        XWikiDocument userDoc = xwiki.getDocument(wikiContext.getUserReference(), wikiContext);
        return userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS)) != null;
    }

    private boolean isUserInGroups()
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki xwiki = wikiContext.getWiki();
        User user = xwiki.getUser(wikiContext.getUserReference(), wikiContext);
        return Stream.of(entraIDConfiguration.getXWikiLoginGroups().split(",")).anyMatch(user::isUserInGroup);
    }
}
