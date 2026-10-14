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
package com.xwiki.azureoauth.internal.configuration;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.oidc.auth.store.OIDCClientConfiguration;
import org.xwiki.contrib.oidc.auth.store.OIDCClientConfigurationStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.azureoauth.configuration.EntraIDClientConfiguration;

import static com.xwiki.azureoauth.internal.configuration.EntraIDConfigurationSource.CONFIG_CLASS;
import static com.xwiki.azureoauth.internal.configuration.EntraIDConfigurationSource.CONFIG_DOC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultEntraIDClientConfigurationStore}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultEntraIDClientConfigurationStoreTest
{
    @InjectMockComponents
    private DefaultEntraIDClientConfigurationStore store;

    @MockComponent
    private OIDCClientConfigurationStore oidcConfigurationStore;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws QueryException
    {
        when(contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(wiki);
        when(context.getWikiReference()).thenReturn(new WikiReference("wiki"));
        when(wikiManager.getCurrentWikiId()).thenReturn("wiki");
        when(queryManager.createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);
        when(query.setWiki("wiki")).thenReturn(query);
    }

    @Test
    void getDefaultConfiguration() throws Exception
    {
        DocumentReference configReference = new DocumentReference(CONFIG_DOC, new WikiReference("wiki"));
        XWikiDocument configDoc = createConfigurationDocument(configReference, "EntraID", "defaultTenant");
        when(wiki.getDocument(configReference, context)).thenReturn(configDoc);

        EntraIDClientConfiguration configuration = store.getDefaultConfiguration();

        assertEquals(configReference, configuration.getDocumentReference());
        assertEquals("defaultTenant", configuration.getTenantID());
    }

    @Test
    void getConfiguration() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "OIDCPro", "Tenant");
        XWikiDocument document = createConfigurationDocument(reference, "tenant", "tenantID");
        when(oidcConfigurationStore.getOIDCClientConfigurationDocument("tenant")).thenReturn(document);

        EntraIDClientConfiguration configuration = store.getConfiguration("tenant");

        assertEquals(reference, configuration.getDocumentReference());
        assertEquals("tenant", configuration.getConfigurationName());
        assertEquals("tenantID", configuration.getTenantID());
    }

    @Test
    void getConfigurationWithoutEntraIDObject() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(oidcConfigurationStore.getOIDCClientConfigurationDocument("other")).thenReturn(document);

        assertNull(store.getConfiguration("other"));
        assertNull(store.getConfiguration("missing"));
        assertNull(store.getConfiguration(""));
    }

    @Test
    void getConfigurations() throws Exception
    {
        DocumentReference reference = new DocumentReference("wiki", "OIDCPro", "Tenant");
        XWikiDocument document = createConfigurationDocument(reference, "tenant", "tenantID");
        when(oidcConfigurationStore.getOIDCClientConfigurationDocument("tenant")).thenReturn(document);
        when(oidcConfigurationStore.getOIDCClientConfigurationDocument("broken"))
            .thenThrow(new RuntimeException("broken configuration"));
        when(query.execute()).thenReturn(Arrays.asList("tenant", "untrusted", "broken", null));

        List<EntraIDClientConfiguration> configurations = store.getConfigurations();

        assertEquals(1, configurations.size());
        assertEquals("tenant", configurations.get(0).getConfigurationName());
        assertEquals("Failed to load the Entra ID client configuration [broken].", logCapture.getMessage(0));
    }

    private XWikiDocument createConfigurationDocument(DocumentReference reference, String name, String tenantID)
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(reference);
        BaseObject oidcObject = new BaseObject();
        oidcObject.setStringValue("configurationName", name);
        BaseObject entraIDObject = new BaseObject();
        entraIDObject.setStringValue("tenantId", tenantID);
        when(document.getXObject(OIDCClientConfiguration.CLASS_REFERENCE)).thenReturn(oidcObject);
        when(document.getXObject(CONFIG_CLASS)).thenReturn(entraIDObject);
        return document;
    }
}
