package com.mangamover.api;

import com.mangamover.model.Job;
import com.mangamover.service.HistoryService;
import com.mangamover.service.JobService;
import com.mangamover.service.WatcherService;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StatsControllerTest {

    private JobService jobService;
    private HistoryService historyService;
    private WatcherService watcherService;
    private StatsController controller;

    @BeforeEach
    void setUp() {
        jobService = mock(JobService.class);
        historyService = mock(HistoryService.class);
        watcherService = mock(WatcherService.class);
        controller = new StatsController(jobService, historyService, watcherService);
    }

    private Context mockCtx() {
        Context ctx = mock(Context.class, RETURNS_DEEP_STUBS);
        when(ctx.status(anyInt())).thenReturn(ctx);
        return ctx;
    }

    private void invokeStats(Context ctx) throws Exception {
        Method m = StatsController.class.getDeclaredMethod("stats", Context.class);
        m.setAccessible(true);
        m.invoke(controller, ctx);
    }

    @Test
    void stats_aggregatesAllServices() throws Exception {
        when(jobService.findAll()).thenReturn(List.of(new Job(), new Job(), new Job()));
        when(watcherService.activeCount()).thenReturn(2);
        when(historyService.count(null)).thenReturn(42L);

        Context ctx = mockCtx();
        invokeStats(ctx);

        verify(ctx).json(argThat(obj -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return Integer.valueOf(3).equals(map.get("total_jobs"))
                    && Integer.valueOf(2).equals(map.get("active_watchers"))
                    && Long.valueOf(42L).equals(map.get("total_moves"));
        }));
    }

    @Test
    void stats_withNoJobsAndNoHistory() throws Exception {
        when(jobService.findAll()).thenReturn(List.of());
        when(watcherService.activeCount()).thenReturn(0);
        when(historyService.count(null)).thenReturn(0L);

        Context ctx = mockCtx();
        invokeStats(ctx);

        verify(jobService).findAll();
        verify(watcherService).activeCount();
        verify(historyService).count(null);
        verify(ctx).json(any(Map.class));
    }
}
