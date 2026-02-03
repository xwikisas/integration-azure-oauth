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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.azureoauth.user.sync.EntraIDUsersSyncJobRequest;
import com.xwiki.azureoauth.user.sync.EntraIDUsersSyncJobStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link EntraIDUsersSyncJobTest}
 *
 * @version $Id$
 */
@ComponentTest
class EntraIDUsersSyncJobTest
{
    @InjectMockComponents
    private EntraIDUsersSyncJob syncJob;

    @MockComponent
    private EntraIDUsersSyncManager syncManager;

    @Mock
    private EntraIDUsersSyncJobRequest request;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Test
    void createNewStatus()
    {
        assertEquals(EntraIDUsersSyncJobStatus.class, syncJob.createNewStatus(new EntraIDUsersSyncJobRequest()).getClass());
    }

    @Test
    void runInternalTestError() throws Exception
    {
        when(request.shouldDisable()).thenReturn(true);
        when(request.shouldRemove()).thenReturn(true);
        when(request.getId()).thenReturn(List.of("entra", "users", "sync", "true", "true"));

        syncJob.initialize(request);
        doThrow(new RuntimeException("Some error")).when(syncManager).syncUsers(true, true);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            syncJob.runInternal();
        });
        assertEquals("java.lang.RuntimeException: Some error", exception.getMessage());
        assertEquals("Started EntraID user sync job with ID: [[entra, users, sync, true, true]]", logCapture.getMessage(0));
        assertEquals("Failed to synchronize EntraID users.", logCapture.getMessage(1));
        assertEquals("Finished EntraID user sync job with ID: [[entra, users, sync, true, true]]", logCapture.getMessage(2));
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
        assertEquals("Started EntraID user sync job with ID: [[entra, users, sync, true, false]]", logCapture.getMessage(0));
        assertEquals("Finished EntraID user sync job with ID: [[entra, users, sync, true, false]]", logCapture.getMessage(1));
    }
}
