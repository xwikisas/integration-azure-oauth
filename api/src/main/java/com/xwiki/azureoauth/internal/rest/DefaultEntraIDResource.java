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

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.azureoauth.internal.user.sync.EntraIDUsersSyncJob;
import com.xwiki.azureoauth.rest.EntraIDResource;
import com.xwiki.azureoauth.user.sync.EntraIDUsersSyncJobRequest;

/**
 * Default implementation of {@link EntraIDResource}.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named("com.xwiki.azureoauth.internal.rest.DefaultEntraIDResource")
@Singleton
public class DefaultEntraIDResource implements EntraIDResource
{
    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Override
    public Response xwikiLogin(String redirectDocument) throws XWikiRestException
    {
        try {
            XWikiContext xwikiContext = wikiContextProvider.get();
            XWiki xwiki = xwikiContext.getWiki();
            EscapeTool escapeTool = new EscapeTool();

            String redirectDocumentURL = xwiki.getURL(escapeTool.xml(redirectDocument), "view", "", xwikiContext);
            String parameters = String.format("xredirect=%s&loginLink=1&oidc.skipped=true", redirectDocumentURL);
            String loginURL = xwiki.getURL("XWiki.XWikiLogin", "login", parameters, xwikiContext);

            return Response.seeOther(new URI(loginURL)).build();
        } catch (Exception e) {
            logger.warn("Failed to generate the log in redirect URL. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response syncUsers(String disable, String remove) throws XWikiRestException
    {
        try {
            contextualAuthorizationManager.checkAccess(Right.ADMIN);
            logger.debug("Received actions: disabled [{}]; remove [{}]", disable, remove);
            List<String> jobId = List.of("entra", "users", "sync", disable, remove);
            Job job = this.jobExecutor.getJob(jobId);
            if (job == null) {
                EntraIDUsersSyncJobRequest entraIdUsersSyncJobRequest =
                    new EntraIDUsersSyncJobRequest(jobId, Boolean.parseBoolean(disable), Boolean.parseBoolean(remove));
                this.jobExecutor.execute(EntraIDUsersSyncJob.JOB_TYPE, entraIdUsersSyncJobRequest);
                return Response.status(201).build();
            } else {
                return Response.status(200).build();
            }
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to synchronize users with EntraID due to restricted rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to synchronize users with EntraID. Root cause is: [%s]",
                ExceptionUtils.getRootCauseMessage(e));
            logger.warn(errorMessage, e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
