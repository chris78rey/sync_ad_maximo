package com.syncadmaximo.scheduler;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.orchestration.ExecutionMode;
import com.syncadmaximo.orchestration.SyncOrchestrator;
import com.syncadmaximo.service.SyncService;
import com.syncadmaximo.util.StringSanitizer;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entrada ejecutable para disparar la sincronización de forma manual o periódica.
 */
public final class SyncScheduler implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(SyncScheduler.class.getName());

    private final AppConfig config;
    private final SyncService syncService;
    private final ScheduledExecutorService executor;

    public SyncScheduler() {
        this(AppConfig.getInstance(), new SyncOrchestrator());
    }

    public SyncScheduler(AppConfig config, SyncService syncService) {
        this.config = config == null ? AppConfig.getInstance() : config;
        this.syncService = syncService == null ? new SyncOrchestrator(this.config, null, null, null, null, null) : syncService;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sync-ad-maximo-scheduler");
            thread.setDaemon(false);
            return thread;
        });
    }

    public SyncResult runOnce() {
        return syncService.execute(createExecution());
    }

    public void startPeriodic() {
        String schedulerTime = StringSanitizer.trimToNull(config.getSchedulerTime());
        if (schedulerTime != null) {
            long initialDelaySeconds = computeInitialDelaySeconds(schedulerTime, config.getZoneId(), Instant.now());
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    runScheduledExecution();
                }
            }, initialDelaySeconds, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
            return;
        }

        long initialDelaySeconds = Math.max(0, config.getInt("sync.scheduler.initialDelaySeconds", 0));
        long fixedDelaySeconds = Math.max(1, config.getInt("sync.scheduler.fixedDelaySeconds", 3600));
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                runScheduledExecution();
            }
        }, initialDelaySeconds, fixedDelaySeconds, TimeUnit.SECONDS);
    }

    public void startAndBlock() {
        startPeriodic();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }, "sync-ad-maximo-shutdown"));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        SyncService service = new SyncOrchestrator();
        SyncScheduler scheduler = new SyncScheduler(config, service);
        boolean schedule = shouldSchedule(args, config);
        if (schedule) {
            scheduler.startAndBlock();
            return;
        }
        SyncResult result = scheduler.runOnce();
        if (result != null) {
            LOGGER.info(result.getMessage());
        }
        scheduler.close();
    }

    private SyncExecution createExecution() {
        SyncExecution execution = new SyncExecution();
        execution.setRunId(UUID.randomUUID().toString());
        execution.setStartedAt(Instant.now());
        execution.setProcessName(config.getAppName());
        execution.setInitiatedBy("scheduler");
        execution.setDryRun(ExecutionMode.fromConfig(config).isDryRun());
        return execution;
    }

    void runScheduledExecution() {
        try {
            SyncResult result = runOnce();
            LOGGER.info(result == null ? "Sin resultado de ejecucion" : result.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Fallo una ejecucion programada", ex);
        }
    }

    static long computeInitialDelaySeconds(String schedulerTime, ZoneId zoneId, Instant now) {
        if (schedulerTime == null || schedulerTime.trim().isEmpty()) {
            return 0L;
        }
        ZoneId effectiveZone = zoneId == null ? ZoneId.of("UTC") : zoneId;
        LocalTime targetTime = parseSchedulerTime(schedulerTime.trim());
        if (targetTime == null) {
            return 0L;
        }
        ZonedDateTime current = now == null ? ZonedDateTime.now(effectiveZone) : now.atZone(effectiveZone);
        ZonedDateTime next = current.withHour(targetTime.getHour())
                .withMinute(targetTime.getMinute())
                .withSecond(targetTime.getSecond())
                .withNano(0);
        if (!next.isAfter(current)) {
            next = next.plusDays(1);
        }
        return Math.max(0L, next.toEpochSecond() - current.toEpochSecond());
    }

    private static LocalTime parseSchedulerTime(String schedulerTime) {
        try {
            return LocalTime.parse(schedulerTime);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean shouldSchedule(String[] args, AppConfig config) {
        if (args != null) {
            for (String arg : args) {
                if (arg == null) {
                    continue;
                }
                String normalized = arg.trim().toLowerCase(Locale.ROOT);
                if ("--schedule".equals(normalized) || "schedule".equals(normalized)) {
                    return true;
                }
                if ("--once".equals(normalized) || "once".equals(normalized)) {
                    return false;
                }
            }
        }
        return config.getBoolean("sync.scheduler.enabled", false);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
