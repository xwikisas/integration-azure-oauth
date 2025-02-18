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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authservice.XWikiAuthServiceComponent;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xwiki.azureoauth.configuration.EntraIDConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraIDObjectUpdateListener}
 *
 * @version $Id$
 */
@ComponentTest
class EntraIDObjectUpdateListenerTest
{
    private static final String BASE_ENDPOINT = "https://login.microsoftonline.com/%s/oauth2/v2.0/%s";

    private static final List<String> SPACE = Arrays.asList("EntraID", "Code");

    /**
     * Entra ID OIDC configuration document.
     */
    private static final LocalDocumentReference CONFIG_DOC =
        new LocalDocumentReference(SPACE, "EntraOIDCClientConfiguration");

    @InjectMockComponents
    private EntraIDObjectUpdateListener objectUpdateListener;

    @MockComponent
    private WikiDescriptorManager wikiManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @MockComponent
    @Named("default")
    private Provider<EntraIDConfiguration> entraIDConfigurationProvider;

    @MockComponent
    private Provider<AzureADOIDCMigrator> azureOIDCMigratorProvider;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Mock
    private XWiki xwiki;

    @MockComponent
    private XWikiAuthServiceComponent defaultAuth;

    @MockComponent
    private XWikiAuthServiceComponent differentAuth;

    @MockComponent
    private Provider<ComponentManager> componentManagerProvider;

    @Mock
    private ComponentManager componentManager;

    @Mock
    private XWikiDocument xWikiDocument;

    @MockComponent
    private EntraIDConfiguration entraIDConfiguration;

    @MockComponent
    private AzureADOIDCMigrator azureADOIDCMigrator;

    private XObjectUpdatedEvent event = new XObjectUpdatedEvent();

    private DocumentReference configReference = new DocumentReference(CONFIG_DOC, new WikiReference("mywiki"));

    private Map<String, Object> configMap =
        Map.of("authorizationEndpoint", String.format(BASE_ENDPOINT, "new_value", "authorize"));

    @BeforeComponent
    void setUp()
    {
        when(entraIDConfigurationProvider.get()).thenReturn(entraIDConfiguration);
        when(azureOIDCMigratorProvider.get()).thenReturn(azureADOIDCMigrator);
    }

    @BeforeEach
    void beforeAll()
    {
        when(xWikiDocument.getDocumentReference()).thenReturn(configReference);
        when(wikiManager.getCurrentWikiId()).thenReturn("mywiki");
        when(entraIDConfiguration.getOIDCTenantID()).thenReturn("old_value");
        when(entraIDConfiguration.getTenantID()).thenReturn("new_value");
        when(azureADOIDCMigrator.getEndpoints("new_value")).thenReturn(configMap);
    }

    @Test
    void onEventSuccess() throws ConfigurationSaveException
    {
        objectUpdateListener.onEvent(event, xWikiDocument, null);

        verify(entraIDConfiguration, Mockito.times(1)).setOIDCConfiguration(anyMap());
    }

    @Test
    void onEventFail() throws ConfigurationSaveException
    {
        doThrow(new ConfigurationSaveException("Mock test exception.")).when(entraIDConfiguration)
            .setOIDCConfiguration(configMap);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            objectUpdateListener.onEvent(event, xWikiDocument, null);
        });
        assertEquals(
            "There was an error while trying to update OIDC endpoints. Root cause is: [ConfigurationSaveException: Mock test exception.]",
            logCapture.getMessage(0));
        assertEquals("org.xwiki.configuration.ConfigurationSaveException: Mock test exception.",
            exception.getMessage());
    }

    @Test
    void initializeDefault() throws InitializationException
    {
        when(azureADOIDCMigrator.getXWiki()).thenReturn(xwiki);
        when(xwikicfg.getProperty("xwiki.authentication.authclass")).thenReturn(null);
        when(defaultAuth.getId()).thenReturn("default");
        when(xwiki.getAuthService()).thenReturn(defaultAuth);
        objectUpdateListener.initialize();
        verify(xwiki, Mockito.times(0)).setAuthService(null);
    }

    @Test
    void initializeDifferentNoDefault() throws InitializationException, ComponentLookupException
    {
        when(azureADOIDCMigrator.getXWiki()).thenReturn(xwiki);
        when(xwikicfg.getProperty("xwiki.authentication.authclass")).thenReturn(null);
        when(differentAuth.getId()).thenReturn("test id");
        when(xwiki.getAuthService()).thenReturn(differentAuth);
        when(componentManagerProvider.get()).thenReturn(componentManager);
        when(componentManager.hasComponent(XWikiAuthServiceComponent.class)).thenReturn(false);

        objectUpdateListener.initialize();
        verify(xwiki, Mockito.times(1)).setAuthService(null);
        verify(componentManager, Mockito.times(0)).getInstanceList(XWikiAuthServiceComponent.class);
    }

    @Test
    void initializeDifferentDefault() throws InitializationException, ComponentLookupException
    {
        when(azureADOIDCMigrator.getXWiki()).thenReturn(xwiki);
        when(xwikicfg.getProperty("xwiki.authentication.authclass")).thenReturn(null);
        when(differentAuth.getId()).thenReturn("test id");
        when(defaultAuth.getId()).thenReturn("default");
        when(xwiki.getAuthService()).thenReturn(differentAuth);
        when(componentManagerProvider.get()).thenReturn(componentManager);
        when(componentManager.hasComponent(XWikiAuthServiceComponent.class)).thenReturn(true);

        when(componentManager.getInstanceList(XWikiAuthServiceComponent.class)).thenReturn(
            Collections.singletonList(defaultAuth));

        objectUpdateListener.initialize();
        verify(xwiki, Mockito.times(1)).setAuthService(null);
        verify(componentManager, Mockito.times(1)).getInstanceList(XWikiAuthServiceComponent.class);
        verify(xwiki, Mockito.times(1)).setAuthService(defaultAuth);
    }
}
