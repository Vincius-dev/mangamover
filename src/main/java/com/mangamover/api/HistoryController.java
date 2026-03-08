package com.mangamover.api;

import com.mangamover.service.HistoryService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void register(Javalin app) {
        app.get("/api/history", this::list);
    }

    private void list(Context ctx) throws Exception {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int perPage = ctx.queryParamAsClass("per_page", Integer.class).getOrDefault(20);
        String jobIdStr = ctx.queryParam("job_id");
        Long jobId = jobIdStr != null ? Long.parseLong(jobIdStr) : null;
        long total = historyService.count(jobId);
        ctx.json(Map.of(
                "data", historyService.findPaged(page, perPage, jobId),
                "total", total,
                "page", page,
                "per_page", perPage
        ));
    }
}
