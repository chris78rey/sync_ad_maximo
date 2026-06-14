package com.syncadmaximo.orchestration;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.util.StringSanitizer;

/**
 * Modo de ejecución del proceso de sincronización.
 */
public enum ExecutionMode {
    DRY_RUN,
    PRODUCTION;

    public boolean isDryRun() {
        return this == DRY_RUN;
    }

    public static ExecutionMode fromConfig(AppConfig config) {
        if (config == null) {
            return DRY_RUN;
        }

        String explicitMode = StringSanitizer.trimToNull(config.getString("sync.execution.mode", null));
        if (explicitMode == null) {
            explicitMode = StringSanitizer.trimToNull(config.getString("sync.mode", null));
        }
        if (explicitMode != null) {
            if ("PRODUCTION".equalsIgnoreCase(explicitMode) || "PROD".equalsIgnoreCase(explicitMode)) {
                return PRODUCTION;
            }
            if ("DRY_RUN".equalsIgnoreCase(explicitMode) || "DRYRUN".equalsIgnoreCase(explicitMode) || "SIMULATION".equalsIgnoreCase(explicitMode)) {
                return DRY_RUN;
            }
        }

        if (config.getBoolean("sync.production", false)) {
            return PRODUCTION;
        }
        if (config.getBoolean("sync.dryRun", true)) {
            return DRY_RUN;
        }
        return PRODUCTION;
    }
}
