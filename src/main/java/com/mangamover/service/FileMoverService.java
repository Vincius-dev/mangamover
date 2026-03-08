package com.mangamover.service;

import com.mangamover.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileMoverService {
    private static final Logger log = LoggerFactory.getLogger(FileMoverService.class);
    private final HistoryService historyService;

    public FileMoverService(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void moveAll(Job job) {
        Path source = Path.of(job.sourcePath);
        Path dest = Path.of(job.destPath);
        if (!Files.isDirectory(source)) {
            log.warn("[Job {}] Source path not found: {}", job.id, source);
            return;
        }
        try {
            Files.createDirectories(dest);
            try (Stream<Path> stream = Files.list(source)) {
                List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
                for (Path file : files) {
                    moveFile(job, file, dest);
                }
            }
        } catch (IOException e) {
            log.error("[Job {}] Error listing source: {}", job.id, e.getMessage());
        }
    }

    public void moveFile(Job job, Path file, Path dest) {
        String filename = file.getFileName().toString();
        try {
            Files.createDirectories(dest);
        } catch (IOException e) {
            log.error("[Job {}] Cannot create dest dir: {}", job.id, e.getMessage());
            return;
        }
        Path target = resolveConflict(dest, filename);
        try {
            try {
                Files.move(file, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(file);
            }
            log.info("[Job {}] Moved: {} -> {}", job.id, filename, target.getFileName());
            historyService.record(job.id, filename, "OK", null);
        } catch (Exception e) {
            log.error("[Job {}] Failed to move {}: {}", job.id, filename, e.getMessage());
            try {
                historyService.record(job.id, filename, "ERROR", e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    private Path resolveConflict(Path dest, String filename) {
        Path target = dest.resolve(filename);
        if (!Files.exists(target)) return target;
        String base = filename;
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            base = filename.substring(0, dot);
            ext = filename.substring(dot);
        }
        int i = 1;
        while (Files.exists(target)) {
            target = dest.resolve(base + "_" + i + ext);
            i++;
        }
        return target;
    }
}
