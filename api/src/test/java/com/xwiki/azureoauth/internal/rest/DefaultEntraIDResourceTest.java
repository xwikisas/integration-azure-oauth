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
package com.xwiki.azureoauth.internal.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.azureoauth.configuration.EntraIDClientConfiguration;
import com.xwiki.azureoauth.configuration.EntraIDClientConfigurationStore;
import com.xwiki.azureoauth.internal.user.sync.EntraIDUsersSyncJob;
import com.xwiki.azureoauth.user.sync.EntraIDUsersSyncJobRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class DefaultEntraIDResourceTest
{
    @InjectMockComponents
    private DefaultEntraIDResource defaultEntraIDResource;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private EntraIDClientConfigurationStore configurationStore;

    @Mock
    private EntraIDClientConfiguration defaultConfiguration;

    @Mock
    private EntraIDUsersSyncJob job;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWiki wiki;

    @Mock
    private DocumentReference user;

    @BeforeEach
    void setup() throws Exception
    {
        when(configurationStore.getDefaultConfiguration()).thenReturn(defaultConfiguration);
        when(defaultConfiguration.hasSpecificTenant()).thenReturn(true);
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(wikiContext.getRequest()).thenReturn(request);
    }

    @Test
    void xwikiLoginTest() throws XWikiRestException
    {
        String redirectDocumentURL = "http://localhost:8080/xwiki/bin/view/Sandbox/WebHome";
        when(wiki.getURL("Sandbox.WebHome", "view", "", wikiContext)).thenReturn(redirectDocumentURL);
        String parameters = String.format("xredirect=%s&loginLink=1&oidc.skipped=true", redirectDocumentURL);
        when(wiki.getURL("XWiki.XWikiLogin", "login", parameters, wikiContext)).thenReturn("login_url");
        assertEquals(303, defaultEntraIDResource.xwikiLogin("Sandbox.WebHome").getStatus());
    }

    @Test
    void xwikiLoginTestFail()
    {
        String redirectDocumentURL = null;
        when(wiki.getURL("Sandbox.WebHome", "view", "", wikiContext)).thenReturn(redirectDocumentURL);
        String parameters = String.format("xredirect=%s&loginLink=1&oidc.skipped=true", redirectDocumentURL);
        when(wiki.getURL("XWiki.XWikiLogin", "login", parameters, wikiContext)).thenReturn(null);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultEntraIDResource.xwikiLogin("Sandbox.WebHome");
        });
        assertEquals("Failed to generate the log in redirect URL. Root cause: [NullPointerException: Cannot invoke " +
                "\"String.length()\" because \"this.input\" is null]",
            logCapture.getMessage(0));
        assertEquals(500, exception.getResponse().getStatus());
    }

    @Test
    void syncUsersTestNotAdmin() throws AccessDeniedException
    {
        when(request.getParameterMap()).thenReturn(
            Map.of("disable", new String[] { "false" }, "remove", new String[] { "false" }));

        doThrow(new AccessDeniedException(Right.ADMIN, user, null)).when(contextualAuthorizationManager)
            .checkAccess(Right.ADMIN);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultEntraIDResource.syncUsers();
        });

        assertEquals(401, exception.getResponse().getStatus());
        assertEquals("Failed to synchronize users with EntraID due to restricted rights.", logCapture.getMessage(0));
    }

    @Test
    void syncUsersTestJobFound() throws XWikiRestException
    {
        when(request.getParameterMap()).thenReturn(
            Map.of("disable", new String[] { "true" }, "remove", new String[] { "true" }));
        List<String> jobId = List.of("entra", "users", "sync", "true", "true");
        when(jobExecutor.getJob(jobId)).thenReturn(job);
        assertEquals(200, defaultEntraIDResource.syncUsers().getStatus());
        assertEquals("Requested actions: disabled [true]; remove [true]", logCapture.getMessage(0));
    }

    @Test
    void syncUsersTestFail() throws JobException
    {
        when(request.getParameterMap()).thenReturn(
            Map.of("disable", new String[] { "true" }, "remove", new String[] { "true" }));
        List<String> jobId = List.of("entra", "users", "sync", "true", "true");
        when(jobExecutor.getJob(jobId)).thenReturn(null);
        when(jobExecutor.execute(eq(EntraIDUsersSyncJob.JOB_TYPE), any(EntraIDUsersSyncJobRequest.class))).thenThrow(
            new JobException("Job execution error"));
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultEntraIDResource.syncUsers();
        });
        assertEquals(500, exception.getResponse().getStatus());
        assertEquals("Requested actions: disabled [true]; remove [true]", logCapture.getMessage(0));
        assertEquals("Failed to synchronize users with EntraID. Root cause is: [JobException: Job execution error]",
            logCapture.getMessage(1));
    }

    @Test
    void syncUsersForConfiguration() throws Exception
    {
        when(request.getParameterMap()).thenReturn(Map.of("disable", new String[] { "true" }, "remove",
            new String[] { "false" }, "configuration", new String[] { "tenant" }));
        when(configurationStore.getConfiguration("tenant"))
            .thenReturn(Optional.of(mock(EntraIDClientConfiguration.class)));
        List<String> jobId = List.of("entra", "users", "sync", "true", "false", "tenant");
        when(jobExecutor.getJob(jobId)).thenReturn(null);

        assertEquals(201, defaultEntraIDResource.syncUsers().getStatus());
        assertEquals("Requested actions: disabled [true]; remove [false]", logCapture.getMessage(0));

        ArgumentCaptor<EntraIDUsersSyncJobRequest> requestCaptor =
            ArgumentCaptor.forClass(EntraIDUsersSyncJobRequest.class);
        verify(jobExecutor).execute(eq(EntraIDUsersSyncJob.JOB_TYPE), requestCaptor.capture());
        assertEquals(jobId, requestCaptor.getValue().getId());
        assertEquals("tenant", requestCaptor.getValue().getConfigurationName());
        assertTrue(requestCaptor.getValue().shouldDisable());
        assertFalse(requestCaptor.getValue().shouldRemove());
    }

    @Test
    void syncUsersForMissingConfiguration() throws Exception
    {
        when(request.getParameterMap()).thenReturn(Map.of("disable", new String[] { "true" }, "remove",
            new String[] { "false" }, "configuration", new String[] { "missing" }));

        assertEquals(404, defaultEntraIDResource.syncUsers().getStatus());
        assertEquals("Requested actions: disabled [true]; remove [false]", logCapture.getMessage(0));
        assertEquals("No Entra ID client configuration named [missing] to synchronize the users of.",
            logCapture.getMessage(1));
        verify(jobExecutor, never()).execute(any(), any());
    }

    @Test
    void syncUsersRemoveWithoutSpecificTenant() throws Exception
    {
        when(request.getParameterMap()).thenReturn(Map.of("disable", new String[] { "false" }, "remove",
            new String[] { "true" }, "configuration", new String[] { "multiTenant" }));
        EntraIDClientConfiguration configuration = mock(EntraIDClientConfiguration.class);
        when(configuration.getConfigurationName()).thenReturn("multiTenant");
        when(configurationStore.getConfiguration("multiTenant")).thenReturn(Optional.of(configuration));

        assertEquals(400, defaultEntraIDResource.syncUsers().getStatus());
        assertEquals("Requested actions: disabled [false]; remove [true]", logCapture.getMessage(0));
        assertEquals("Cannot remove the users of the Entra ID client configuration [multiTenant] because it doesn't "
            + "target a single tenant.", logCapture.getMessage(1));
        verify(jobExecutor, never()).execute(any(), any());
    }

    @Test
    void syncUsersTest() throws XWikiRestException
    {
        when(request.getParameterMap()).thenReturn(
            Map.of("disable", new String[] { "true" }, "remove", new String[] { "false" }));
        List<String> jobId = List.of("entra", "users", "sync", "true", "false");
        when(jobExecutor.getJob(jobId)).thenReturn(null);
        assertEquals(201, defaultEntraIDResource.syncUsers().getStatus());
        assertEquals("Requested actions: disabled [true]; remove [false]", logCapture.getMessage(0));
    }
}
