package com.xwiki.azureoauth;
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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessTokenErrorResponse;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.xwiki.identityoauth.IdentityOAuthException;
import com.xwiki.identityoauth.IdentityOAuthProvider;

/**
 * Microsoft Azure Active Directory authentication client.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = AzureADOAuthClient.class)
@Singleton
public class AzureADOAuthClient
{
    private static final String IMAGE_JPEG = "image/jpeg";

    @Inject
    protected Logger logger;

    /**
     * The connection service to the azure Active-Directory API.
     */
    private OAuth20Service service;

    void buildService(String clientId, String secret, String usedScopes, String redir, String tenantId)
    {
        service = new ServiceBuilder(clientId)
                .apiSecret(secret)
                .defaultScope(usedScopes)
                //.httpClientConfig(ApacheHttpClientConfig.defaultConfig())
                .callback(redir)
                .build(MicrosoftAzureActiveDirectory20Api.custom(tenantId));
    }

    String getAuthorizationUrl()
    {
        return service.getAuthorizationUrl();
    }

    Pair<String, Date> createToken(String authCode)
    {
        try {

            OAuth2AccessToken accessToken = service.getAccessToken(authCode);
            logger.debug("Obtained accessToken from MS-AD Services.");
            // TODO: change return type to an object that contains expiry (and is serializable...)
            Date expiry = new Date(System.currentTimeMillis() + 1000L * accessToken.getExpiresIn());
            return new ImmutablePair<>(accessToken.getAccessToken(), expiry);
        } catch (OAuth2AccessTokenErrorResponse e) {
            String msg = "OAuth trouble at creating token:" + e.getErrorDescription();
            logger.warn(msg, e);
            throw new IdentityOAuthException(msg, e);
        } catch (Exception e) {
            String msg = "Generic trouble at creating Token: " + e;
            logger.warn(msg, e);
            throw new IdentityOAuthException(msg, e);
        }
    }

    String readAuthorizationFromReturn(Map<String, String[]> params)
    {
        String errorDescription = "error_description";
        if (params.containsKey(errorDescription)) {
            throw new IdentityOAuthException("An error occurred at AzureAD:"
                    + " " + Arrays.asList(params.get("error"))
                    + " " + Arrays.asList(params.get(errorDescription)));
        }
        String codeP = "code";
        String code = params.containsKey(codeP) ? params.get(codeP)[0] : null;
        logger.debug("Obtained authorization-code from MS-AD Services.");
        return code;
    }

    String performApiRequest(String token, String url) throws Exception
    {
        OAuthRequest request =
                new OAuthRequest(Verb.GET, url);
        service.signRequest(token, request);
        Response response = service.execute(request);
        return response.getBody();
    }

    IdentityOAuthProvider.AbstractIdentityDescription fetchIdentityDetails(String token, String issuerId)
    {
        try {

            OAuthRequest request;
            request = new OAuthRequest(Verb.GET, "https://graph.microsoft.com/v1.0/me");
            service.signRequest(token, request);
            Response response = service.execute(request);
            String responseBody = response.getBody();
            Map json = new ObjectMapper().readValue(responseBody, Map.class);
            AzureADIdentityOAuthProvider.MSADIdentityDescription
                    id = new AzureADIdentityOAuthProvider.MSADIdentityDescription(json, issuerId);

            return id;
        } catch (Exception e) {
            logger.warn("Trouble at fetchIdentityDetails:", e);
            throw new IdentityOAuthException("Trouble at fetchIdentityDetails.", e);
        }
    }

    Triple<InputStream, String, String> fetchUserImage(Date ifModifiedSince,
            IdentityOAuthProvider.AbstractIdentityDescription id, String token, List<String> scopes)
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
                    if (logger.isDebugEnabled()) {
                        logger.debug("Photo response: " + photoResponse.getBody());
                    }
                    return null;
                }
            }
        } catch (Throwable e) {
            logger.warn("Can't save photo.", e);
        }
        return null;
    }
}
