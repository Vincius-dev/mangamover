package com.mangamover.model;

public class LogEntry {
    public long id;
    public String level;   // INFO, WARN, ERROR
    public String source;  // watcher, mover, job
    public String message;
    public String timestamp;
}
