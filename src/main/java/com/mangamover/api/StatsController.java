package com.mangamover.api;

import com.mangamover.service.HistoryService;
import com.mangamover.service.JobService;
import com.mangamover.service.WatcherService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class StatsController {
    private final JobService jobService;
    private final HistoryService historyService;
    private final WatcherService watcherService;

    public StatsController(JobService jobService, HistoryService historyService, WatcherService watcherService) {
        this.jobService = jobService;
        this.historyService = historyService;
        this.watcherService = watcherService;
    }

    public void register(Javalin app) {
        app.get("/api/stats", this::stats);
    }

    private void stats(Context ctx) throws Exception {
        ctx.json(Map.of(
                "total_jobs", jobService.findAll().size(),
                "active_watchers", watcherService.activeCount(),
                "total_moves", historyService.count(null)
        ));
    }
}
