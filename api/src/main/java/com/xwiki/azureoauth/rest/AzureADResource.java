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
package com.xwiki.azureoauth.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Provides the APIs needed by the Azure AD server in order to skip the OIDC authenticator.
 *
 * @version $Id$
 * @since 2.0
 */
@Unstable
@Path("/azuread")
public interface AzureADResource extends XWikiRestComponent
{
    /**
     * Redirect the user to the XWiki login page, skipping the OIDC authenticator.
     * @param redirectDocument the page where to be redirected after log in.
     * @return status code 303 SEE OTHER and the log in link for redirecting.
     * @throws XWikiRestException if an error occurred while building the redirect URL.
     */
    @GET
    @Path("/login/xwiki/{redirectDocument}")
    Response xwikiLogin(@PathParam("redirectDocument") String redirectDocument) throws XWikiRestException;
}
