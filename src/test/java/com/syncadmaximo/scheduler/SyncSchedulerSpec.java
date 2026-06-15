package com.syncadmaximo.scheduler;

import com.syncadmaximo.testsupport.TestAssertions;

import java.time.Instant;
import java.time.ZoneId;

public final class SyncSchedulerSpec {

    public static void runAll() {
        computesNextDelayFromConfiguredTime();
    }

    static void computesNextDelayFromConfiguredTime() {
        Instant now = Instant.parse("2026-06-14T10:15:00Z");

        long delay = SyncScheduler.computeInitialDelaySeconds("10:30:00", ZoneId.of("UTC"), now);
        TestAssertions.equals(900L, delay, "Debe calcular el retraso hasta la siguiente ejecucion");

        long nextDayDelay = SyncScheduler.computeInitialDelaySeconds("09:00:00", ZoneId.of("UTC"), now);
        TestAssertions.equals(81900L, nextDayDelay, "Debe programar para el dia siguiente cuando ya paso la hora");
    }
}
