package com.mangamover.service;

import com.mangamover.model.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulerServiceTest {

    private FileMoverService fileMoverService;
    private LogStore logStore;
    private SchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        fileMoverService = mock(FileMoverService.class);
        logStore = new LogStore();
        schedulerService = new SchedulerService(fileMoverService, logStore);
    }

    @AfterEach
    void tearDown() {
        schedulerService.shutdown();
    }

    private Job makeJob(long id, int scheduleMinutes) {
        Job j = new Job();
        j.id = id;
        j.name = "Test Job " + id;
        j.sourcePath = "/src";
        j.destPath = "/dst";
        j.active = true;
        j.scheduleMinutes = scheduleMinutes;
        return j;
    }

    @Test
    void schedule_withPositiveMinutes_isScheduled() {
        Job job = makeJob(1, 15);
        schedulerService.schedule(job);
        assertTrue(schedulerService.isScheduled(1));
    }

    @Test
    void schedule_withZeroMinutes_isNotScheduled() {
        Job job = makeJob(1, 0);
        schedulerService.schedule(job);
        assertFalse(schedulerService.isScheduled(1));
    }

    @Test
    void schedule_withNegativeMinutes_isNotScheduled() {
        Job job = makeJob(1, -1);
        schedulerService.schedule(job);
        assertFalse(schedulerService.isScheduled(1));
    }

    @Test
    void unschedule_removesScheduledJob() {
        Job job = makeJob(1, 15);
        schedulerService.schedule(job);
        assertTrue(schedulerService.isScheduled(1));

        schedulerService.unschedule(1);
        assertFalse(schedulerService.isScheduled(1));
    }

    @Test
    void unschedule_nonExistentJob_doesNotThrow() {
        assertDoesNotThrow(() -> schedulerService.unschedule(999));
    }

    @Test
    void reschedule_updatesSchedule() {
        Job job = makeJob(1, 15);
        schedulerService.schedule(job);
        assertTrue(schedulerService.isScheduled(1));

        job.scheduleMinutes = 30;
        schedulerService.reschedule(job);
        assertTrue(schedulerService.isScheduled(1));
    }

    @Test
    void reschedule_withZero_unschedules() {
        Job job = makeJob(1, 15);
        schedulerService.schedule(job);
        assertTrue(schedulerService.isScheduled(1));

        job.scheduleMinutes = 0;
        schedulerService.reschedule(job);
        assertFalse(schedulerService.isScheduled(1));
    }

    @Test
    void isScheduled_nonExistentJob_returnsFalse() {
        assertFalse(schedulerService.isScheduled(999));
    }

    @Test
    void shutdown_clearsAllSchedules() {
        schedulerService.schedule(makeJob(1, 15));
        schedulerService.schedule(makeJob(2, 30));
        assertTrue(schedulerService.isScheduled(1));
        assertTrue(schedulerService.isScheduled(2));

        schedulerService.shutdown();
        assertFalse(schedulerService.isScheduled(1));
        assertFalse(schedulerService.isScheduled(2));
    }

    @Test
    void schedule_multipleJobs_allScheduled() {
        schedulerService.schedule(makeJob(1, 15));
        schedulerService.schedule(makeJob(2, 30));
        schedulerService.schedule(makeJob(3, 60));

        assertTrue(schedulerService.isScheduled(1));
        assertTrue(schedulerService.isScheduled(2));
        assertTrue(schedulerService.isScheduled(3));
    }

    @Test
    void schedule_sameJobTwice_replacesSchedule() {
        Job job = makeJob(1, 15);
        schedulerService.schedule(job);
        schedulerService.schedule(job);
        assertTrue(schedulerService.isScheduled(1));
    }

    @Test
    void schedule_logsToLogStore() {
        Job job = makeJob(1, 15);
        schedulerService.schedule(job);

        var logs = logStore.get("INFO", "scheduler", 10);
        assertFalse(logs.isEmpty());
        assertTrue(logs.get(0).message.contains("Agendado"));
    }
}
