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
package com.xwiki.azureoauth.internal.user.sync;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;

import com.xwiki.azureoauth.user.sync.EntraIDUsersSyncJobRequest;
import com.xwiki.azureoauth.user.sync.EntraIDUsersSyncJobStatus;

/**
 * Job that handles the user sync between XWiki users and Entra ID users.
 *
 * @version $Id$
 * @since 2.1
 */
@Component
@Named(EntraIDUsersSyncJob.JOB_TYPE)
public class EntraIDUsersSyncJob extends AbstractJob<EntraIDUsersSyncJobRequest, EntraIDUsersSyncJobStatus>
    implements GroupedJob
{
    /**
     * Entra users sync job type.
     */
    public static final String JOB_TYPE = "entra.users.sync";

    @Inject
    private EntraIDUsersSyncManager syncManager;

    @Override
    public JobGroupPath getGroupPath()
    {
        return new JobGroupPath(List.of("entra", "users", "sync"));
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected EntraIDUsersSyncJobStatus createNewStatus(EntraIDUsersSyncJobRequest request)
    {
        return new EntraIDUsersSyncJobStatus(JOB_TYPE, request, observationManager, loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        try {
            if (!status.isCanceled()) {
                logger.debug("Started EntraID user sync job with ID: [{}]", this.status.getJobID());
                this.progressManager.pushLevelProgress(1, this);
                progressManager.startStep(this);
                syncManager.syncUsers(request.shouldDisable(), request.shouldRemove());
                progressManager.endStep(this);
            }
        } catch (Exception e) {
            logger.error("Failed to synchronize EntraID users.", e);
            progressManager.endStep(this);
            throw new RuntimeException(e);
        } finally {
            this.progressManager.popLevelProgress(this);
            logger.debug("Finished EntraID user sync job with ID: [{}]", this.status.getJobID());
        }
    }
}
