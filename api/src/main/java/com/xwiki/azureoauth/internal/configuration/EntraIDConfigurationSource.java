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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractWikisConfigurationSource;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Configuration source for the Entra ID configuration class.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named(EntraIDConfigurationSource.HINT)
@Singleton
@Unstable
public class EntraIDConfigurationSource extends AbstractWikisConfigurationSource
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "entraid.configuration";

    private static final List<String> SPACE = Arrays.asList("EntraID", "Code");

    /**
     * Entra ID OIDC configuration document.
     */
    public static final LocalDocumentReference CONFIG_DOC =
        new LocalDocumentReference(SPACE, "EntraOIDCClientConfiguration");

    private static final LocalDocumentReference CONFIG_CLASS =
        new LocalDocumentReference(SPACE, "EntraIDConfigurationClass");

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CONFIG_CLASS;
    }

    @Override
    protected LocalDocumentReference getLocalDocumentReference()
    {
        return CONFIG_DOC;
    }

    @Override
    protected String getCacheId()
    {
        return HINT;
    }
}
