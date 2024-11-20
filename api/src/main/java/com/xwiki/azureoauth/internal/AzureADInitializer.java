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
package com.xwiki.azureoauth.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.configuration.AzureConfiguration;
import com.xwiki.azureoauth.configuration.AzureOldConfiguration;
import com.xwiki.azureoauth.internal.oldConfiguration.OldOAuthAzureConfiguration;

import static com.xwiki.azureoauth.internal.configuration.DefaultAzureConfiguration.OIDC_USER_CLASS;

/**
 * Handles the initialization of Azure AD with OIDC integration.
 *
 * @version $Id$
 * @since 2.0
 */
@Component(roles = AzureADInitializer.class)
@Singleton
@Unstable
public class AzureADInitializer
{
    private static final String INVALID_VERSION = "/2.0";

    private static final String VALID_VERSION = "/v2.0";

    private static final String ISSUER = "issuer";

    @Inject
    @Named(OldOAuthAzureConfiguration.HINT)
    private Provider<AzureOldConfiguration> oauthConfigurationProvider;

    @Inject
    @Named("default")
    private Provider<AzureConfiguration> azureClientConfigurationProvider;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private InstalledExtensionRepository installedRepository;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Refactor OIDC issuer for Azure users and if the new version of the API module of Integration Azure consists with
     * the first version of OIDC integration, transfer the old configurations to the new OIDC client configuration
     * class.
     *
     * @throws ConfigurationSaveException if any error occurs while saving the new configuration.
     */
    public void initializeConfiguration() throws ConfigurationSaveException
    {
        InstalledExtension apiModule = installedRepository.getInstalledExtension(
            new ExtensionId("com.xwiki.integration-azure-oauth:integration-azure-oauth-api", "2.0"));
        if (apiModule != null) {
            azureClientConfigurationProvider.get().setOIDCConfiguration(generateNewConfigurationMap());
        }
    }

    /**
     * Refactor OIDC issuer for already existing Azure users.
     *
     * @throws QueryException if there is an error while executing the query to find the users.
     */
    public void refactorOIDCIssuer() throws QueryException, XWikiException
    {
        List<String> results = this.queryManager.createQuery(
                ", BaseObject as obj where doc.fullName = obj" + ".name and obj.className = :className", Query.HQL)
            .setWiki(this.wikiManager.getCurrentWikiId()).bindValue("className", OIDC_USER_CLASS).execute();
        XWikiContext wikiContext = wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();

        for (String userRef : results) {
            XWikiDocument userDoc = wiki.getDocument(documentReferenceResolver.resolve(userRef), wikiContext);
            BaseObject oidcObj = userDoc.getXObject(documentReferenceResolver.resolve(OIDC_USER_CLASS));
            String issuer = oidcObj.getField(ISSUER).toFormString();
            if (issuer.endsWith(INVALID_VERSION)) {
                int index = issuer.lastIndexOf(INVALID_VERSION);
                String fixedIssuer = issuer.substring(0, index) + VALID_VERSION;
                oidcObj.set(ISSUER, fixedIssuer, wikiContext);
                wiki.saveDocument(userDoc, "Fixed issuer", wikiContext);
            }
        }
    }

    /**
     * Generates the endpoints that are required by OIDC configuration.
     *
     * @param tenantID the AD tenant ID.
     * @return the formatted endpoints.
     */
    public Map<String, Object> getEndpointsMap(String tenantID)
    {
        return Map.of("authorizationEndpoint", getFormattedEndpoint(tenantID, "authorize"), "tokenEndpoint",
            getFormattedEndpoint(tenantID, "token"), "logoutEndpoint", getFormattedEndpoint(tenantID, "logout"));
    }

    private Map<String, Object> generateNewConfigurationMap()
    {
        AzureOldConfiguration oauthConfiguration = oauthConfigurationProvider.get();
        String tenantID = oauthConfiguration.getTenantID();
        Map<String, Object> newConfig = new HashMap<>(getEndpointsMap(tenantID));
        newConfig.put("clientId", oauthConfiguration.getClientID());
        newConfig.put("clientSecret", oauthConfiguration.getSecret());
        newConfig.put("skipped", !oauthConfiguration.isActive());
        newConfig.put("scope", oauthConfiguration.getScope());
        return newConfig;
    }

    private String getFormattedEndpoint(String tenantID, String scope)
    {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/%s", tenantID, scope);
    }
}
