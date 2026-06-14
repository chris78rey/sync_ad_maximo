package com.syncadmaximo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncResult {

    private String runId;
    private boolean success;
    private String message;
    private final List<SyncIssue> issues = new ArrayList<>();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<SyncIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void addIssue(SyncIssue issue) {
        if (issue != null) {
            issues.add(issue);
        }
    }

    public void addIssue(String code, String description) {
        issues.add(new SyncIssue(code, description));
    }

    public int getIssueCount() {
        return issues.size();
    }
}
