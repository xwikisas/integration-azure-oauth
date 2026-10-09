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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.configuration.AzureOldConfiguration;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;
import com.xwiki.azureoauth.internal.oldConfiguration.OldAzureOAuthConfiguration;
import com.xwiki.azureoauth.user.EntraIDUsersManager;

import static com.xwiki.azureoauth.internal.configuration.DefaultEntraIDConfiguration.OIDC_USER_CLASS;

/**
 * Migrate old Identity OAuth configurations to the new Entra ID and OIDC configurations. Refactor the issuer value in
 * the OIDC User class to be compatible to the one used by the OIDC application.
 *
 * @version $Id$
 * @since 2.0
 */
@Component(roles = AzureADOIDCMigrator.class)
@Singleton
@Unstable
public class AzureADOIDCMigrator
{
    private static final String INVALID_VERSION = "/2.0";

    private static final String VALID_VERSION = "/v2.0";

    private static final String ISSUER = "issuer";

    private static final String PROVIDER_ENDPOINT = "https://login.microsoftonline.com/%s/v2.0";

    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";

    @Inject
    @Named(OldAzureOAuthConfiguration.HINT)
    private Provider<AzureOldConfiguration> identityOAuthConfigurationProvider;

    @Inject
    @Named("default")
    private Provider<EntraIDConfiguration> entraIDConfigurationProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntraIDUsersManager usersManager;

    /**
     * Check if the current EntraID/OIDC configuration is empty and populate it with the old configuration from Azure
     * AD.
     *
     * @throws ConfigurationSaveException if any error occurs while saving the new configuration.
     */
    public void initializeOIDCConfiguration() throws ConfigurationSaveException
    {
        // XWiki might not be fully initialized yet, in which case it means we are not attempting to update the
        // configuration.
        if (getXWiki() != null) {
            EntraIDConfiguration entraIDConfiguration = this.entraIDConfigurationProvider.get();
            Map<String, Object> configurationMap = generateNewConfiguration();
            String tenantID = entraIDConfiguration.getTenantID();
            if (tenantID.isEmpty()) {
                entraIDConfiguration.setEntraIDConfiguration(getTenantIdConfiguration());
                configurationMap.putAll(getEndpoints(this.identityOAuthConfigurationProvider.get().getTenantID()));
                this.logger.info("Successfully set Entra ID configuration.");
            } else if (!tenantID.equals(entraIDConfiguration.getOIDCTenantID())) {
                // The OIDC configuration doesn't have the provider endpoint yet, so it still uses the separate
                // endpoints set by previous versions, which need to be replaced.
                // TODO: Remove this migration step once users have updated the application to a version above 2.2.3.
                configurationMap.putAll(getEndpoints(tenantID));
            }
            if (!configurationMap.isEmpty()) {
                entraIDConfiguration.setOIDCConfiguration(configurationMap);
                this.logger.info("Successfully set OIDC configuration.");
            }
        }
    }

    /**
     * Check the OIDC issuer for already existing Azure users to make sure it has the right format and refactor it if
     * needed.
     *
     * @throws QueryException if there is an error while executing the query to find the users.
     */
    public void refactorOIDCIssuer() throws QueryException, XWikiException
    {
        XWiki wiki = getXWiki();

        // XWiki might not be fully initialized yet, in which case it means we are not attempting to update the users.
        if (wiki != null) {
            List<XWikiDocument> users = this.usersManager.getXWikiUsers();
            for (XWikiDocument userDoc : users) {
                BaseObject oidcObj = userDoc.getXObject(this.documentReferenceResolver.resolve(OIDC_USER_CLASS));
                String issuer = oidcObj.getField(ISSUER).toFormString();
                if (issuer.endsWith(INVALID_VERSION)) {
                    int index = issuer.lastIndexOf(INVALID_VERSION);
                    String fixedIssuer = issuer.substring(0, index) + VALID_VERSION;
                    oidcObj.set(ISSUER, fixedIssuer, this.xcontextProvider.get());
                    wiki.saveDocument(userDoc, "Refactored OIDC issuer to the right format used by Entra ID.",
                        this.xcontextProvider.get());
                }
            }
        }
    }

    /**
     * Generates the provider and token endpoints required by the OIDC configuration, and resets the authorization and
     * logout endpoints set by previous versions, which are now discovered from the provider. The token endpoint is kept
     * since it is also used for the Entra ID users synchronization.
     *
     * @param tenantID the AD tenant ID.
     * @return the OIDC endpoints configuration.
     */
    public Map<String, Object> getEndpoints(String tenantID)
    {
        // TODO: Stop clearing the old authorization and logout endpoints once users have updated the application to
        // a version above 2.2.3.
        return Map.of("provider", String.format(PROVIDER_ENDPOINT, tenantID), "tokenEndpoint",
            String.format(TOKEN_ENDPOINT, tenantID), "authorizationEndpoint", "", "logoutEndpoint", "");
    }

    private XWiki getXWiki()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext != null ? xcontext.getWiki() : null;
    }

    private Map<String, Object> generateNewConfiguration()
    {
        Map<String, Object> newConfig = new HashMap<>();
        EntraIDConfiguration entraIDConfiguration = this.entraIDConfigurationProvider.get();
        AzureOldConfiguration oauthConfiguration = this.identityOAuthConfigurationProvider.get();
        if (entraIDConfiguration.getClientID().isEmpty()) {
            newConfig.put("clientId", oauthConfiguration.getClientID());
        }
        if (entraIDConfiguration.getSecret().isEmpty()) {
            newConfig.put("clientSecret", oauthConfiguration.getSecret());
        }
        return newConfig;
    }

    private Map<String, Object> getTenantIdConfiguration()
    {
        return Map.of("tenantId", this.identityOAuthConfigurationProvider.get().getTenantID());
    }
}
