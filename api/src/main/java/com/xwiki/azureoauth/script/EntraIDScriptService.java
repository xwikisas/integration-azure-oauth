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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.configuration.EntraIDClientConfiguration;
import com.xwiki.azureoauth.configuration.EntraIDClientConfigurationStore;
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

    @Inject
    private EntraIDClientConfigurationStore clientConfigurationStore;

    @Inject
    private Logger logger;

    /**
     * Check if XWiki log in is enabled for guest users.
     *
     * @return {@code true} if global log in is enabled and {@code false} otherwise. Only the default configuration is
     *     taken into account, so that adding an Entra ID client configuration can't enable it for guest users.
     */
    public boolean isXWikiLoginEnabled()
    {
        return entraIDConfiguration.isXWikiLoginGlobalEnabled() || getClientConfigurations().stream()
            .anyMatch(configuration -> configuration.isActive() && configuration.isXWikiLoginGlobalEnabled());
    }

    /**
     * Check if the XWiki log in switch option should be displayed.
     *
     * @return {@code true} if the user is an Azure user and member of the groups allowed by the Entra ID client
     *     configurations of the user's tenant (or by the default configuration if there is none), and {@code false}
     *     otherwise.
     * @throws XWikiException
     */
    public boolean shouldDisplayXWikiLogin() throws XWikiException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        DocumentReference userReference = wikiContext.getUserReference();
        if (userReference == null) {
            return false;
        }
        XWikiDocument userDoc = wikiContext.getWiki().getDocument(userReference, wikiContext);
        BaseObject oidcUserObject = userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS));
        if (oidcUserObject == null) {
            return false;
        }
        String issuer = oidcUserObject.getStringValue("issuer");
        List<EntraIDClientConfiguration> userConfigurations = getClientConfigurations().stream()
            .filter(configuration -> configuration.matchesIssuer(issuer)).collect(Collectors.toList());
        Stream<String> groups;
        if (userConfigurations.isEmpty()) {
            groups = Stream.of(entraIDConfiguration.getXWikiLoginGroups().split(","));
        } else {
            groups = userConfigurations.stream().flatMap(configuration -> configuration.getXWikiLoginGroups().stream());
        }
        User user = wikiContext.getWiki().getUser(userReference, wikiContext);
        return groups.anyMatch(user::isUserInGroup);
    }

    private List<EntraIDClientConfiguration> getClientConfigurations()
    {
        try {
            return clientConfigurationStore.getConfigurations();
        } catch (Exception e) {
            logger.warn("Failed to retrieve the Entra ID client configurations. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return Collections.emptyList();
        }
    }
}
