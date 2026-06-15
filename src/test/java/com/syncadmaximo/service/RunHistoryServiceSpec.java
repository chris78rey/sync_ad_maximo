package com.syncadmaximo.service;

import com.syncadmaximo.audit.AccessAuditRecord;
import com.syncadmaximo.audit.AuditRecord;
import com.syncadmaximo.audit.ExecutionAuditDao;
import com.syncadmaximo.audit.MailAuditRecord;
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
        listsRecentRunsFromDao();
    }

    static void resolvesHistoricalExecutionFromDao() {
        RecordingExecutionAuditDao dao = new RecordingExecutionAuditDao();
        RunHistoryService service = new RunHistoryService(dao);

        Optional<RunHistoryService.HistoricalExecution> historical = service.findExecutionByRunId("105");

        TestAssertions.isTrue(historical.isPresent(), "Debe resolver una ejecucion historica");
        TestAssertions.equals("105", historical.get().getExecution().getRunId(), "Debe conservar el runId");
        TestAssertions.equals("jdoe", historical.get().getExecution().getInitiatedBy(), "Debe recuperar el ejecutor");
        TestAssertions.equals(1, historical.get().getResult().getIssues().size(), "Debe mapear los issues");
        TestAssertions.equals("VALIDATION_REVIEW", historical.get().getResult().getIssues().get(0).getCode(), "Debe mapear el codigo");
        TestAssertions.equals(1, dao.findRunCalls, "Debe consultar el run historico");
        TestAssertions.equals(1, dao.findAuditCalls, "Debe consultar el detalle");
    }

    static void listsRecentRunsFromDao() {
        RecordingExecutionAuditDao dao = new RecordingExecutionAuditDao();
        RunHistoryService service = new RunHistoryService(dao);

        List<RunRecord> runs = service.findRecentRuns(10);

        TestAssertions.equals(1, dao.findRunsCalls, "Debe consultar el listado de ejecuciones");
        TestAssertions.equals(1, runs.size(), "Debe retornar la ejecucion reciente");
        TestAssertions.equals(Long.valueOf(105L), runs.get(0).getRunId(), "Debe conservar el runId del listado");
    }

    private static final class RecordingExecutionAuditDao implements ExecutionAuditDao {
        private int findRunCalls;
        private int findAuditCalls;
        private int findRunsCalls;

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
        public long insertAccessAudit(AccessAuditRecord accessAuditRecord) {
            return 0;
        }

        @Override
        public long insertMailAudit(MailAuditRecord mailAuditRecord) {
            return 0;
        }

        @Override
        public Optional<RunRecord> findRunById(long runId) {
            findRunCalls++;
            return Optional.of(sampleRunRecord(runId));
        }

        @Override
        public List<RunRecord> findRuns(int limit) {
            findRunsCalls++;
            List<RunRecord> runs = new ArrayList<>();
            runs.add(sampleRunRecord(105L));
            return runs;
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
        public List<AccessAuditRecord> findAccessAudits(int limit) {
            return new ArrayList<>();
        }

        @Override
        public List<MailAuditRecord> findMailAuditsByRunId(long runId) {
            return new ArrayList<>();
        }

        private RunRecord sampleRunRecord(long runId) {
            RunRecord record = new RunRecord();
            record.setRunId(runId);
            record.setFechaInicio(Instant.parse("2026-01-01T10:00:00Z"));
            record.setFechaFin(Instant.parse("2026-01-01T10:05:00Z"));
            record.setModo("PRODUCTION");
            record.setProceso("sync-ad-maximo");
            record.setUsuarioEjecutor("jdoe");
            record.setOrigenEjecucion("MANUAL");
            record.setEstado("FINISHED");
            record.setMensaje("Ejecucion reciente");
            return record;
        }
    }
}
