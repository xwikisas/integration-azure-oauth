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

import java.io.File;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.identityoauth.IdentityOAuthException;
import com.xwiki.identityoauth.IdentityOAuthManager;
import com.xwiki.identityoauth.IdentityOAuthProvider;
import com.xwiki.licensing.Licensor;

/**
 * A provider to read identity based on OAuth/OpenID from MicroSoft Azure Active Directory.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("AzureAD")
@Singleton
public class AzureADIdentityOAuthProvider implements IdentityOAuthProvider
{
    /**
     * The string tenant_id.
     */
    public static final String TENANT_ID = "tenantid";

    /**
     * The string AzureAD.
     */
    public static final String PROVIDERNAME = "AzureAD";

    /**
     * The string "This extension is not licensed".
     */
    public static final String EXCEPTIONUNLICENSED = "This extension is not licensed.";

    protected static Class deepestClassOfHierarchy;

    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected DocumentReferenceResolver<String> documentResolver;

    @Inject
    protected Logger logger;

    @Inject
    protected Provider<Licensor> licensorProvider;

    @Inject
    protected Provider<IdentityOAuthManager> identityOAuthManager;

    protected DocumentReference configPageRef;

    protected static AzureADIdentityOAuthProvider deepestInstance = null;
    /**
     * The wiki reference to the WebPreferences page of the AzureAD space.
     */
    protected DocumentReference azureADWebPrefsRef;

    /**
     * The connection service to the azure Active-Directory API.
     */
    private OAuth20Service service;

    private List<String> scopes;

    private ThreadLocal<String> currentlyRequestedUrl = new ThreadLocal<>();

    private ThreadLocal<Map> currentlyObtainedJson = new ThreadLocal<>();

    /**
     * AD administrators can create users without email. If this setting is true, users that show up with a record that
     * has no email will have their email field be defined from the "user-principal", username@domain, which may or may
     * not be a valid email address.
     */
    private boolean acceptUserWithoutEmail = true;

    /**
     * Reads initialization from objects expecting key-names clientid, secret, scopes, redirectUrl and tenantid.
     *
     * @param config the map merging objects from the XWiki-Classes IdentityOAuthConfigClass and AzureADCnfigClass.
     */
    public void initialize(Map<String, String> config)
    {
        Class clz = this.getClass();
        if (deepestClassOfHierarchy != null) {
            if (deepestClassOfHierarchy.isAssignableFrom(clz)) {
                if (logger != null) {
                    logger.warn("Class " + clz + " is a specialization of " + deepestClassOfHierarchy
                            + "... marking IdentityOAuthProvider of class "
                            + deepestClassOfHierarchy + " as inactive.");
                }
                deepestInstance = this;
                deepestClassOfHierarchy = clz;
            }
        } else {
            deepestClassOfHierarchy = clz;
        }
        this.initialize(config.get("clientid"), config.get("secret"), config.get("scopes"),
                config.get("redirectUrl"), config.get(TENANT_ID), config.get("configurationObjectsPage"));
    }

    private void initialize(String clientId, String secret, String scopesParam,
            String redirectUrl, String tenantId, String configPage)
    {
        if (scopesParam == null || scopesParam.trim().length() == 0) {
            scopes = getMinimumScopes();
        } else {
            scopes = makeScopes(Arrays.asList(scopesParam.split(" ")));
        }
        StringBuilder usedScopes = new StringBuilder();
        for (String s : scopes) {
            usedScopes.append(s).append(" ");
        }
        //logger.debug("Scopes: " + usedScopes);
        service = new ServiceBuilder(clientId)
                .apiSecret(secret)
                .defaultScope(usedScopes.toString())
                //.httpClientConfig(ApacheHttpClientConfig.defaultConfig())
                .callback(redirectUrl)
                .build(MicrosoftAzureActiveDirectory20Api.custom(tenantId));

        configPageRef = documentResolver.resolve(configPage);
        azureADWebPrefsRef = documentResolver.resolve("xwiki:AzureAD.WebPreferences");

        if (logger != null) {
            logger.debug("MS-AD-Service configured: " + this);
        }
    }

    /**
     * Verifies that the configured object is activated, that the license is current, and that this object is the only
     * object of this class hierarchy, thus ensuring that the presence of a configured provider implementing a subclass
     * "neutralizes" any parent class. Note that this behaviour bases on the fact that all providers are instantiated
     * before being consulted for being active and that the class hierarchy does not have multiple leaves (in this case,
     * the elected subclass is not deterministic).
     *
     * @return true if the verification succeeded.
     */
    @Override public boolean isActive()
    {
        return licensorProvider.get().hasLicensure(azureADWebPrefsRef)
                && this.getClass().equals(deepestClassOfHierarchy);
    }

    @Override
    public List<String> getMinimumScopes()
    {
        return Arrays.asList("openid", "User.Read");
    }

    public static AzureADIdentityOAuthProvider getDeepestInstance() {
        return deepestInstance;
    }

    public DocumentReference getConfigPageRef() {
        return configPageRef;
    }

    public List<String> getConfigObjectsClasses() {
        return Arrays.asList("IdentityOAuth.IdentityOAuthConfigClass",
                "AzureAD.AzureADConfigClass");
    }

    @Override
    public String getRemoteAuthorizationUrl(String redirectUrl)
    {
        if (!licensorProvider.get().hasLicensure(azureADWebPrefsRef)) {
            throw new IllegalStateException(EXCEPTIONUNLICENSED);
        }
        String authorizationUrl = service.getAuthorizationUrl();
        logger.debug("Authorization URL: " + authorizationUrl);
        return authorizationUrl;
    }


    @Override
    public Pair<String, Date> createToken(String authCode)
    {
        try {
            if (!licensorProvider.get().hasLicensure(azureADWebPrefsRef)) {
                throw new IllegalStateException(EXCEPTIONUNLICENSED);
            }

            OAuth2AccessToken accessToken = service.getAccessToken(authCode);
            logger.debug("Obtained accessToken from MS-AD Services.");
            // TODO: change return type to an object that contains expiry (and is serializable...)
            Date expiry = new Date(System.currentTimeMillis() + 1000 * accessToken.getExpiresIn());
            return new ImmutablePair<>(accessToken.getAccessToken(), expiry);
        } catch (Exception e) {
            logger.warn("Trouble at creating Token:", e);
            throw new IdentityOAuthException("Trouble at creating Token.", e);
        }
    }

    @Override
    public String readAuthorizationFromReturn(Map<String, String[]> params)
    {
        String code = params.get("code")[0];
        logger.debug("Obtained authorization-code from MS-AD Services.");
        return code;
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
            identityOAuthManager.get().requestCurrentToken(getProviderName());
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
            OAuthRequest request =
                    new OAuthRequest(Verb.GET, currentlyRequestedUrl.get());
            service.signRequest(token, request);
            Response response = service.execute(request);
            String responseBody = response.getBody();
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
    public IdentityDescription fetchIdentityDetails(String token)
    {
        try {
            if (!licensorProvider.get().hasLicensure(azureADWebPrefsRef)) {
                throw new IllegalStateException(EXCEPTIONUNLICENSED);
            }

            OAuthRequest request;

            request = new OAuthRequest(Verb.GET, "https://graph.microsoft.com/v1.0/me");
            service.signRequest(token, request);
            Response response = service.execute(request);
            String responseBody = response.getBody();
            Map json = new ObjectMapper().readValue(responseBody, Map.class);
            IdentityDescription id = new MSADIdentityDescription(json);

            // add photo metadata
            // https://docs.microsoft.com/en-us/graph/api/profilephoto-get?view=graph-rest-1.0
            try {
                if (scopes.contains("avatar")) {
                    final List<String> supportedMediaTypes = Arrays.asList("image/jpeg");
                    request = new OAuthRequest(Verb.GET,
                            "https://graph.microsoft.com/v1.0/users/" + id.internalId + "/photo");
                    service.signRequest(token, request);
                    Response photoResponse = service.execute(request);
                    String mediaType = photoResponse.getHeader("Content-Type");
                    if (photoResponse.isSuccessful()
                            && supportedMediaTypes.contains(mediaType))
                    {
                        File tmpFile = File.createTempFile("photo", ".jpg");
                        InputStream in = photoResponse.getStream();
                        FileUtils.copyInputStreamToFile(in, tmpFile);
                        id.fetchedUserImage = tmpFile.toURL();
                    } else {
                        logger.warn("Fetching photo failed: " + photoResponse.getMessage());
                        logger.debug("Photo response: " + photoResponse.getBody());
                    }
                }
            } catch (Throwable e) {
                logger.warn("Can't save photo.", e);
            }

            return id;
        } catch (Exception e) {
            logger.warn("Trouble at fetchIdentityDetails:", e);
            throw new IdentityOAuthException("Trouble at fetchIdentityDetails.", e);
        }
    }

    @Override
    public boolean enrichUserObject(IdentityDescription idDescription, XWikiDocument doc)
    {
        MSADIdentityDescription id = (MSADIdentityDescription) idDescription;
        // TODO: explore the information that can be collected and be inserted in
        //  the XWiki profile, e.g. photos, URLs, ...
        return false;
    }

    private List<String> makeScopes(List<String> proposedScopes)
    {
        return proposedScopes == null || proposedScopes.size() == 0 ? getMinimumScopes() : proposedScopes;
    }

    @Override
    public String getProviderName()
    {
        return PROVIDERNAME;
    }

    @Override
    public void setProviderName(String name)
    {
        if (!this.PROVIDERNAME.equals(name)) {
            throw new IllegalStateException("Only \"AzureAD\" is accepted as name.");
        }
    }

    @Override
    public String validateConfiguration()
    {
        return "ok";
    }

    private final class MSADIdentityDescription extends IdentityDescription
    {
        private final Map json;

        private MSADIdentityDescription(Map jsonRecord)
        {
            this.json = jsonRecord;
            // found entries (2020-10-02):
            //   businessPhone, displayName, givenName, jobTitle, mail, mobilePhone, officeLocation,
            //   preferredLanguage, surname, userPrincipalName, id
            firstName = json.get("givenName").toString();
            lastName = json.get("surname").toString();
            internalId = json.get("id").toString();
            String providedEmail = (String) json.get("mail");
            if (providedEmail != null) {
                emails = Collections.singletonList(providedEmail);
            } else if (acceptUserWithoutEmail) {
                emails = Collections.singletonList(json.get("userPrincipalName").toString());
            }
        }
    }
}
