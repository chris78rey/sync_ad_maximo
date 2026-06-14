package com.syncadmaximo.model;

import java.time.Instant;
import java.util.Objects;

public class SyncExecution {

    private String runId;
    private Instant startedAt;
    private Instant finishedAt;
    private String initiatedBy;
    private String processName;
    private boolean dryRun;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public String toString() {
        return "SyncExecution{" +
                "runId='" + runId + '\'' +
                ", processName='" + processName + '\'' +
                ", dryRun=" + dryRun +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncExecution)) {
            return false;
        }
        SyncExecution that = (SyncExecution) o;
        return Objects.equals(runId, that.runId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId);
    }
}
