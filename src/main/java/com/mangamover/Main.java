package com.mangamover;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.mangamover.api.HistoryController;
import com.mangamover.api.JobController;
import com.mangamover.api.StatsController;
import com.mangamover.config.AppConfig;
import com.mangamover.db.Database;
import com.mangamover.model.Job;
import com.mangamover.service.FileMoverService;
import com.mangamover.service.HistoryService;
import com.mangamover.service.JobService;
import com.mangamover.service.WatcherService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Database db = new Database(AppConfig.DB_PATH);
        HistoryService historyService = new HistoryService(db);
        JobService jobService = new JobService(db);
        FileMoverService fileMoverService = new FileMoverService(historyService);
        WatcherService watcherService = new WatcherService(fileMoverService);

        ObjectMapper mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.jsonMapper(new JavalinJackson(mapper, true));
        });

        new JobController(jobService, watcherService, fileMoverService, mapper).register(app);
        new HistoryController(historyService).register(app);
        new StatsController(jobService, historyService, watcherService).register(app);
        app.get("/", ctx -> ctx.redirect("/index.html"));

        List<Job> jobs = jobService.findAll();
        for (Job job : jobs) {
            if (job.watch && job.active) watcherService.start(job);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            watcherService.stopAll();
            app.stop();
        }));

        app.start(AppConfig.PORT);
        log.info("MangaMover running on port {}", AppConfig.PORT);
    }
}
