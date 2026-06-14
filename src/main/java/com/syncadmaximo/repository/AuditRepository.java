package com.syncadmaximo.repository;

import com.syncadmaximo.audit.ExecutionAuditDao;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;

import java.util.List;

public interface AuditRepository extends com.syncadmaximo.service.AuditRepository, ExecutionAuditDao {
    @Override
    void saveExecutionStart(SyncExecution execution);

    @Override
    void saveExecutionEnd(SyncExecution execution);

    @Override
    void saveIssue(String runId, SyncIssue issue);

    @Override
    List<SyncIssue> findIssuesByRunId(String runId);
}
