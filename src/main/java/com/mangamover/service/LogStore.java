package com.mangamover.service;

import com.mangamover.model.LogEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LogStore {
    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private final AtomicLong seq = new AtomicLong(1);

    public void info(String source, String message) {
        add("INFO", source, message);
    }

    public void warn(String source, String message) {
        add("WARN", source, message);
    }

    public void error(String source, String message) {
        add("ERROR", source, message);
    }

    private synchronized void add(String level, String source, String message) {
        LogEntry e = new LogEntry();
        e.id = seq.getAndIncrement();
        e.level = level;
        e.source = source;
        e.message = message;
        e.timestamp = LocalDateTime.now().format(FMT);
        entries.addFirst(e);
        if (entries.size() > MAX_ENTRIES) entries.removeLast();
    }

    public synchronized List<LogEntry> get(String level, String source, int limit) {
        return entries.stream()
                .filter(e -> level == null || e.level.equalsIgnoreCase(level))
                .filter(e -> source == null || e.source.equalsIgnoreCase(source))
                .limit(limit > 0 ? limit : MAX_ENTRIES)
                .collect(Collectors.toList());
    }

    public synchronized void clear() {
        entries.clear();
    }
}
