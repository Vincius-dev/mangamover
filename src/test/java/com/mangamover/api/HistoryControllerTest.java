package com.mangamover.api;

import com.mangamover.model.HistoryEntry;
import com.mangamover.service.HistoryService;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HistoryControllerTest {

    private HistoryService historyService;
    private HistoryController controller;

    @BeforeEach
    void setUp() {
        historyService = mock(HistoryService.class);
        controller = new HistoryController(historyService);
    }

    private Context mockCtx() {
        Context ctx = mock(Context.class, RETURNS_DEEP_STUBS);
        when(ctx.status(anyInt())).thenReturn(ctx);
        return ctx;
    }

    private void invokeList(Context ctx) throws Exception {
        Method m = HistoryController.class.getDeclaredMethod("list", Context.class);
        m.setAccessible(true);
        m.invoke(controller, ctx);
    }

    @Test
    void list_noFilter_returnsAllEntries() throws Exception {
        when(historyService.count(null)).thenReturn(2L);
        when(historyService.findPaged(1, 20, null)).thenReturn(List.of(new HistoryEntry(), new HistoryEntry()));

        Context ctx = mockCtx();
        when(ctx.queryParam("job_id")).thenReturn(null);
        when(ctx.queryParamAsClass("page", Integer.class).getOrDefault(1)).thenReturn(1);
        when(ctx.queryParamAsClass("per_page", Integer.class).getOrDefault(20)).thenReturn(20);

        invokeList(ctx);

        verify(historyService).count(null);
        verify(historyService).findPaged(1, 20, null);
        verify(ctx).json(any(Map.class));
    }

    @Test
    void list_withJobIdFilter_filtersCorrectly() throws Exception {
        when(historyService.count(5L)).thenReturn(1L);
        when(historyService.findPaged(1, 20, 5L)).thenReturn(List.of(new HistoryEntry()));

        Context ctx = mockCtx();
        when(ctx.queryParam("job_id")).thenReturn("5");
        when(ctx.queryParamAsClass("page", Integer.class).getOrDefault(1)).thenReturn(1);
        when(ctx.queryParamAsClass("per_page", Integer.class).getOrDefault(20)).thenReturn(20);

        invokeList(ctx);

        verify(historyService).count(5L);
        verify(historyService).findPaged(1, 20, 5L);
    }

    @Test
    void list_withPagination_passesCorrectPageParams() throws Exception {
        when(historyService.count(null)).thenReturn(50L);
        when(historyService.findPaged(3, 10, null)).thenReturn(List.of());

        Context ctx = mockCtx();
        when(ctx.queryParam("job_id")).thenReturn(null);
        when(ctx.queryParamAsClass("page", Integer.class).getOrDefault(1)).thenReturn(3);
        when(ctx.queryParamAsClass("per_page", Integer.class).getOrDefault(20)).thenReturn(10);

        invokeList(ctx);

        verify(historyService).findPaged(3, 10, null);
    }
}
