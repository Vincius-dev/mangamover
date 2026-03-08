package com.mangamover.service;

import com.mangamover.model.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileMoverServiceTest {

    @TempDir
    Path tempDir;

    private HistoryService historyService;
    private LogStore logStore;
    private FileMoverService fileMoverService;

    @BeforeEach
    void setUp() {
        historyService = Mockito.mock(HistoryService.class);
        logStore = new LogStore();
        fileMoverService = new FileMoverService(historyService, logStore);
    }

    private Job createJob(Path source, Path dest) {
        Job job = new Job();
        job.id = 1;
        job.name = "Test Job";
        job.sourcePath = source.toString();
        job.destPath = dest.toString();
        return job;
    }

    @Test
    void moveAll_movesAllFilesFromSourceToDest() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);

        Files.writeString(source.resolve("manga1.cbz"), "conteudo1");
        Files.writeString(source.resolve("manga2.cbz"), "conteudo2");

        Job job = createJob(source, dest);
        fileMoverService.moveAll(job);

        assertFalse(Files.exists(source.resolve("manga1.cbz")));
        assertFalse(Files.exists(source.resolve("manga2.cbz")));
        assertTrue(Files.exists(dest.resolve("manga1.cbz")));
        assertTrue(Files.exists(dest.resolve("manga2.cbz")));
    }

    @Test
    void moveAll_recordsHistoryForEachFile() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);

        Files.writeString(source.resolve("a.cbz"), "a");
        Files.writeString(source.resolve("b.cbz"), "b");

        Job job = createJob(source, dest);
        fileMoverService.moveAll(job);

        verify(historyService, times(2)).record(eq(1L), anyString(), eq("OK"), isNull());
    }

    @Test
    void moveAll_sourceNotFound_logsWarnAndDoesNotThrow() {
        Path source = tempDir.resolve("nao_existe");
        Path dest = tempDir.resolve("dest");

        Job job = createJob(source, dest);
        assertDoesNotThrow(() -> fileMoverService.moveAll(job));

        List<com.mangamover.model.LogEntry> warns = logStore.get("WARN", null, 10);
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).message.contains("Pasta de origem não encontrada"));
    }

    @Test
    void moveAll_emptySource_movesZeroFiles() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);

        Job job = createJob(source, dest);
        fileMoverService.moveAll(job);

        verify(historyService, never()).record(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void moveAll_doesNotMoveSubdirectories() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(source.resolve("subdir"));
        Files.writeString(source.resolve("arquivo.cbz"), "conteudo");

        Job job = createJob(source, dest);
        fileMoverService.moveAll(job);

        // apenas o arquivo é movido, o subdir permanece
        assertTrue(Files.exists(source.resolve("subdir")));
        assertTrue(Files.exists(dest.resolve("arquivo.cbz")));
        verify(historyService, times(1)).record(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void moveFile_conflictResolution_renamesWithSuffix() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);

        // arquivo já existe no destino
        Files.writeString(dest.resolve("manga.cbz"), "existente");
        Files.writeString(source.resolve("manga.cbz"), "novo");

        Job job = createJob(source, dest);
        fileMoverService.moveFile(job, source.resolve("manga.cbz"), dest);

        assertTrue(Files.exists(dest.resolve("manga.cbz")));   // original preservado
        assertTrue(Files.exists(dest.resolve("manga_1.cbz"))); // novo renomeado
    }

    @Test
    void moveFile_conflictResolution_multipleConflicts() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);

        Files.writeString(dest.resolve("manga.cbz"), "original");
        Files.writeString(dest.resolve("manga_1.cbz"), "copia1");
        Files.writeString(source.resolve("manga.cbz"), "novo");

        Job job = createJob(source, dest);
        fileMoverService.moveFile(job, source.resolve("manga.cbz"), dest);

        assertTrue(Files.exists(dest.resolve("manga_2.cbz")));
    }

    @Test
    void moveFile_noConflict_keepsOriginalName() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);

        Files.writeString(source.resolve("manga.cbz"), "conteudo");

        Job job = createJob(source, dest);
        fileMoverService.moveFile(job, source.resolve("manga.cbz"), dest);

        assertTrue(Files.exists(dest.resolve("manga.cbz")));
        assertFalse(Files.exists(source.resolve("manga.cbz")));
    }

    @Test
    void moveFile_fileWithoutExtension_conflictResolution() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.createDirectories(dest);

        Files.writeString(dest.resolve("arquivo"), "existente");
        Files.writeString(source.resolve("arquivo"), "novo");

        Job job = createJob(source, dest);
        fileMoverService.moveFile(job, source.resolve("arquivo"), dest);

        assertTrue(Files.exists(dest.resolve("arquivo")));
        assertTrue(Files.exists(dest.resolve("arquivo_1")));
    }

    @Test
    void moveFile_createsDestDirIfNotExists() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("destino_novo");
        Files.createDirectories(source);
        Files.writeString(source.resolve("manga.cbz"), "conteudo");

        Job job = createJob(source, dest);
        fileMoverService.moveFile(job, source.resolve("manga.cbz"), dest);

        assertTrue(Files.isDirectory(dest));
        assertTrue(Files.exists(dest.resolve("manga.cbz")));
    }

    @Test
    void moveFile_recordsOkInHistory() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.writeString(source.resolve("manga.cbz"), "conteudo");

        Job job = createJob(source, dest);
        fileMoverService.moveFile(job, source.resolve("manga.cbz"), dest);

        verify(historyService).record(1L, "manga.cbz", "OK", null);
    }
}
