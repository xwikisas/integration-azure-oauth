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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Provides the API needed by the server in order to skip the OIDC authenticator.
 *
 * @version $Id$
 * @since 2.0
 */
@Unstable
@Path("/entraid")
public interface EntraIDResource extends XWikiRestComponent
{
    /**
     * Redirect the user to the XWiki login page, skipping the OIDC authenticator.
     *
     * @param redirectDocument the page where to be redirected after log in.
     * @return status code 303 SEE OTHER and the login link for redirecting.
     * @throws XWikiRestException if an error occurred while building the redirect URL.
     */
    @GET
    @Path("/login/xwiki/{redirectDocument}")
    Response xwikiLogin(@PathParam("redirectDocument") String redirectDocument) throws XWikiRestException;

    /**
     * Sync XWiki users with the users from Entra ID. The request parameters {@code disable} and {@code remove} select
     * the actions to perform. The optional {@code configuration} parameter is the name of the Entra ID client
     * configuration whose users are synced; when missing, the default configuration is used. Since 2.2.5, the job ID
     * ends with the configuration name when one is given: {@code entra/users/sync/<disable>/<remove>/<configuration>}.
     * Removing users is refused for a configuration that doesn't target a single tenant (blank tenant or multi-tenant
     * alias), since its users can't be told apart from the users of other tenants.
     *
     * @return status code 201 if a new job has been created, status code 200 if a job with the same ID already
     *     exists, status code 400 if users should be removed and the configuration doesn't target a single tenant, or
     *     status code 404 if there is no Entra ID client configuration with the given name
     * @throws XWikiRestException with status code 401 if the user requesting is missing admin rights, or code 500
     *     if any error occurs
     * @since 2.1
     */
    @POST
    @Path("/user/sync")
    @Unstable
    Response syncUsers() throws XWikiRestException;
}
