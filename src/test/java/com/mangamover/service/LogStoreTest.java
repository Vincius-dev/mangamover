package com.mangamover.service;

import com.mangamover.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogStoreTest {

    private LogStore logStore;

    @BeforeEach
    void setUp() {
        logStore = new LogStore();
    }

    @Test
    void info_addsEntryWithCorrectLevel() {
        logStore.info("src", "mensagem info");
        List<LogEntry> entries = logStore.get(null, null, 10);
        assertEquals(1, entries.size());
        assertEquals("INFO", entries.get(0).level);
        assertEquals("src", entries.get(0).source);
        assertEquals("mensagem info", entries.get(0).message);
    }

    @Test
    void warn_addsEntryWithCorrectLevel() {
        logStore.warn("src", "mensagem warn");
        List<LogEntry> entries = logStore.get(null, null, 10);
        assertEquals(1, entries.size());
        assertEquals("WARN", entries.get(0).level);
    }

    @Test
    void error_addsEntryWithCorrectLevel() {
        logStore.error("src", "mensagem error");
        List<LogEntry> entries = logStore.get(null, null, 10);
        assertEquals(1, entries.size());
        assertEquals("ERROR", entries.get(0).level);
    }

    @Test
    void get_filterByLevel_returnsOnlyMatchingEntries() {
        logStore.info("src", "info msg");
        logStore.warn("src", "warn msg");
        logStore.error("src", "error msg");

        List<LogEntry> infoOnly = logStore.get("INFO", null, 10);
        assertEquals(1, infoOnly.size());
        assertEquals("INFO", infoOnly.get(0).level);
    }

    @Test
    void get_filterByLevel_caseInsensitive() {
        logStore.info("src", "info msg");
        logStore.warn("src", "warn msg");

        List<LogEntry> result = logStore.get("info", null, 10);
        assertEquals(1, result.size());
        assertEquals("INFO", result.get(0).level);
    }

    @Test
    void get_filterBySource_returnsOnlyMatchingEntries() {
        logStore.info("mover", "msg mover");
        logStore.info("watcher", "msg watcher");

        List<LogEntry> result = logStore.get(null, "mover", 10);
        assertEquals(1, result.size());
        assertEquals("mover", result.get(0).source);
    }

    @Test
    void get_filterByLevelAndSource() {
        logStore.info("mover", "info mover");
        logStore.warn("mover", "warn mover");
        logStore.info("watcher", "info watcher");

        List<LogEntry> result = logStore.get("INFO", "mover", 10);
        assertEquals(1, result.size());
        assertEquals("INFO", result.get(0).level);
        assertEquals("mover", result.get(0).source);
    }

    @Test
    void get_withLimit_respectsLimit() {
        for (int i = 0; i < 10; i++) {
            logStore.info("src", "msg " + i);
        }

        List<LogEntry> result = logStore.get(null, null, 3);
        assertEquals(3, result.size());
    }

    @Test
    void get_withZeroLimit_returnsAll() {
        for (int i = 0; i < 5; i++) {
            logStore.info("src", "msg " + i);
        }

        List<LogEntry> result = logStore.get(null, null, 0);
        assertEquals(5, result.size());
    }

    @Test
    void entries_haveIncrementalIds() {
        logStore.info("src", "primeiro");
        logStore.info("src", "segundo");
        logStore.info("src", "terceiro");

        List<LogEntry> entries = logStore.get(null, null, 10);
        // entries são retornadas mais recentes primeiro
        assertEquals("terceiro", entries.get(0).message);
        assertEquals("segundo", entries.get(1).message);
        assertEquals("primeiro", entries.get(2).message);
        assertTrue(entries.get(0).id > entries.get(1).id);
        assertTrue(entries.get(1).id > entries.get(2).id);
    }

    @Test
    void entries_haveTimestamp() {
        logStore.info("src", "msg");
        LogEntry entry = logStore.get(null, null, 1).get(0);
        assertNotNull(entry.timestamp);
        assertFalse(entry.timestamp.isBlank());
    }

    @Test
    void clear_removesAllEntries() {
        logStore.info("src", "msg1");
        logStore.warn("src", "msg2");
        logStore.clear();

        List<LogEntry> entries = logStore.get(null, null, 10);
        assertTrue(entries.isEmpty());
    }

    @Test
    void maxEntries_limitsStoredEntriesTo500() {
        for (int i = 0; i < 510; i++) {
            logStore.info("src", "msg " + i);
        }

        List<LogEntry> entries = logStore.get(null, null, 0);
        assertEquals(500, entries.size());
    }
}
