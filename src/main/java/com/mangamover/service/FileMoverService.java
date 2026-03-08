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
    private final LogStore logStore;

    public FileMoverService(HistoryService historyService, LogStore logStore) {
        this.historyService = historyService;
        this.logStore = logStore;
    }

    public void moveAll(Job job) {
        Path source = Path.of(job.sourcePath);
        Path dest = Path.of(job.destPath);
        if (!Files.isDirectory(source)) {
            String msg = "[Job " + job.id + "] Pasta de origem não encontrada: " + source;
            log.warn(msg);
            logStore.warn("mover", msg);
            return;
        }
        try {
            Files.createDirectories(dest);
            try (Stream<Path> stream = Files.list(source)) {
                List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
                logStore.info("mover", "[Job " + job.id + "] Iniciando movimentação de " + files.size() + " arquivo(s)");
                for (Path file : files) {
                    moveFile(job, file, dest);
                }
            }
        } catch (IOException e) {
            String msg = "[Job " + job.id + "] Erro ao listar origem: " + e.getMessage();
            log.error(msg);
            logStore.error("mover", msg);
        }
    }

    public void moveFile(Job job, Path file, Path dest) {
        String filename = file.getFileName().toString();
        try {
            Files.createDirectories(dest);
        } catch (IOException e) {
            String msg = "[Job " + job.id + "] Não foi possível criar pasta de destino: " + e.getMessage();
            log.error(msg);
            logStore.error("mover", msg);
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
            String msg = "[Job " + job.id + "] Movido: " + filename + " → " + target.getFileName();
            log.info(msg);
            logStore.info("mover", msg);
            historyService.record(job.id, filename, "OK", null);
        } catch (Exception e) {
            String msg = "[Job " + job.id + "] Falha ao mover " + filename + ": " + e.getMessage();
            log.error(msg);
            logStore.error("mover", msg);
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
