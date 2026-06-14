package com.syncadmaximo.web.report;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class ExecutionPresentationModel {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final SyncExecution execution;
    private final SyncResult result;
    private final List<SyncIssue> issues;
    private final Map<String, Integer> issueCountsByCode;
    private final String startedAtText;
    private final String finishedAtText;
    private final String durationText;

    ExecutionPresentationModel(SyncExecution execution,
                               SyncResult result,
                               ZoneId zoneId) {
        this.execution = execution;
        this.result = result;
        this.issues = Collections.unmodifiableList(new ArrayList<SyncIssue>(result == null ? Collections.<SyncIssue>emptyList() : result.getIssues()));
        this.issueCountsByCode = buildIssueCountsByCode(this.issues);
        this.startedAtText = formatInstant(execution == null ? null : execution.getStartedAt(), zoneId);
        this.finishedAtText = formatInstant(execution == null ? null : execution.getFinishedAt(), zoneId);
        this.durationText = calculateDuration(execution == null ? null : execution.getStartedAt(), execution == null ? null : execution.getFinishedAt());
    }

    public SyncExecution getExecution() {
        return execution;
    }

    public SyncResult getResult() {
        return result;
    }

    public List<SyncIssue> getIssues() {
        return issues;
    }

    public Map<String, Integer> getIssueCountsByCode() {
        return issueCountsByCode;
    }

    public String getRunId() {
        return execution == null ? null : execution.getRunId();
    }

    public String getProcessName() {
        return execution == null ? null : execution.getProcessName();
    }

    public String getInitiatedBy() {
        return execution == null ? null : execution.getInitiatedBy();
    }

    public boolean isDryRun() {
        return execution != null && execution.isDryRun();
    }

    public String getStartedAtText() {
        return startedAtText;
    }

    public String getFinishedAtText() {
        return finishedAtText;
    }

    public String getDurationText() {
        return durationText;
    }

    public boolean isSuccess() {
        return result != null && result.isSuccess();
    }

    public String getMessage() {
        return result == null ? null : result.getMessage();
    }

    public int getIssueCount() {
        return result == null ? 0 : result.getIssueCount();
    }

    public boolean hasIssues() {
        return getIssueCount() > 0;
    }

    public String getSummaryState() {
        if (result == null) {
            return "SIN_DATOS";
        }
        return result.isSuccess() ? "OK" : "CON_ERRORES";
    }

    private static Map<String, Integer> buildIssueCountsByCode(List<SyncIssue> issues) {
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        for (SyncIssue issue : issues) {
            String key = normalizeCountKey(issue == null ? null : issue.getCode());
            Integer current = counts.get(key);
            counts.put(key, current == null ? 1 : current + 1);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(counts));
    }

    private static String normalizeCountKey(String value) {
        if (value == null) {
            return "UNCATEGORIZED";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "UNCATEGORIZED" : trimmed;
    }

    private static String formatInstant(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "-";
        }
        ZoneId safeZoneId = zoneId == null ? ZoneId.of("UTC") : zoneId;
        return DATE_TIME_FORMATTER.format(instant.atZone(safeZoneId));
    }

    private static String calculateDuration(Instant start, Instant end) {
        if (start == null || end == null) {
            return "-";
        }
        Duration duration = Duration.between(start, end);
        long millis = duration.toMillis();
        if (millis < 0) {
            return "-";
        }
        long seconds = millis / 1000L;
        long remainingMillis = millis % 1000L;
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%dm %02ds %03dms", minutes, remainingSeconds, remainingMillis);
        }
        if (seconds > 0) {
            return String.format(Locale.ROOT, "%ds %03dms", seconds, remainingMillis);
        }
        return String.format(Locale.ROOT, "%dms", millis);
    }
}
