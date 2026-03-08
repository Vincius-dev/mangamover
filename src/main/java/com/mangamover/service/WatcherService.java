package com.mangamover.service;

import com.mangamover.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WatcherService {
    private static final Logger log = LoggerFactory.getLogger(WatcherService.class);
    private final FileMoverService fileMoverService;
    private final Map<Long, Thread> watchers = new ConcurrentHashMap<>();

    public WatcherService(FileMoverService fileMoverService) {
        this.fileMoverService = fileMoverService;
    }

    public void start(Job job) {
        stop(job.id);
        Thread t = Thread.ofVirtual().name("watcher-" + job.id).start(() -> watch(job));
        watchers.put(job.id, t);
        log.info("[Job {}] Watcher started: {}", job.id, job.sourcePath);
    }

    public void stop(long jobId) {
        Thread t = watchers.remove(jobId);
        if (t != null) {
            t.interrupt();
            log.info("[Job {}] Watcher stopped", jobId);
        }
    }

    public void stopAll() {
        watchers.keySet().forEach(this::stop);
    }

    public int activeCount() {
        return watchers.size();
    }

    public boolean isActive(long jobId) {
        return watchers.containsKey(jobId);
    }

    private void watch(Job job) {
        Path source = Path.of(job.sourcePath);
        try {
            Files.createDirectories(source);
        } catch (IOException e) {
            log.error("[Job {}] Cannot create source dir: {}", job.id, e.getMessage());
            return;
        }
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            source.register(ws, StandardWatchEventKinds.ENTRY_CREATE);
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = ws.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    @SuppressWarnings("unchecked")
                    Path filename = ((WatchEvent<Path>) event).context();
                    Path file = source.resolve(filename);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (Files.isRegularFile(file)) {
                        fileMoverService.moveFile(job, file, Path.of(job.destPath));
                    }
                }
                if (!key.reset()) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("[Job {}] Watcher error: {}", job.id, e.getMessage());
        }
        log.info("[Job {}] Watcher exited", job.id);
    }
}
