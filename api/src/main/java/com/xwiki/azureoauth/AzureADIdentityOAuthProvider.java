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
package com.xwiki.azureoauth;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.identityoauth.IdentityOAuthException;
import com.xwiki.identityoauth.IdentityOAuthManager;
import com.xwiki.identityoauth.IdentityOAuthProvider;
import com.xwiki.identityoauth.internal.IdentityOAuthConstants;
import com.xwiki.licensing.Licensor;

/**
 * A provider to read identity based on OAuth/OpenID from Microsoft Azure Active Directory.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("AzureAD")
@Singleton
public class AzureADIdentityOAuthProvider implements IdentityOAuthProvider
{
    private static final String TENANT_ID = "tenantid";

    private static final String PROVIDERHINT = "AzureAD";

    private static final String EXCEPTIONUNLICENSED = "This extension is not licensed.";

    @Inject
    protected DocumentReferenceResolver<String> documentResolver;

    @Inject
    protected Logger logger;

    @Inject
    protected Provider<Licensor> licensorProvider;

    @Inject
    protected Provider<IdentityOAuthManager> identityOAuthManager;

    protected DocumentReference configPageRef;

    @Inject
    private AzureADOAuthClient oauthClient;

    private ExtensionId thisExtensionId =
            new ExtensionId("com.xwiki.integration-azure-oauth:integration-azure-oauth-ui");

    private List<String> scopes;

    private boolean active;

    private String tenantId;

    private ThreadLocal<String> currentlyRequestedUrl = new ThreadLocal<>();

    private ThreadLocal<Map> currentlyObtainedJson = new ThreadLocal<>();

    /**
     * Reads initialization from objects expecting key-names clientid, secret, scopes, redirectUrl and tenantid.
     *
     * @param config the map merging objects from the XWiki-Classes IdentityOAuthConfigClass and AzureADCnfigClass.
     */
    public void initialize(Map<String, String> config)
    {
        this.active = false;
        try {
            this.initialize(config.get("active"), config.get("clientid"), config.get("secret"), config.get("scope"),
                    config.get("redirectUrl"), config.get(TENANT_ID),
                    config.get("configurationObjectsPage"));
        } catch (Exception e) {
            logger.warn("Configuration reading failed.", e);
            throw new IdentityOAuthException("Trouble at reading configuration.", e);
        }
    }

    private void initialize(String activeParam, String clientId, String secret, String scopesParam,
            String redirectUrl, String tenantId, String configPage)
    {

        this.tenantId = tenantId;

        if (scopesParam == null || scopesParam.trim().length() == 0) {
            scopes = getMinimumScopes();
        } else {
            scopes = makeScopes(Arrays.asList(scopesParam.split(" ")));
        }
        StringBuilder usedScopes = new StringBuilder();
        for (String s : scopes) {
            usedScopes.append(s).append(" ");
        }
        this.active = activeParam.equals("1") || Boolean.parseBoolean(activeParam);
        logger.debug("Configuring class " + this.getClass().getSimpleName()
                + " with: \n - scopes: " + scopes + "\n - clientId " + clientId);

        String redir = redirectUrl;
        if (redir == null || redir.trim().length() == 0) {
            redir = IdentityOAuthConstants.CHANGE_ME_LOGIN_URL;
        }

        oauthClient.buildService(clientId, secret, usedScopes.toString(), redir, tenantId);

        configPageRef = documentResolver.resolve(configPage);
        logger.debug("MS-AD-Service configured: " + this);
    }

    /**
     * Verifies that the configured object is activated.
     *
     * @return true if the verification succeeded.
     */
    @Override public boolean isActive()
    {
        return active;
    }

    /**
     * Verifies that the license is current every five minutes.
     *
     * @return true if license was valid in the last five minutes.
     */
    @Override public boolean isReady()
    {
        return licensorProvider.get().hasLicensure(thisExtensionId);
    }

    /**
     * @return Returns "openid", "User.Read.
     */
    @Override
    public List<String> getMinimumScopes()
    {
        return Arrays.asList("openid", "User.Read");
    }

    /**
     * @return the reference to the page.
     */
    public DocumentReference getConfigPageRef()
    {
        return configPageRef;
    }

    /**
     * Receives the reference to the page from the provider.
     *
     * @param page the string reference
     */
    @Override public void setConfigPage(String page)
    {
        this.configPageRef = documentResolver.resolve(page);
    }

    /**
     * @return The list of objects' classes to be found in the config page (used for the admin screen).
     */
    public List<String> getConfigObjectsClasses()
    {
        return Arrays.asList("IdentityOAuth.IdentityOAuthConfigClass",
                "AzureAD.AzureADConfigClass");
    }

    @Override
    public String getRemoteAuthorizationUrl(String redirectUrl)
    {
        if (!isReady()) {
            throw new IllegalStateException(EXCEPTIONUNLICENSED);
        }
        String authorizationUrl = oauthClient.getAuthorizationUrl();
        logger.debug("Authorization URL: " + authorizationUrl);
        return authorizationUrl;
    }

    @Override
    public Pair<String, Date> createToken(String authCode)
    {
        if (!isReady()) {
            throw new IllegalStateException(EXCEPTIONUNLICENSED);
        }
        return oauthClient.createToken(authCode);
    }

    @Override
    public String readAuthorizationFromReturn(Map<String, String[]> params)
    {
        return oauthClient.readAuthorizationFromReturn(params);
    }

    /**
     * Method for use by sub-classes to perform signed API-calls signed by a current authorization token.
     *
     * @param url the service URL.
     * @return a map of values as parsed by a plain Jackson's {@link ObjectMapper}.
     */
    protected Map makeApiCall(String url)
    {
        Map returnValue;
        try {
            currentlyRequestedUrl.set(url);
            identityOAuthManager.get().requestCurrentToken(getProviderHint());
            returnValue = currentlyObtainedJson.get();
        } catch (Exception e) {
            if (e instanceof IdentityOAuthException) {
                throw (IdentityOAuthException) e;
            } else {
                throw new IdentityOAuthException("Trouble at API call.", e);
            }
        } finally {
            currentlyRequestedUrl.remove();
            currentlyObtainedJson.remove();
        }
        return returnValue;
    }

    /**
     * Inner part of {@link #makeApiCall(String)}, using the token.
     *
     * @param token a currently valid token used to sign the API call
     */
    @Override public void receiveFreshToken(String token)
    {
        try {
            String responseBody = oauthClient.performApiRequest(token, currentlyRequestedUrl.get());
            if (logger.isDebugEnabled()) {
                logger.debug("Response received: " + responseBody);
            }
            Map json = new ObjectMapper().readValue(responseBody, Map.class);
            currentlyObtainedJson.set(json);
        } catch (Exception e) {
            throw new IdentityOAuthException("Failure at API call.", e);
        }
    }

    @Override
    public AbstractIdentityDescription fetchIdentityDetails(String token)
    {
        if (!isReady()) {
            throw new IllegalStateException(EXCEPTIONUNLICENSED);
        }
        return oauthClient.fetchIdentityDetails(token, tenantId);
    }

    /**
     * Opens the stream of the user image file if it was modified later than the given date.
     *
     * @param ifModifiedSince Only fetch the file if it is modified after this date.
     * @param id              the currently collected identity-description.
     * @param token           the currently valid token.
     * @return A triple made of inputstream, media-type, and possibly guessed filename.
     */
    public Triple<InputStream, String, String> fetchUserImage(Date ifModifiedSince, AbstractIdentityDescription id,
            String token)
    {
        return oauthClient.fetchUserImage(ifModifiedSince, id, token, scopes);
    }

    @Override
    public boolean enrichUserObject(AbstractIdentityDescription idDescription, XWikiDocument doc)
    {
        MSADIdentityDescription id = (MSADIdentityDescription) idDescription;
        return false;
    }

    private List<String> makeScopes(List<String> proposedScopes)
    {
        return proposedScopes == null || proposedScopes.size() == 0 ? getMinimumScopes() : proposedScopes;
    }

    @Override
    public String getProviderHint()
    {
        return PROVIDERHINT;
    }

    @Override
    public void setProviderHint(String hint)
    {
        if (!PROVIDERHINT.equals(hint)) {
            throw new IllegalStateException("Only \"AzureAD\" is accepted as hint.");
        }
    }

    @Override
    public String validateConfiguration()
    {
        return "ok";
    }

    static final class MSADIdentityDescription extends AbstractIdentityDescription
    {
        private final Map json;

        private String issuerId;

        MSADIdentityDescription(Map jsonRecord, String issuerId)
        {
            this.issuerId = issuerId;
            this.json = jsonRecord;
            // found entries (2020-10-02):
            //   businessPhone, displayName, givenName, jobTitle, mail, mobilePhone, officeLocation,
            //   preferredLanguage, surname, userPrincipalName, id
            firstName = json.get("givenName") + "";
            lastName = json.get("surname") + "";
            internalId = json.get("id") + "";
            String providedEmail = (String) json.get("mail");
            if (providedEmail != null) {
                emails = Collections.singletonList(providedEmail);
            } else {
                /*
                 * AD administrators can create users without email. If this entering here, users that show up
                 * with a record that has no email will have their email field be defined from the "user-principal",
                 * username@domain, which may or may not be a valid email address.
                 */
                emails = Collections.singletonList(json.get("userPrincipalName").toString());
            }
            this.userImageUrl = "https://graph.microsoft.com/v1.0/users/" + internalId + "/photo/$value";
        }

        @Override public String getIssuerURL()
        {
            return "https://login.microsoftonline.com/" + issuerId + "/2.0";
        }
    }
}
