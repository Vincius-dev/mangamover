package com.mangamover.service;

import com.mangamover.db.Database;
import com.mangamover.model.HistoryEntry;
import com.mangamover.model.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {

    private Database db;
    private HistoryService historyService;
    private JobService jobService;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(":memory:");
        historyService = new HistoryService(db);
        jobService = new JobService(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.getConnection().close();
    }

    private long createJobId(String name) throws SQLException {
        Job j = new Job();
        j.name = name;
        j.sourcePath = "/s";
        j.destPath = "/d";
        j.watch = false;
        j.active = true;
        return jobService.create(j).id;
    }

    @Test
    void record_andFindPaged_returnsEntry() throws SQLException {
        long jobId = createJobId("Job");
        historyService.record(jobId, "manga.cbz", "OK", null);

        List<HistoryEntry> entries = historyService.findPaged(1, 10, null);
        assertEquals(1, entries.size());
        assertEquals(jobId, entries.get(0).jobId);
        assertEquals("manga.cbz", entries.get(0).filename);
        assertEquals("OK", entries.get(0).status);
        assertNull(entries.get(0).message);
    }

    @Test
    void record_withErrorMessage_persistsMessage() throws SQLException {
        long jobId = createJobId("Job");
        historyService.record(jobId, "erro.cbz", "ERROR", "Permissão negada");

        List<HistoryEntry> entries = historyService.findPaged(1, 10, null);
        assertEquals("ERROR", entries.get(0).status);
        assertEquals("Permissão negada", entries.get(0).message);
    }

    @Test
    void findPaged_returnsEntriesInDescendingOrder() throws SQLException {
        long jobId = createJobId("Job");
        historyService.record(jobId, "primeiro.cbz", "OK", null);
        historyService.record(jobId, "segundo.cbz", "OK", null);
        historyService.record(jobId, "terceiro.cbz", "OK", null);

        List<HistoryEntry> entries = historyService.findPaged(1, 10, null);
        assertEquals("terceiro.cbz", entries.get(0).filename);
        assertEquals("segundo.cbz", entries.get(1).filename);
        assertEquals("primeiro.cbz", entries.get(2).filename);
    }

    @Test
    void findPaged_paginationWorks() throws SQLException {
        long jobId = createJobId("Job");
        for (int i = 1; i <= 5; i++) {
            historyService.record(jobId, "manga" + i + ".cbz", "OK", null);
        }

        List<HistoryEntry> page1 = historyService.findPaged(1, 2, null);
        List<HistoryEntry> page2 = historyService.findPaged(2, 2, null);
        List<HistoryEntry> page3 = historyService.findPaged(3, 2, null);

        assertEquals(2, page1.size());
        assertEquals(2, page2.size());
        assertEquals(1, page3.size());
    }

    @Test
    void findPaged_filterByJobId() throws SQLException {
        long jobA = createJobId("Job A");
        long jobB = createJobId("Job B");

        historyService.record(jobA, "arqA.cbz", "OK", null);
        historyService.record(jobA, "arqA2.cbz", "OK", null);
        historyService.record(jobB, "arqB.cbz", "OK", null);

        List<HistoryEntry> jobAHistory = historyService.findPaged(1, 10, jobA);
        assertEquals(2, jobAHistory.size());
        assertTrue(jobAHistory.stream().allMatch(e -> e.jobId == jobA));
    }

    @Test
    void findPaged_noJobIdFilter_returnsAllJobs() throws SQLException {
        long jobA = createJobId("Job A");
        long jobB = createJobId("Job B");

        historyService.record(jobA, "arqA.cbz", "OK", null);
        historyService.record(jobB, "arqB.cbz", "OK", null);

        List<HistoryEntry> all = historyService.findPaged(1, 10, null);
        assertEquals(2, all.size());
    }

    @Test
    void count_withNoFilter_returnsTotal() throws SQLException {
        long jobId = createJobId("Job");
        historyService.record(jobId, "a.cbz", "OK", null);
        historyService.record(jobId, "b.cbz", "OK", null);
        historyService.record(jobId, "c.cbz", "ERROR", "erro");

        long total = historyService.count(null);
        assertEquals(3, total);
    }

    @Test
    void count_filteredByJobId() throws SQLException {
        long jobA = createJobId("Job A");
        long jobB = createJobId("Job B");

        historyService.record(jobA, "a1.cbz", "OK", null);
        historyService.record(jobA, "a2.cbz", "OK", null);
        historyService.record(jobB, "b1.cbz", "OK", null);

        assertEquals(2, historyService.count(jobA));
        assertEquals(1, historyService.count(jobB));
    }

    @Test
    void count_empty_returnsZero() throws SQLException {
        assertEquals(0, historyService.count(null));
    }

    @Test
    void entry_hasCreatedAt() throws SQLException {
        long jobId = createJobId("Job");
        historyService.record(jobId, "manga.cbz", "OK", null);

        HistoryEntry entry = historyService.findPaged(1, 10, null).get(0);
        assertNotNull(entry.createdAt);
        assertFalse(entry.createdAt.isBlank());
    }

    @Test
    void entry_hasId() throws SQLException {
        long jobId = createJobId("Job");
        historyService.record(jobId, "manga.cbz", "OK", null);

        HistoryEntry entry = historyService.findPaged(1, 10, null).get(0);
        assertTrue(entry.id > 0);
    }
}
