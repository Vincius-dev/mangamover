package com.mangamover.service;

import com.mangamover.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private final ScheduledExecutorService executor;
    private final FileMoverService fileMoverService;
    private final LogStore logStore;
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public SchedulerService(FileMoverService fileMoverService, LogStore logStore) {
        this.fileMoverService = fileMoverService;
        this.logStore = logStore;
        this.executor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());
    }

    public void schedule(Job job) {
        if (job.scheduleMinutes <= 0) return;
        unschedule(job.id);
        long intervalMinutes = job.scheduleMinutes;
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                log.info("Execução agendada do Job {}: {}", job.id, job.name);
                logStore.info("scheduler", "[Job " + job.id + "] Execução agendada iniciada");
                fileMoverService.moveAll(job);
            } catch (Exception e) {
                log.error("Erro na execução agendada do Job {}: {}", job.id, e.getMessage());
                logStore.error("scheduler", "[Job " + job.id + "] Erro na execução agendada: " + e.getMessage());
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
        tasks.put(job.id, future);
        String msg = "[Job " + job.id + "] Agendado a cada " + intervalMinutes + " minuto(s)";
        log.info(msg);
        logStore.info("scheduler", msg);
    }

    public void unschedule(long jobId) {
        ScheduledFuture<?> future = tasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("Job {} desagendado", jobId);
        }
    }

    public void reschedule(Job job) {
        unschedule(job.id);
        schedule(job);
    }

    public boolean isScheduled(long jobId) {
        ScheduledFuture<?> future = tasks.get(jobId);
        return future != null && !future.isCancelled();
    }

    public void shutdown() {
        executor.shutdownNow();
        tasks.clear();
        log.info("SchedulerService encerrado");
    }
}
