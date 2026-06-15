package com.syncadmaximo.service;

import com.syncadmaximo.audit.AuditRecord;
import com.syncadmaximo.audit.ExecutionAuditDao;
import com.syncadmaximo.audit.RunRecord;
import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.repository.oracle.JdbcAuditRepository;
import com.syncadmaximo.repository.oracle.OracleConnectionFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Expone historial de ejecuciones y detalle desde Oracle.
 */
public class RunHistoryService {

    private final ExecutionAuditDao auditDao;

    public RunHistoryService(AppConfig config) {
        this(createDao(config));
    }

    public RunHistoryService(ExecutionAuditDao auditDao) {
        this.auditDao = auditDao;
    }

    public Optional<HistoricalExecution> findExecutionByRunId(String runId) {
        if (auditDao == null || runId == null || runId.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            long numericRunId = Long.parseLong(runId.trim());
            Optional<RunRecord> record = auditDao.findRunById(numericRunId);
            if (!record.isPresent()) {
                return Optional.empty();
            }
            List<AuditRecord> audits = safeList(auditDao.findAuditsByRunId(numericRunId));
            return Optional.of(new HistoricalExecution(toExecution(record.get()), toResult(record.get(), audits)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer el historial de ejecuciones", ex);
        }
    }

    public List<RunRecord> findRecentRuns(int limit) {
        if (auditDao == null) {
            return Collections.emptyList();
        }
        try {
            return safeList(auditDao.findRuns(limit));
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer el historial de ejecuciones", ex);
        }
    }

    private static ExecutionAuditDao createDao(AppConfig config) {
        if (config == null || config.getOracleUrl() == null || config.getOracleUsername() == null) {
            return null;
        }
        OracleConnectionFactory factory = new OracleConnectionFactory(
                config.getOracleUrl(),
                config.getOracleUsername(),
                config.getOraclePassword(),
                config.getOracleDriverClassName(),
                config.getOracleSchema(),
                config.getOracleRunSequence(),
                config.getOracleAuditSequence(),
                config.getOracleAccessAuditSequence(),
                config.getOracleMailAuditSequence(),
                config.getOracleEmailIdSequence(),
                config.getOracleRowstampSequence()
        );
        return new JdbcAuditRepository(factory);
    }

    private static SyncExecution toExecution(RunRecord runRecord) {
        SyncExecution execution = new SyncExecution();
        execution.setRunId(runRecord.getRunId() == null ? null : String.valueOf(runRecord.getRunId()));
        execution.setStartedAt(runRecord.getFechaInicio());
        execution.setFinishedAt(runRecord.getFechaFin());
        execution.setProcessName(runRecord.getProceso());
        execution.setInitiatedBy(runRecord.getUsuarioEjecutor());
        execution.setDryRun("DRY_RUN".equalsIgnoreCase(runRecord.getModo()));
        return execution;
    }

    private static SyncResult toResult(RunRecord runRecord, List<AuditRecord> audits) {
        SyncResult result = new SyncResult();
        result.setRunId(runRecord.getRunId() == null ? null : String.valueOf(runRecord.getRunId()));
        result.setSuccess("FINISHED".equalsIgnoreCase(runRecord.getEstado()));
        result.setMessage(runRecord.getMensaje());
        for (AuditRecord audit : audits) {
            SyncIssue issue = new SyncIssue();
            issue.setCreatedAt(audit.getFechaEvento());
            issue.setCode(audit.getEstado());
            issue.setDescription(audit.getMensaje());
            issue.setReferenceId(audit.getCedula());
            result.addIssue(issue);
        }
        return result;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    public static final class HistoricalExecution {
        private final SyncExecution execution;
        private final SyncResult result;

        private HistoricalExecution(SyncExecution execution, SyncResult result) {
            this.execution = execution;
            this.result = result;
        }

        public SyncExecution getExecution() {
            return execution;
        }

        public SyncResult getResult() {
            return result;
        }
    }
}
