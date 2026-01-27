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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraIDNetworkManager}
 *
 * @version $Id$
 */
@ComponentTest
public class EntraIDNetworkManagerTest
{
    @Mock
    HttpClient httpClient;

    @InjectMockComponents
    private EntraIDNetworkManager entraIDNetworkManager;

    @MockComponent
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @MockComponent
    private EntraIDConfiguration configuration;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpResponse<String> httpResponse2;

    @BeforeEach
    void setup() throws IOException, InterruptedException
    {
        when(configuration.getTokenEndpoint()).thenReturn("http://localhost:8080/some/token/url/");
        when(configuration.getClientID()).thenReturn("client_id");
        when(configuration.getSecret()).thenReturn("secret");
        when(httpClientBuilderFactory.getHttpClient()).thenReturn(httpClient);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
    }

    @Test
    void getEntraUsersJsonMapTestFailToGetAccessToken()
    {
        when(httpResponse.statusCode()).thenReturn(500);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            entraIDNetworkManager.getEntraUsersJson();
        });
        assertEquals("Failed to get token", exception.getMessage());
    }

    @Test
    void getEntraUsersJsonMapTestGraphError() throws IOException, InterruptedException
    {
        when(httpResponse.statusCode()).thenReturn(200);
        String jsonBody = "{ \"access_token\": \"test-token-123\" }";

        when(httpResponse.body()).thenReturn(jsonBody);
        HttpRequest request =
            HttpRequest.newBuilder().uri(URI.create("https://graph.microsoft.com/v1.0/users?$select=id,accountEnabled"))
                .header("Authorization", "Bearer test-token-123").GET().build();

        when(httpClient.send(eq(request), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse2);
        when(httpResponse2.statusCode()).thenReturn(500);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            entraIDNetworkManager.getEntraUsersJson();
        });

        assertEquals("Graph request did not return a valid response.", exception.getMessage());
    }

    @Test
    void getEntraUsersJsonMapTest() throws Exception
    {
        when(httpResponse.statusCode()).thenReturn(200);
        String jsonBody = "{ \"access_token\": \"test-token-123\", \"value\": ["
            + "{ \"id\": \"user1\", \"accountEnabled\": true },{ \"id\": \"user2\", \"accountEnabled\": false },"
            + "{ \"id\": \"user3\", \"accountEnabled\": true }]}";

        when(httpResponse.body()).thenReturn(jsonBody);
        JSONArray jsonArray = entraIDNetworkManager.getEntraUsersJson();
        assertEquals("user1", jsonArray.getJSONObject(0).optString("id"));
        assertFalse(jsonArray.getJSONObject(1).optBoolean("accountEnabled"));
        assertEquals("user3", jsonArray.getJSONObject(2).optString("id"));
    }
}
