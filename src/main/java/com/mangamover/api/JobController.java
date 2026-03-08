package com.mangamover.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangamover.model.Job;
import com.mangamover.service.FileMoverService;
import com.mangamover.service.JobService;
import com.mangamover.service.WatcherService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class JobController {
    private final JobService jobService;
    private final WatcherService watcherService;
    private final FileMoverService fileMoverService;
    private final ObjectMapper mapper;

    public JobController(JobService jobService, WatcherService watcherService,
                         FileMoverService fileMoverService, ObjectMapper mapper) {
        this.jobService = jobService;
        this.watcherService = watcherService;
        this.fileMoverService = fileMoverService;
        this.mapper = mapper;
    }

    public void register(Javalin app) {
        app.get("/api/jobs", this::list);
        app.post("/api/jobs", this::create);
        app.put("/api/jobs/{id}", this::update);
        app.delete("/api/jobs/{id}", this::delete);
        app.post("/api/jobs/{id}/run", this::run);
    }

    private void list(Context ctx) throws Exception {
        ctx.json(jobService.findAll());
    }

    private void create(Context ctx) throws Exception {
        Job body = mapper.readValue(ctx.body(), Job.class);
        Job job = jobService.create(body);
        if (job.watch && job.active) watcherService.start(job);
        ctx.status(201).json(job);
    }

    private void update(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Job existing = jobService.findById(id);
        if (existing == null) { ctx.status(404); return; }
        Job body = mapper.readValue(ctx.body(), Job.class);
        body.id = id;
        Job job = jobService.update(body);
        watcherService.stop(id);
        if (job.watch && job.active) watcherService.start(job);
        ctx.json(job);
    }

    private void delete(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        watcherService.stop(id);
        if (jobService.delete(id)) ctx.status(204);
        else ctx.status(404);
    }

    private void run(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Job job = jobService.findById(id);
        if (job == null) { ctx.status(404); return; }
        Thread.ofVirtual().start(() -> fileMoverService.moveAll(job));
        ctx.status(202).json(Map.of("message", "Job started"));
    }
}
