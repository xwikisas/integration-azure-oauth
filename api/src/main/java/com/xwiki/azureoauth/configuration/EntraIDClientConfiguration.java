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
package com.xwiki.azureoauth.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.objects.BaseObject;

/**
 * The configuration of an Entra ID OIDC client, read from a document holding both an OIDC client configuration object
 * and an Entra ID configuration object.
 *
 * @version $Id$
 * @since 2.2.5
 */
@Unstable
public class EntraIDClientConfiguration
{
    private static final String ENTRA_ISSUER_HOST = "login.microsoftonline.com/";

    /**
     * The tenant aliases used by multi-tenant applications. The users authenticated through them are issued by their
     * own tenant, so the configured tenant can't be used to find them: such a configuration matches the users of any
     * tenant.
     */
    private static final Set<String> MULTI_TENANT_ALIASES = Set.of("common", "organizations", "consumers");

    private final DocumentReference documentReference;

    private final String configurationName;

    private final String tenantID;

    private final String clientID;

    private final String secret;

    private final String tokenEndpoint;

    private final boolean active;

    private final String xwikiLoginGroups;

    /**
     * Read the configuration from the objects of a document. The objects are not kept, so this configuration doesn't
     * change when the document is modified.
     *
     * @param documentReference the document holding the configuration
     * @param oidcObject the OIDC client configuration object, or {@code null} if the document doesn't have one
     * @param entraIDObject the Entra ID configuration object, or {@code null} if the document doesn't have one
     */
    public EntraIDClientConfiguration(DocumentReference documentReference, BaseObject oidcObject,
        BaseObject entraIDObject)
    {
        this.documentReference = documentReference;
        this.configurationName = getStringValue(oidcObject, "configurationName");
        this.clientID = getStringValue(oidcObject, "clientId");
        this.secret = getStringValue(oidcObject, "clientSecret");
        this.tokenEndpoint = getStringValue(oidcObject, "tokenEndpoint");
        this.active = oidcObject == null || oidcObject.getIntValue("skipped", 0) != 1;
        this.tenantID = getStringValue(entraIDObject, "tenantId");
        this.xwikiLoginGroups = getStringValue(entraIDObject, "xwikiLoginGroups");
    }

    /**
     * @return the document holding this configuration
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     * @return the name of the OIDC client configuration
     */
    public String getConfigurationName()
    {
        return this.configurationName;
    }

    /**
     * @return the Entra ID tenant ID, which must be the GUID of the tenant for its users to be matched (see
     *     {@link #matchesIssuer(String)})
     */
    public String getTenantID()
    {
        return this.tenantID;
    }

    /**
     * @return the client ID
     */
    public String getClientID()
    {
        return this.clientID;
    }

    /**
     * @return the client secret
     */
    public String getSecret()
    {
        return this.secret;
    }

    /**
     * @return the token endpoint
     */
    public String getTokenEndpoint()
    {
        return this.tokenEndpoint;
    }

    /**
     * @return {@code true} if the users can authenticate with this configuration, {@code false} otherwise
     */
    public boolean isActive()
    {
        return this.active;
    }

    /**
     * @return the groups whose members are allowed to switch to the standard XWiki authenticator
     */
    public List<String> getXWikiLoginGroups()
    {
        return Arrays.asList(StringUtils.split(this.xwikiLoginGroups, ','));
    }

    /**
     * Check whether a user authenticated with the given OIDC issuer belongs to the tenant of this configuration.
     *
     * @param issuer the OIDC issuer stored in the profile of the user
     * @return {@code true} if the issuer belongs to the tenant of this configuration, {@code false} otherwise
     */
    public boolean matchesIssuer(String issuer)
    {
        if (issuer == null || !issuer.contains(ENTRA_ISSUER_HOST)) {
            return false;
        }
        if (!hasSpecificTenant()) {
            return true;
        }
        // Entra ID issues the tenant GUID in lower case, while the configured one may be in upper case.
        return issuer.toLowerCase(Locale.ROOT)
            .contains(ENTRA_ISSUER_HOST + this.tenantID.toLowerCase(Locale.ROOT) + '/');
    }

    /**
     * @return {@code true} if this configuration targets a single tenant, {@code false} if its tenant is blank or a
     *     multi-tenant alias, in which case it matches the users of any tenant
     * @see #matchesIssuer(String)
     */
    public boolean hasSpecificTenant()
    {
        return StringUtils.isNotBlank(this.tenantID)
            && !MULTI_TENANT_ALIASES.contains(this.tenantID.toLowerCase(Locale.ROOT));
    }

    private static String getStringValue(BaseObject object, String property)
    {
        return object != null ? StringUtils.defaultString(object.getStringValue(property)) : "";
    }
}
