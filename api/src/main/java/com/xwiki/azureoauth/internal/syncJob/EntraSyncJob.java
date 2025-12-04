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
package com.xwiki.azureoauth.internal.syncJob;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;

import com.xwiki.azureoauth.syncJob.EntraSyncJobRequest;
import com.xwiki.azureoauth.syncJob.EntraSyncJobStatus;

/**
 * Job that handles the user sync between XWiki users and Entra ID users.
 *
 * @version $Id$
 * @since 2.1
 */
@Component
@Named(EntraSyncJob.JOB_TYPE)
public class EntraSyncJob extends AbstractJob<EntraSyncJobRequest, EntraSyncJobStatus> implements GroupedJob
{
    /**
     * Entra users sync job type.
     */
    public static final String JOB_TYPE = "entra.users.sync";

    @Inject
    private EntraSyncManager syncManager;

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
    protected EntraSyncJobStatus createNewStatus(EntraSyncJobRequest request)
    {
        return new EntraSyncJobStatus(JOB_TYPE, request, observationManager, loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        try {
            if (!status.isCanceled()) {
                logger.debug("Started sync job with ID: [{}]", this.status.getJobID());
                this.progressManager.pushLevelProgress(1, this);
                progressManager.startStep(this);
                syncManager.syncUsers(request.shouldDisable(), request.shouldRemove());
                progressManager.endStep(this);
            }
        } catch (Exception e) {
            logger.error("Error during user sync with Entra ID.", e);
            progressManager.endStep(this);
            throw new RuntimeException(e);
        } finally {
            this.progressManager.popLevelProgress(this);
            logger.debug("Finished sync job with ID: [{}]", this.status.getJobID());
        }
    }
}
