package com.mangamover.service;

import com.mangamover.model.Job;
import com.mangamover.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WatcherServiceTest {

    private FileMoverService fileMoverService;
    private LogStore logStore;
    private WatcherService watcherService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileMoverService = Mockito.mock(FileMoverService.class);
        logStore = new LogStore();
        watcherService = new WatcherService(fileMoverService, logStore);
    }

    private Job createJob(long id, Path source, Path dest) {
        Job j = new Job();
        j.id = id;
        j.name = "Job " + id;
        j.sourcePath = source.toString();
        j.destPath = dest.toString();
        j.watch = true;
        j.active = true;
        return j;
    }

    @Test
    void start_addsJobToActiveWatchers() throws Exception {
        Job job = createJob(1L, tempDir.resolve("src"), tempDir.resolve("dst"));
        watcherService.start(job);
        try {
            assertTrue(watcherService.isActive(1L));
            assertEquals(1, watcherService.activeCount());
        } finally {
            watcherService.stop(1L);
        }
    }

    @Test
    void stop_removesJobFromActiveWatchers() throws Exception {
        Job job = createJob(1L, tempDir.resolve("src"), tempDir.resolve("dst"));
        watcherService.start(job);
        watcherService.stop(1L);
        Thread.sleep(100);

        assertFalse(watcherService.isActive(1L));
        assertEquals(0, watcherService.activeCount());
    }

    @Test
    void stop_nonExistentJob_doesNotThrow() {
        assertDoesNotThrow(() -> watcherService.stop(9999L));
    }

    @Test
    void stop_nonExistentJob_doesNotLog() {
        watcherService.stop(9999L);
        assertTrue(logStore.get(null, "watcher", 10).isEmpty());
    }

    @Test
    void stopAll_stopsAllActiveWatchers() throws Exception {
        Job j1 = createJob(1L, tempDir.resolve("s1"), tempDir.resolve("d1"));
        Job j2 = createJob(2L, tempDir.resolve("s2"), tempDir.resolve("d2"));
        watcherService.start(j1);
        watcherService.start(j2);
        assertEquals(2, watcherService.activeCount());

        watcherService.stopAll();
        Thread.sleep(100);

        assertEquals(0, watcherService.activeCount());
    }

    @Test
    void isActive_returnsFalseForNeverStartedJob() {
        assertFalse(watcherService.isActive(42L));
    }

    @Test
    void activeCount_zeroWhenNoWatchersRunning() {
        assertEquals(0, watcherService.activeCount());
    }

    @Test
    void start_logsStartedMessage() {
        Job job = createJob(1L, tempDir.resolve("src"), tempDir.resolve("dst"));
        watcherService.start(job);
        try {
            List<LogEntry> logs = logStore.get("INFO", "watcher", 10);
            assertFalse(logs.isEmpty());
            assertTrue(logs.stream().anyMatch(e -> e.message.contains("Watcher iniciado")));
        } finally {
            watcherService.stop(1L);
        }
    }

    @Test
    void stop_logsStoppedMessage() throws Exception {
        Job job = createJob(1L, tempDir.resolve("src"), tempDir.resolve("dst"));
        watcherService.start(job);
        watcherService.stop(1L);

        List<LogEntry> logs = logStore.get("INFO", "watcher", 10);
        assertTrue(logs.stream().anyMatch(e -> e.message.contains("Watcher parado")));
    }

    @Test
    void start_replacesExistingWatcher() throws Exception {
        Job job = createJob(1L, tempDir.resolve("src"), tempDir.resolve("dst"));
        watcherService.start(job);
        watcherService.start(job); // reinicia
        try {
            assertEquals(1, watcherService.activeCount());
        } finally {
            watcherService.stop(1L);
        }
    }

    @Test
    void watchThread_startsAndExitsCleanlyOnInterrupt() throws Exception {
        Path source = tempDir.resolve("watched");
        Files.createDirectories(source);
        Job job = createJob(10L, source, tempDir.resolve("dst"));

        watcherService.start(job);
        Thread.sleep(150); // aguarda thread chegar no ws.take()
        watcherService.stop(10L);
        Thread.sleep(200); // aguarda thread encerrar

        assertFalse(watcherService.isActive(10L));
        List<LogEntry> logs = logStore.get(null, "watcher", 20);
        assertTrue(logs.stream().anyMatch(e -> e.message.contains("Watcher parado")));
    }

    @Test
    void watchThread_processesFileEvent_callsMoveFile() throws Exception {
        Path source = tempDir.resolve("watched_src");
        Path dest   = tempDir.resolve("watched_dst");
        Files.createDirectories(source);

        Job job = createJob(20L, source, dest);
        watcherService.start(job);
        Thread.sleep(150); // aguarda watcher registrar no WatchService

        // dropa um arquivo na pasta observada
        Files.writeString(source.resolve("manga.cbz"), "conteudo");
        Thread.sleep(1500); // aguarda o sleep(1000) interno + processamento

        verify(fileMoverService).moveFile(any(Job.class), any(Path.class), eq(dest));
        watcherService.stop(20L);
    }

    @Test
    void watchThread_interruptedDuringFileSleep_exitsGracefully() throws Exception {
        Path source = tempDir.resolve("watched_int");
        Files.createDirectories(source);

        Job job = createJob(30L, source, tempDir.resolve("dst_int"));
        watcherService.start(job);
        Thread.sleep(150); // aguarda watcher no ws.take()

        // dropa arquivo para iniciar o sleep(1000) interno
        Files.writeString(source.resolve("manga.cbz"), "conteudo");
        Thread.sleep(200); // thread está no sleep(1000) agora

        // interrompe durante o sleep — deve sair via InterruptedException
        watcherService.stop(30L);
        Thread.sleep(200);

        assertFalse(watcherService.isActive(30L));
    }

    @Test
    void watchThread_nonRegularFileEvent_doesNotCallMoveFile() throws Exception {
        Path source = tempDir.resolve("watched_dir");
        Files.createDirectories(source);

        Job job = createJob(40L, source, tempDir.resolve("dst_dir"));
        watcherService.start(job);
        Thread.sleep(150);

        // cria subdiretório — não é arquivo regular, não deve chamar moveFile
        Files.createDirectories(source.resolve("subdir"));
        Thread.sleep(1500);

        verify(fileMoverService, never()).moveFile(any(), any(), any());
        watcherService.stop(40L);
    }
}
