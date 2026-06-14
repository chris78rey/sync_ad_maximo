package com.syncadmaximo.audit;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ExecutionAuditDao {
    long insertRun(RunRecord runRecord) throws SQLException;

    int updateRun(RunRecord runRecord) throws SQLException;

    long insertAudit(AuditRecord auditRecord) throws SQLException;

    long insertAccessAudit(AccessAuditRecord accessAuditRecord) throws SQLException;

    long insertMailAudit(MailAuditRecord mailAuditRecord) throws SQLException;

    Optional<RunRecord> findRunById(long runId) throws SQLException;

    List<RunRecord> findRuns(int limit) throws SQLException;

    List<AuditRecord> findAuditsByRunId(long runId) throws SQLException;

    List<AccessAuditRecord> findAccessAudits(int limit) throws SQLException;

    List<MailAuditRecord> findMailAuditsByRunId(long runId) throws SQLException;
}
