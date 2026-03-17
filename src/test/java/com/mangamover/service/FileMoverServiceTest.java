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
    void moveAll_recursive_movesSuwayomiStructureToKavita() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");

        // Suwayomi structure: source/AllManga (EN)/Solo Leveling/Chapter 7.cbz
        Path seriesDir = source.resolve("AllManga (EN)").resolve("Solo Leveling");
        Files.createDirectories(seriesDir);
        Files.writeString(seriesDir.resolve("Chapter 7.cbz"), "content");
        Files.writeString(seriesDir.resolve("Chapter 72.cbz"), "content2");

        Job job = createJob(source, dest);
        job.recursive = true;
        fileMoverService.moveAll(job);

        // Should end up as dest/Solo Leveling/Solo Leveling Ch.007.cbz
        assertTrue(Files.exists(dest.resolve("Solo Leveling").resolve("Solo Leveling Ch.007.cbz")));
        assertTrue(Files.exists(dest.resolve("Solo Leveling").resolve("Solo Leveling Ch.072.cbz")));
    }

    @Test
    void moveAll_recursive_nonChapterFile_keepsOriginalName() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");

        Path seriesDir = source.resolve("MySeries");
        Files.createDirectories(seriesDir);
        Files.writeString(seriesDir.resolve("random_file.cbz"), "content");

        Job job = createJob(source, dest);
        job.recursive = true;
        fileMoverService.moveAll(job);

        // Non-matching filename keeps original name under series folder
        assertTrue(Files.exists(dest.resolve("MySeries").resolve("random_file.cbz")));
    }

    @Test
    void moveAll_recursive_multipleSeriesDirs() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");

        Path series1 = source.resolve("src1").resolve("Series A");
        Path series2 = source.resolve("src2").resolve("Series B");
        Files.createDirectories(series1);
        Files.createDirectories(series2);
        Files.writeString(series1.resolve("Chapter 1.cbz"), "a");
        Files.writeString(series2.resolve("Chapter 2.cbr"), "b");

        Job job = createJob(source, dest);
        job.recursive = true;
        fileMoverService.moveAll(job);

        assertTrue(Files.exists(dest.resolve("Series A").resolve("Series A Ch.001.cbz")));
        assertTrue(Files.exists(dest.resolve("Series B").resolve("Series B Ch.002.cbr")));
    }

    @Test
    void moveAll_recursive_ignoresNonCbzCbrFiles() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");

        Path seriesDir = source.resolve("Series");
        Files.createDirectories(seriesDir);
        Files.writeString(seriesDir.resolve("Chapter 1.cbz"), "content");
        Files.writeString(seriesDir.resolve("readme.txt"), "text");

        Job job = createJob(source, dest);
        job.recursive = true;
        fileMoverService.moveAll(job);

        assertTrue(Files.exists(dest.resolve("Series").resolve("Series Ch.001.cbz")));
        // txt file should not be moved
        assertTrue(Files.exists(seriesDir.resolve("readme.txt")));
    }

    @Test
    void moveAll_nonRecursive_behavesAsFlat() throws Exception {
        Path source = tempDir.resolve("source");
        Path dest = tempDir.resolve("dest");
        Files.createDirectories(source);
        Files.writeString(source.resolve("manga1.cbz"), "c1");

        Job job = createJob(source, dest);
        job.recursive = false;
        fileMoverService.moveAll(job);

        assertTrue(Files.exists(dest.resolve("manga1.cbz")));
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
