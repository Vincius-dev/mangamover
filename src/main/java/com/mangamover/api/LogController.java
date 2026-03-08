package com.mangamover.api;

import com.mangamover.service.LogStore;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class LogController {
    private final LogStore logStore;

    public LogController(LogStore logStore) {
        this.logStore = logStore;
    }

    public void register(Javalin app) {
        app.get("/api/logs", this::list);
        app.delete("/api/logs", this::clear);
    }

    private void list(Context ctx) {
        String level = ctx.queryParam("level");
        String source = ctx.queryParam("source");
        int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(200);
        ctx.json(logStore.get(level, source, limit));
    }

    private void clear(Context ctx) {
        logStore.clear();
        ctx.status(204);
    }
}
