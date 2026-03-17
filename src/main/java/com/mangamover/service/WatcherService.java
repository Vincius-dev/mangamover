package com.mangamover.service;

import com.mangamover.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class WatcherService {
    private static final Logger log = LoggerFactory.getLogger(WatcherService.class);
    private static final Pattern CBZ_CBR = Pattern.compile("(?i).*\\.cb[zr]$");
    private final FileMoverService fileMoverService;
    private final LogStore logStore;
    private final Map<Long, Thread> watchers = new ConcurrentHashMap<>();

    public WatcherService(FileMoverService fileMoverService, LogStore logStore) {
        this.fileMoverService = fileMoverService;
        this.logStore = logStore;
    }

    public void start(Job job) {
        stop(job.id);
        Thread t = Thread.ofVirtual().name("watcher-" + job.id).start(() -> {
            if (job.recursive) watchRecursive(job);
            else watch(job);
        });
        watchers.put(job.id, t);
        String msg = "[Job " + job.id + "] Watcher iniciado: " + job.sourcePath;
        log.info(msg);
        logStore.info("watcher", msg);
    }

    public void stop(long jobId) {
        Thread t = watchers.remove(jobId);
        if (t != null) {
            t.interrupt();
            String msg = "[Job " + jobId + "] Watcher parado";
            log.info(msg);
            logStore.info("watcher", msg);
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
            String msg = "[Job " + job.id + "] Não foi possível criar pasta de origem: " + e.getMessage();
            log.error(msg);
            logStore.error("watcher", msg);
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
                        logStore.info("watcher", "[Job " + job.id + "] Arquivo detectado: " + filename);
                        fileMoverService.moveFile(job, file, Path.of(job.destPath));
                    }
                }
                if (!key.reset()) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            String msg = "[Job " + job.id + "] Erro no watcher: " + e.getMessage();
            log.error(msg);
            logStore.error("watcher", msg);
        }
        log.info("[Job {}] Watcher encerrado", job.id);
    }

    private void watchRecursive(Job job) {
        Path source = Path.of(job.sourcePath);
        Path dest = Path.of(job.destPath);
        try {
            Files.createDirectories(source);
        } catch (IOException e) {
            String msg = "[Job " + job.id + "] Não foi possível criar pasta de origem: " + e.getMessage();
            log.error(msg);
            logStore.error("watcher", msg);
            return;
        }
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            Map<WatchKey, Path> keyDirMap = new ConcurrentHashMap<>();
            registerTree(ws, source, keyDirMap);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = ws.take();
                Path dir = keyDirMap.get(key);
                if (dir == null) { key.reset(); continue; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                    @SuppressWarnings("unchecked")
                    Path name = ((WatchEvent<Path>) event).context();
                    Path child = dir.resolve(name);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if (Files.isDirectory(child)) {
                        registerTree(ws, child, keyDirMap);
                    } else if (Files.isRegularFile(child) && CBZ_CBR.matcher(child.getFileName().toString()).matches()) {
                        String seriesName = child.getParent().getFileName().toString();
                        logStore.info("watcher", "[Job " + job.id + "] Arquivo detectado (recursivo): " + seriesName + "/" + child.getFileName());
                        fileMoverService.moveFileRecursive(job, child, dest, seriesName);
                    }
                }
                if (!key.reset()) {
                    keyDirMap.remove(key);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            String msg = "[Job " + job.id + "] Erro no watcher recursivo: " + e.getMessage();
            log.error(msg);
            logStore.error("watcher", msg);
        }
        log.info("[Job {}] Watcher recursivo encerrado", job.id);
    }

    private void registerTree(WatchService ws, Path root, Map<WatchKey, Path> keyDirMap) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(ws, StandardWatchEventKinds.ENTRY_CREATE);
                keyDirMap.put(key, dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
