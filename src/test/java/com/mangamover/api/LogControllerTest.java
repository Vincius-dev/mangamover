package com.mangamover.api;

import com.mangamover.model.LogEntry;
import com.mangamover.service.LogStore;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LogControllerTest {

    private LogStore logStore;
    private LogController controller;

    @BeforeEach
    void setUp() {
        logStore = new LogStore();
        controller = new LogController(logStore);
    }

    private Context mockCtx() {
        Context ctx = mock(Context.class, RETURNS_DEEP_STUBS);
        when(ctx.status(anyInt())).thenReturn(ctx);
        return ctx;
    }

    private void invokeList(Context ctx) throws Exception {
        Method m = LogController.class.getDeclaredMethod("list", Context.class);
        m.setAccessible(true);
        m.invoke(controller, ctx);
    }

    private void invokeClear(Context ctx) throws Exception {
        Method m = LogController.class.getDeclaredMethod("clear", Context.class);
        m.setAccessible(true);
        m.invoke(controller, ctx);
    }

    @Test
    void list_returnsAllLogsWhenNoFilter() throws Exception {
        logStore.info("mover", "msg1");
        logStore.warn("watcher", "msg2");

        Context ctx = mockCtx();
        when(ctx.queryParam("level")).thenReturn(null);
        when(ctx.queryParam("source")).thenReturn(null);
        when(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(200)).thenReturn(200);

        invokeList(ctx);

        verify(ctx).json(argThat(obj -> obj instanceof List && ((List<?>) obj).size() == 2));
    }

    @Test
    void list_filtersByLevel() throws Exception {
        logStore.info("mover", "info msg");
        logStore.warn("mover", "warn msg");

        Context ctx = mockCtx();
        when(ctx.queryParam("level")).thenReturn("INFO");
        when(ctx.queryParam("source")).thenReturn(null);
        when(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(200)).thenReturn(200);

        invokeList(ctx);

        verify(ctx).json(argThat(obj -> obj instanceof List && ((List<?>) obj).size() == 1));
    }

    @Test
    void list_filtersBySource() throws Exception {
        logStore.info("mover", "msg mover");
        logStore.info("watcher", "msg watcher");

        Context ctx = mockCtx();
        when(ctx.queryParam("level")).thenReturn(null);
        when(ctx.queryParam("source")).thenReturn("mover");
        when(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(200)).thenReturn(200);

        invokeList(ctx);

        verify(ctx).json(argThat(obj -> {
            List<?> list = (List<?>) obj;
            return list.size() == 1 && ((LogEntry) list.get(0)).source.equals("mover");
        }));
    }

    @Test
    void list_respectsLimit() throws Exception {
        for (int i = 0; i < 10; i++) logStore.info("src", "msg " + i);

        Context ctx = mockCtx();
        when(ctx.queryParam("level")).thenReturn(null);
        when(ctx.queryParam("source")).thenReturn(null);
        when(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(200)).thenReturn(3);

        invokeList(ctx);

        verify(ctx).json(argThat(obj -> ((List<?>) obj).size() == 3));
    }

    @Test
    void clear_emptiesLogStore() throws Exception {
        logStore.info("src", "msg1");
        logStore.error("src", "msg2");

        Context ctx = mockCtx();
        invokeClear(ctx);

        assertTrue(logStore.get(null, null, 10).isEmpty());
    }

    @Test
    void clear_returns204() throws Exception {
        Context ctx = mockCtx();
        invokeClear(ctx);
        verify(ctx).status(204);
    }
}
