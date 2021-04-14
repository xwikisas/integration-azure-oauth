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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.xpn.xwiki.XWikiContext;
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

    private static final String IMAGE_JPEG = "image/jpeg";

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

    /**
     * The wiki reference to the WebPreferences page of the AzureAD space.
     */
    protected DocumentReference azureADWebPrefsRef;

    /**
     * The connection service to the azure Active-Directory API.
     */
    private OAuth20Service service;

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

        service = new ServiceBuilder(clientId)
                .apiSecret(secret)
                .defaultScope(usedScopes.toString())
                //.httpClientConfig(ApacheHttpClientConfig.defaultConfig())
                .callback(redir)
                .build(MicrosoftAzureActiveDirectory20Api.custom(tenantId));

        configPageRef = documentResolver.resolve(configPage);
        azureADWebPrefsRef = documentResolver.resolve("xwiki:AzureAD.WebPreferences");
        logger.debug("MS-AD-Service configured: " + this);
    }

    /**
     * Verifies that the configured object is activated, that the license is current.
     *
     * @return true if the verification succeeded.
     */
    @Override public boolean isActive()
    {
        return licensorProvider.get().hasLicensure(azureADWebPrefsRef) && active;
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
        } catch (OAuth2AccessTokenErrorResponse e) {
            String msg = "OAuth trouble at creating token:" + e.getErrorDescription();
            logger.warn(msg, e);
            throw new IdentityOAuthException(msg, e);
        } catch (Exception e) {
            String msg = "Generic trouble at creating Token: " + e.toString();
            logger.warn(msg, e);
            throw new IdentityOAuthException(msg, e);
        }
    }

    @Override
    public String readAuthorizationFromReturn(Map<String, String[]> params)
    {
        String errorDescription = "error_description";
        if (params.containsKey(errorDescription)) {
            throw new IdentityOAuthException("An error occurred at AzureAD:"
                    + " " + Arrays.asList(params.get("error"))
                    + " " + Arrays.asList(params.get(errorDescription)));
        }
        String codeP = "code";
        String code = params != null && params.containsKey(codeP) ? params.get(codeP)[0] : null;
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
    public AbstractIdentityDescription fetchIdentityDetails(String token)
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
            MSADIdentityDescription id = new MSADIdentityDescription(json);

            return id;
        } catch (Exception e) {
            logger.warn("Trouble at fetchIdentityDetails:", e);
            throw new IdentityOAuthException("Trouble at fetchIdentityDetails.", e);
        }
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
        // add photo metadata
        // https://docs.microsoft.com/en-us/graph/api/profilephoto-get?view=graph-rest-1.0
        try {
            if (scopes.contains("User.ReadBasic.All") || scopes.contains("User.Read.All")) {
                OAuthRequest request;
                final List<String> supportedMediaTypes = Arrays.asList(IMAGE_JPEG);
                request = new OAuthRequest(Verb.GET, id.userImageUrl);
                if (ifModifiedSince != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                    sdf.setTimeZone(TimeZone.getTimeZone("CET"));
                    String ifms = sdf.format(ifModifiedSince);
                    request.addHeader("If-Modified-Since", ifms);
                }
                logger.debug("will request " + request);
                service.signRequest(token, request);
                Response photoResponse = service.execute(request);
                String mediaType = photoResponse.getHeader("Content-Type");
                logger.debug("Request done " + mediaType);
                if (photoResponse.isSuccessful()
                        && supportedMediaTypes.contains(mediaType))
                {
                    String contentDispo = photoResponse.getHeader("Content-Disposition");
                    String fileName = "image.jpeg";
                    String at = "attachment; ";
                    if (contentDispo != null && contentDispo.startsWith(at)) {
                        fileName = contentDispo.substring(at.length());
                    }
                    logger.debug("Obtained content of file " + fileName);
                    return Triple.of(photoResponse.getStream(), IMAGE_JPEG, fileName);
                } else {
                    logger.warn("Fetching photo failed: " + photoResponse.getMessage());
                    logger.debug("Photo response: " + photoResponse.getBody());
                    return null;
                }
            }
        } catch (Throwable e) {
            logger.warn("Can't save photo.", e);
        }
        return null;
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
        if (!this.PROVIDERHINT.equals(hint)) {
            throw new IllegalStateException("Only \"AzureAD\" is accepted as hint.");
        }
    }

    @Override
    public String validateConfiguration()
    {
        return "ok";
    }

    private final class MSADIdentityDescription extends AbstractIdentityDescription
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
            return "https://login.microsoftonline.com/" + tenantId + "/2.0";
        }
    }
}
