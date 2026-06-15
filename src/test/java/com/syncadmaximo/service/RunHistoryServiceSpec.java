package com.syncadmaximo.service;

import com.syncadmaximo.audit.AuditRecord;
import com.syncadmaximo.audit.ExecutionAuditDao;
import com.syncadmaximo.audit.RunRecord;
import com.syncadmaximo.testsupport.TestAssertions;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RunHistoryServiceSpec {

    public static void runAll() {
        resolvesHistoricalExecutionFromDao();
    }

    static void resolvesHistoricalExecutionFromDao() {
        RecordingExecutionAuditDao dao = new RecordingExecutionAuditDao();
        RunHistoryService service = new RunHistoryService(dao);

        Optional<RunHistoryService.HistoricalExecution> historical = service.findExecutionByRunId("105");

        TestAssertions.isTrue(historical.isPresent(), "Debe resolver una ejecución histórica");
        TestAssertions.equals("105", historical.get().getExecution().getRunId(), "Debe conservar el runId");
        TestAssertions.equals("jdoe", historical.get().getExecution().getInitiatedBy(), "Debe recuperar el ejecutor");
        TestAssertions.equals(1, historical.get().getResult().getIssues().size(), "Debe mapear los issues");
        TestAssertions.equals("VALIDATION_REVIEW", historical.get().getResult().getIssues().get(0).getCode(), "Debe mapear el código");
        TestAssertions.equals(1, dao.findRunCalls, "Debe consultar el run histórico");
        TestAssertions.equals(1, dao.findAuditCalls, "Debe consultar el detalle");
    }

    private static final class RecordingExecutionAuditDao implements ExecutionAuditDao {
        private int findRunCalls;
        private int findAuditCalls;

        @Override
        public long insertRun(RunRecord runRecord) {
            return 0;
        }

        @Override
        public int updateRun(RunRecord runRecord) {
            return 0;
        }

        @Override
        public long insertAudit(AuditRecord auditRecord) {
            return 0;
        }

        @Override
        public long insertAccessAudit(com.syncadmaximo.audit.AccessAuditRecord accessAuditRecord) {
            return 0;
        }

        @Override
        public long insertMailAudit(com.syncadmaximo.audit.MailAuditRecord mailAuditRecord) {
            return 0;
        }

        @Override
        public Optional<RunRecord> findRunById(long runId) {
            findRunCalls++;
            RunRecord record = new RunRecord();
            record.setRunId(runId);
            record.setFechaInicio(Instant.parse("2026-01-01T10:00:00Z"));
            record.setFechaFin(Instant.parse("2026-01-01T10:05:00Z"));
            record.setModo("PRODUCTION");
            record.setProceso("sync-ad-maximo");
            record.setUsuarioEjecutor("jdoe");
            record.setOrigenEjecucion("MANUAL");
            record.setEstado("FINISHED");
            record.setMensaje("Ejecución histórica");
            return Optional.of(record);
        }

        @Override
        public List<RunRecord> findRuns(int limit) {
            return new ArrayList<>();
        }

        @Override
        public List<AuditRecord> findAuditsByRunId(long runId) {
            findAuditCalls++;
            AuditRecord record = new AuditRecord();
            record.setRunId(runId);
            record.setFechaEvento(Instant.parse("2026-01-01T10:01:00Z"));
            record.setEstado("VALIDATION_REVIEW");
            record.setMensaje("Revisar correo");
            record.setCedula("0123456789");
            return java.util.Collections.singletonList(record);
        }

        @Override
        public List<com.syncadmaximo.audit.AccessAuditRecord> findAccessAudits(int limit) {
            return new ArrayList<>();
        }

        @Override
        public List<com.syncadmaximo.audit.MailAuditRecord> findMailAuditsByRunId(long runId) {
            return new ArrayList<>();
        }
    }
}
