package com.syncadmaximo.service;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;

import java.util.List;

public interface AuditRepository {

    void saveExecutionStart(SyncExecution execution);

    void saveExecutionEnd(SyncExecution execution);

    void saveIssue(String runId, SyncIssue issue);

    List<SyncIssue> findIssuesByRunId(String runId);
}
