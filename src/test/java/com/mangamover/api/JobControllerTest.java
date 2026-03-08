package com.mangamover.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangamover.model.Job;
import com.mangamover.service.FileMoverService;
import com.mangamover.service.JobService;
import com.mangamover.service.WatcherService;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JobControllerTest {

    @TempDir
    Path tempDir;

    private JobService jobService;
    private WatcherService watcherService;
    private FileMoverService fileMoverService;
    private ObjectMapper mapper;
    private JobController controller;

    @BeforeEach
    void setUp() {
        jobService = mock(JobService.class);
        watcherService = mock(WatcherService.class);
        fileMoverService = mock(FileMoverService.class);
        mapper = new ObjectMapper();
        controller = new JobController(jobService, watcherService, fileMoverService, mapper);
    }

    private Context mockCtx() {
        Context ctx = mock(Context.class, RETURNS_DEEP_STUBS);
        when(ctx.status(anyInt())).thenReturn(ctx);
        return ctx;
    }

    private Job makeJob(long id, String name, boolean watch, boolean active) {
        Job j = new Job();
        j.id = id;
        j.name = name;
        j.sourcePath = tempDir.resolve("src").toString();
        j.destPath = tempDir.resolve("dst").toString();
        j.watch = watch;
        j.active = active;
        return j;
    }

    private void invoke(String methodName, Context ctx) throws Exception {
        Method m = JobController.class.getDeclaredMethod(methodName, Context.class);
        m.setAccessible(true);
        m.invoke(controller, ctx);
    }

    // ── list ────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllJobs() throws Exception {
        when(jobService.findAll()).thenReturn(List.of(makeJob(1, "J1", true, true)));
        Context ctx = mockCtx();
        invoke("list", ctx);
        verify(ctx).json(any(List.class));
    }

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_validJob_returns201AndStartsWatcherWhenWatchAndActive() throws Exception {
        Job created = makeJob(1L, "J1", true, true);
        when(jobService.create(any())).thenReturn(created);

        Context ctx = mockCtx();
        when(ctx.body()).thenReturn(mapper.writeValueAsString(created));

        invoke("create", ctx);

        verify(jobService).create(any());
        verify(watcherService).start(any());
        verify(ctx).status(201);
    }

    @Test
    void create_watchFalse_doesNotStartWatcher() throws Exception {
        Job created = makeJob(1L, "J1", false, true);
        when(jobService.create(any())).thenReturn(created);

        Context ctx = mockCtx();
        when(ctx.body()).thenReturn(mapper.writeValueAsString(created));

        invoke("create", ctx);

        verify(watcherService, never()).start(any());
    }

    @Test
    void create_activeFalse_doesNotStartWatcher() throws Exception {
        Job created = makeJob(1L, "J1", true, false);
        when(jobService.create(any())).thenReturn(created);

        Context ctx = mockCtx();
        when(ctx.body()).thenReturn(mapper.writeValueAsString(created));

        invoke("create", ctx);

        verify(watcherService, never()).start(any());
    }

    // ── update ──────────────────────────────────────────────────────────────

    @Test
    void update_existingJob_updatesAndRestartWatcher() throws Exception {
        Job existing = makeJob(1L, "Old", true, true);
        Job updated  = makeJob(1L, "New", true, true);
        when(jobService.findById(1L)).thenReturn(existing);
        when(jobService.update(any())).thenReturn(updated);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("1");
        when(ctx.body()).thenReturn(mapper.writeValueAsString(updated));

        invoke("update", ctx);

        verify(jobService).update(any());
        verify(watcherService).stop(1L);
        verify(watcherService).start(any());
        verify(ctx).json(any(Job.class));
    }

    @Test
    void update_nonExistentJob_returns404() throws Exception {
        when(jobService.findById(99L)).thenReturn(null);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("99");

        invoke("update", ctx);

        verify(ctx).status(404);
        verify(jobService, never()).update(any());
    }

    @Test
    void update_watchOrActiveFalse_stopsWatcherWithoutRestarting() throws Exception {
        Job existing = makeJob(1L, "J1", true, true);
        Job updated  = makeJob(1L, "J1", false, true); // watch=false
        when(jobService.findById(1L)).thenReturn(existing);
        when(jobService.update(any())).thenReturn(updated);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("1");
        when(ctx.body()).thenReturn(mapper.writeValueAsString(updated));

        invoke("update", ctx);

        verify(watcherService).stop(1L);
        verify(watcherService, never()).start(any());
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    void delete_existingJob_returns204() throws Exception {
        when(jobService.delete(1L)).thenReturn(true);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("1");

        invoke("delete", ctx);

        verify(watcherService).stop(1L);
        verify(ctx).status(204);
    }

    @Test
    void delete_nonExistentJob_returns404() throws Exception {
        when(jobService.delete(99L)).thenReturn(false);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("99");

        invoke("delete", ctx);

        verify(ctx).status(404);
    }

    // ── run ─────────────────────────────────────────────────────────────────

    @Test
    void run_existingJob_returns202() throws Exception {
        Job job = makeJob(1L, "J1", false, true);
        when(jobService.findById(1L)).thenReturn(job);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("1");

        invoke("run", ctx);

        verify(ctx).status(202);
        verify(ctx.status(202)).json(any(Map.class));
    }

    @Test
    void run_nonExistentJob_returns404() throws Exception {
        when(jobService.findById(99L)).thenReturn(null);

        Context ctx = mockCtx();
        when(ctx.pathParam("id")).thenReturn("99");

        invoke("run", ctx);

        verify(ctx).status(404);
    }

    // ── checkPermissions ────────────────────────────────────────────────────

    @Test
    void checkPermissions_nonExistentPaths_returnsNull() throws Exception {
        Job job = new Job();
        job.sourcePath = tempDir.resolve("nonexistent_src").toString();
        job.destPath   = tempDir.resolve("nonexistent_dst").toString();

        Method m = JobController.class.getDeclaredMethod("checkPermissions", Job.class);
        m.setAccessible(true);
        assertNull(m.invoke(controller, job));
    }

    @Test
    void checkPermissions_existingReadableAndWritablePaths_returnsNull() throws Exception {
        Job job = new Job();
        job.sourcePath = tempDir.toString();
        job.destPath   = tempDir.toString();

        Method m = JobController.class.getDeclaredMethod("checkPermissions", Job.class);
        m.setAccessible(true);
        assertNull(m.invoke(controller, job));
    }

    @Test
    void checkPermissions_unreadableSource_returnsError() throws Exception {
        Path unreadable = tempDir.resolve("unreadable");
        unreadable.toFile().mkdirs();
        boolean changed = unreadable.toFile().setReadable(false);
        org.junit.jupiter.api.Assumptions.assumeTrue(changed && !unreadable.toFile().canRead(),
                "Skipped: unable to remove read permission (may be running as root)");

        Job job = new Job();
        job.sourcePath = unreadable.toString();
        job.destPath   = tempDir.toString();

        Method m = JobController.class.getDeclaredMethod("checkPermissions", Job.class);
        m.setAccessible(true);
        String result = (String) m.invoke(controller, job);
        assertNotNull(result);
        assertTrue(result.contains("leitura"));

        unreadable.toFile().setReadable(true);
    }

    @Test
    void checkPermissions_unwritableDest_returnsError() throws Exception {
        Path unwritable = tempDir.resolve("unwritable");
        unwritable.toFile().mkdirs();
        boolean changed = unwritable.toFile().setWritable(false);
        org.junit.jupiter.api.Assumptions.assumeTrue(changed && !unwritable.toFile().canWrite(),
                "Skipped: unable to remove write permission (may be running as root)");

        Job job = new Job();
        job.sourcePath = tempDir.toString();
        job.destPath   = unwritable.toString();

        Method m = JobController.class.getDeclaredMethod("checkPermissions", Job.class);
        m.setAccessible(true);
        String result = (String) m.invoke(controller, job);
        assertNotNull(result);
        assertTrue(result.contains("escrita"));

        unwritable.toFile().setWritable(true);
    }
}
