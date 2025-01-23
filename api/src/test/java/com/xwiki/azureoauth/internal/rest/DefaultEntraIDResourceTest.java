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

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWiki wiki;

    @Test
    void xwikiLoginTest() throws XWikiRestException
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        String redirectDocumentURL = "http://localhost:8080/xwiki/bin/view/Sandbox/WebHome";
        when(wiki.getURL("Sandbox.WebHome", "view", "", wikiContext)).thenReturn(redirectDocumentURL);
        String parameters = String.format("xredirect=%s&loginLink=1&oidc.skipped=true", redirectDocumentURL);
        when(wiki.getURL("XWiki.XWikiLogin", "login", parameters, wikiContext)).thenReturn("login_url");
        assertEquals(303, defaultEntraIDResource.xwikiLogin("Sandbox.WebHome").getStatus());
    }

    @Test
    void xwikiLoginTestFail() throws XWikiRestException
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        String redirectDocumentURL = null;
        when(wiki.getURL("Sandbox.WebHome", "view", "", wikiContext)).thenReturn(redirectDocumentURL);
        String parameters = String.format("xredirect=%s&loginLink=1&oidc.skipped=true", redirectDocumentURL);
        when(wiki.getURL("XWiki.XWikiLogin", "login", parameters, wikiContext)).thenReturn(null);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultEntraIDResource.xwikiLogin("Sandbox.WebHome");
        });
        assertEquals("Failed to generate the log in redirect URL. Root cause: [NullPointerException: ]", logCapture.getMessage(0));
        assertEquals(500, exception.getResponse().getStatus());
    }
}
