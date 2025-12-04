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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.azureoauth.syncJob.EntraSyncJobRequest;
import com.xwiki.azureoauth.syncJob.EntraSyncJobStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraSyncJobTest}
 *
 * @version $Id$
 */
@ComponentTest
class EntraSyncJobTest
{
    @InjectMockComponents
    private EntraSyncJob syncJob;

    @MockComponent
    private EntraSyncManager syncManager;

    @Mock
    private EntraSyncJobRequest request;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Test
    void createNewStatus()
    {
        assertEquals(EntraSyncJobStatus.class, syncJob.createNewStatus(new EntraSyncJobRequest()).getClass());
    }

    @Test
    void runInternalTestError() throws Exception
    {
        when(request.shouldDisable()).thenReturn(true);
        when(request.shouldRemove()).thenReturn(true);
        when(request.getId()).thenReturn(List.of("entra", "users", "sync", "true", "true"));

        syncJob.initialize(request);
        doThrow(new RuntimeException("")).when(syncManager).syncUsers(true, true);
        syncJob.runInternal();
        assertEquals("Started sync job with ID: [[entra, users, sync, true, true]]", logCapture.getMessage(0));
        assertEquals("Error during user sync with Entra ID.", logCapture.getMessage(1));
        assertEquals("Finished sync job with ID: [[entra, users, sync, true, true]]", logCapture.getMessage(2));
    }

    @Test
    void runInternalTest() throws Exception
    {
        when(request.shouldDisable()).thenReturn(true);
        when(request.shouldRemove()).thenReturn(false);
        when(request.getId()).thenReturn(List.of("entra", "users", "sync", "true", "false"));
        syncJob.initialize(request);
        syncJob.runInternal();
        verify(syncManager, times(1)).syncUsers(true, false);
        assertEquals("Started sync job with ID: [[entra, users, sync, true, false]]", logCapture.getMessage(0));
        assertEquals("Finished sync job with ID: [[entra, users, sync, true, false]]", logCapture.getMessage(1));
    }
}
