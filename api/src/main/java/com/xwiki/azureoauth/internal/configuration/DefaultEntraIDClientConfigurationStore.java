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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.oidc.auth.store.OIDCClientConfiguration;
import org.xwiki.contrib.oidc.auth.store.OIDCClientConfigurationStore;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.azureoauth.configuration.EntraIDClientConfiguration;
import com.xwiki.azureoauth.configuration.EntraIDClientConfigurationStore;

import static com.xwiki.azureoauth.internal.configuration.EntraIDConfigurationSource.CONFIG_CLASS;
import static com.xwiki.azureoauth.internal.configuration.EntraIDConfigurationSource.CONFIG_DOC;

/**
 * Default implementation of {@link EntraIDClientConfigurationStore}.
 *
 * @version $Id$
 * @since 2.2.5
 */
@Component
@Singleton
public class DefaultEntraIDClientConfigurationStore implements EntraIDClientConfigurationStore
{
    private static final String CONFIGURATIONS_QUERY = "select distinct oidc.configurationName from Document doc, "
        + "doc.object(XWiki.OIDC.ClientConfigurationClass) as oidc, "
        + "doc.object(EntraID.Code.EntraIDConfigurationClass) as entra";

    @Inject
    private OIDCClientConfigurationStore oidcConfigurationStore;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public EntraIDClientConfiguration getDefaultConfiguration() throws Exception
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference configReference = new DocumentReference(CONFIG_DOC, context.getWikiReference());
        return createConfiguration(context.getWiki().getDocument(configReference, context));
    }

    @Override
    public EntraIDClientConfiguration getConfiguration(String configurationName) throws Exception
    {
        if (StringUtils.isBlank(configurationName)) {
            return null;
        }
        XWikiDocument document = this.oidcConfigurationStore.getOIDCClientConfigurationDocument(configurationName);
        if (document == null || document.getXObject(CONFIG_CLASS) == null) {
            return null;
        }
        return createConfiguration(document);
    }

    @Override
    public List<EntraIDClientConfiguration> getConfigurations() throws Exception
    {
        List<Object> names = this.queryManager.createQuery(CONFIGURATIONS_QUERY, Query.XWQL)
            .setWiki(this.wikiManager.getCurrentWikiId()).execute();
        List<EntraIDClientConfiguration> configurations = new ArrayList<>();
        for (Object name : names) {
            String configurationName = Objects.toString(name, null);
            // A configuration that can't be loaded must not prevent the others from being used.
            try {
                EntraIDClientConfiguration configuration = getConfiguration(configurationName);
                if (configuration != null) {
                    configurations.add(configuration);
                }
            } catch (Exception e) {
                this.logger.warn("Failed to load the Entra ID client configuration [{}].", configurationName, e);
            }
        }
        return configurations;
    }

    private EntraIDClientConfiguration createConfiguration(XWikiDocument document)
    {
        return new EntraIDClientConfiguration(document.getDocumentReference(),
            document.getXObject(OIDCClientConfiguration.CLASS_REFERENCE), document.getXObject(CONFIG_CLASS));
    }
}
