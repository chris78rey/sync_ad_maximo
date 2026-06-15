package com.syncadmaximo.repository.oracle;

import com.syncadmaximo.audit.AccessAuditRecord;
import com.syncadmaximo.audit.AuditRecord;
import com.syncadmaximo.audit.MailAuditRecord;
import com.syncadmaximo.audit.RunRecord;
import com.syncadmaximo.audit.ExecutionAuditDao;
import com.syncadmaximo.repository.AuditRepository;
import com.syncadmaximo.sql.OracleSql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAuditRepository implements AuditRepository, ExecutionAuditDao {
    private final OracleConnectionFactory connectionFactory;

    public JdbcAuditRepository(OracleConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void saveExecutionStart(com.syncadmaximo.model.SyncExecution execution) {
        if (execution == null) {
            return;
        }
        RunRecord runRecord = new RunRecord();
        runRecord.setRunId(resolveRunId(execution.getRunId()));
        runRecord.setFechaInicio(execution.getStartedAt());
        runRecord.setFechaFin(execution.getFinishedAt());
        runRecord.setModo(execution.isDryRun() ? "DRY_RUN" : "PRODUCTION");
        runRecord.setProceso(execution.getProcessName());
        runRecord.setUsuarioEjecutor(execution.getInitiatedBy());
        runRecord.setOrigenEjecucion("WEB_OR_SCHEDULER");
        runRecord.setEstado("STARTED");
        try {
            if (runRecord.getRunId() == null) {
                insertRun(runRecord);
            } else {
                updateRun(runRecord);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar el inicio de ejecución", ex);
        }
    }

    @Override
    public void saveExecutionEnd(com.syncadmaximo.model.SyncExecution execution) {
        if (execution == null) {
            return;
        }
        RunRecord runRecord = new RunRecord();
        runRecord.setRunId(resolveRunId(execution.getRunId()));
        runRecord.setFechaInicio(execution.getStartedAt());
        runRecord.setFechaFin(execution.getFinishedAt());
        runRecord.setModo(execution.isDryRun() ? "DRY_RUN" : "PRODUCTION");
        runRecord.setProceso(execution.getProcessName());
        runRecord.setUsuarioEjecutor(execution.getInitiatedBy());
        runRecord.setOrigenEjecucion("WEB_OR_SCHEDULER");
        runRecord.setEstado("FINISHED");
        try {
            updateRun(runRecord);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar el fin de ejecución", ex);
        }
    }

    @Override
    public void saveIssue(String runId, com.syncadmaximo.model.SyncIssue issue) {
        if (issue == null) {
            return;
        }
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setRunId(resolveRunId(runId));
        auditRecord.setFechaEvento(java.time.Instant.now());
        auditRecord.setEstado(issue.getCode());
        auditRecord.setMensaje(issue.getDescription());
        auditRecord.setCedula(issue.getReferenceId());
        try {
            insertAudit(auditRecord);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo registrar el issue", ex);
        }
    }

    @Override
    public List<com.syncadmaximo.model.SyncIssue> findIssuesByRunId(String runId) {
        String sql = "SELECT FECHA_EVENTO, ESTADO, MENSAJE, CEDULA FROM "
                + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.AUDIT_TABLE)
                + " WHERE RUN_ID = ? ORDER BY FECHA_EVENTO";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Long resolvedRunId = resolveRunId(runId);
            if (resolvedRunId == null) {
                return new ArrayList<>();
            }
            statement.setLong(1, resolvedRunId);
            try (ResultSet rs = statement.executeQuery()) {
                List<com.syncadmaximo.model.SyncIssue> result = new ArrayList<>();
                while (rs.next()) {
                    com.syncadmaximo.model.SyncIssue issue = new com.syncadmaximo.model.SyncIssue();
                    issue.setCreatedAt(OracleSql.readInstant(rs, "FECHA_EVENTO"));
                    issue.setCode(rs.getString("ESTADO"));
                    issue.setDescription(rs.getString("MENSAJE"));
                    issue.setReferenceId(rs.getString("CEDULA"));
                    result.add(issue);
                }
                return result;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron leer issues por runId", ex);
        }
    }

    @Override
    public long insertRun(RunRecord runRecord) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return insertRun(connection, runRecord);
        }
    }

    public long insertRun(Connection connection, RunRecord runRecord) throws SQLException {
        if (runRecord == null) {
            throw new IllegalArgumentException("runRecord es obligatorio");
        }
        long runId = runRecord.getRunId() != null ? runRecord.getRunId() : nextSequenceValue(connection, connectionFactory.getRunSequenceName());
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.insertRun(connectionFactory.getSchema()))) {
            statement.setLong(1, runId);
            OracleSql.bindInstant(statement, 2, runRecord.getFechaInicio());
            OracleSql.bindInstant(statement, 3, runRecord.getFechaFin());
            OracleSql.bindNullableString(statement, 4, runRecord.getModo());
            OracleSql.bindNullableString(statement, 5, runRecord.getProceso());
            OracleSql.bindNullableString(statement, 6, runRecord.getUsuarioEjecutor());
            OracleSql.bindNullableString(statement, 7, runRecord.getOrigenEjecucion());
            OracleSql.bindNullableString(statement, 8, runRecord.getEstado());
            statement.setInt(9, runRecord.getTotalMaximo());
            statement.setInt(10, runRecord.getTotalAd());
            statement.setInt(11, runRecord.getTotalMigrados());
            statement.setInt(12, runRecord.getTotalCreados());
            statement.setInt(13, runRecord.getTotalInactivados());
            statement.setInt(14, runRecord.getTotalEmailActualizados());
            statement.setInt(15, runRecord.getTotalEmailInsertados());
            statement.setInt(16, runRecord.getTotalSinCambios());
            statement.setInt(17, runRecord.getTotalObservados());
            statement.setInt(18, runRecord.getTotalErrores());
            OracleSql.bindNullableString(statement, 19, runRecord.getMensaje());
            statement.executeUpdate();
        }
        runRecord.setRunId(runId);
        return runId;
    }

    @Override
    public int updateRun(RunRecord runRecord) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return updateRun(connection, runRecord);
        }
    }

    public int updateRun(Connection connection, RunRecord runRecord) throws SQLException {
        if (runRecord == null || runRecord.getRunId() == null) {
            throw new IllegalArgumentException("runRecord y runId son obligatorios");
        }
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.updateRun(connectionFactory.getSchema()))) {
            OracleSql.bindInstant(statement, 1, runRecord.getFechaInicio());
            OracleSql.bindInstant(statement, 2, runRecord.getFechaFin());
            OracleSql.bindNullableString(statement, 3, runRecord.getModo());
            OracleSql.bindNullableString(statement, 4, runRecord.getProceso());
            OracleSql.bindNullableString(statement, 5, runRecord.getUsuarioEjecutor());
            OracleSql.bindNullableString(statement, 6, runRecord.getOrigenEjecucion());
            OracleSql.bindNullableString(statement, 7, runRecord.getEstado());
            statement.setInt(8, runRecord.getTotalMaximo());
            statement.setInt(9, runRecord.getTotalAd());
            statement.setInt(10, runRecord.getTotalMigrados());
            statement.setInt(11, runRecord.getTotalCreados());
            statement.setInt(12, runRecord.getTotalInactivados());
            statement.setInt(13, runRecord.getTotalEmailActualizados());
            statement.setInt(14, runRecord.getTotalEmailInsertados());
            statement.setInt(15, runRecord.getTotalSinCambios());
            statement.setInt(16, runRecord.getTotalObservados());
            statement.setInt(17, runRecord.getTotalErrores());
            OracleSql.bindNullableString(statement, 18, runRecord.getMensaje());
            statement.setLong(19, runRecord.getRunId());
            return statement.executeUpdate();
        }
    }

    @Override
    public long insertAudit(AuditRecord auditRecord) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return insertAudit(connection, auditRecord);
        }
    }

    public long insertAudit(Connection connection, AuditRecord auditRecord) throws SQLException {
        if (auditRecord == null) {
            throw new IllegalArgumentException("auditRecord es obligatorio");
        }
        long id = auditRecord.getId() != null ? auditRecord.getId() : nextSequenceValue(connection, connectionFactory.getAuditSequenceName());
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.insertAudit(connectionFactory.getSchema()))) {
            statement.setLong(1, id);
            if (auditRecord.getRunId() == null) {
                statement.setNull(2, java.sql.Types.NUMERIC);
            } else {
                statement.setLong(2, auditRecord.getRunId());
            }
            OracleSql.bindInstant(statement, 3, auditRecord.getFechaEvento());
            OracleSql.bindNullableString(statement, 4, auditRecord.getModo());
            OracleSql.bindNullableString(statement, 5, auditRecord.getProceso());
            OracleSql.bindNullableString(statement, 6, auditRecord.getCedula());
            OracleSql.bindNullableString(statement, 7, auditRecord.getPersonIdMaximoAnterior());
            OracleSql.bindNullableString(statement, 8, auditRecord.getPersonIdMaximoNuevo());
            OracleSql.bindNullableString(statement, 9, auditRecord.getPersonIdAd());
            OracleSql.bindNullableString(statement, 10, auditRecord.getEmailAnterior());
            OracleSql.bindNullableString(statement, 11, auditRecord.getEmailNuevo());
            OracleSql.bindNullableString(statement, 12, auditRecord.getEmailAd());
            OracleSql.bindNullableString(statement, 13, auditRecord.getEstado());
            OracleSql.bindNullableString(statement, 14, auditRecord.getMensaje());
            OracleSql.bindNullableString(statement, 15, auditRecord.getDetalleError());
            statement.executeUpdate();
        }
        auditRecord.setId(id);
        return id;
    }

    @Override
    public long insertAccessAudit(AccessAuditRecord accessAuditRecord) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return insertAccessAudit(connection, accessAuditRecord);
        }
    }

    public long insertAccessAudit(Connection connection, AccessAuditRecord accessAuditRecord) throws SQLException {
        if (accessAuditRecord == null) {
            throw new IllegalArgumentException("accessAuditRecord es obligatorio");
        }
        long id = accessAuditRecord.getId() != null ? accessAuditRecord.getId() : nextSequenceValue(connection, connectionFactory.getAccessAuditSequenceName());
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.insertAccessAudit(connectionFactory.getSchema()))) {
            statement.setLong(1, id);
            OracleSql.bindInstant(statement, 2, accessAuditRecord.getFechaEvento());
            OracleSql.bindNullableString(statement, 3, accessAuditRecord.getUsuario());
            OracleSql.bindNullableString(statement, 4, accessAuditRecord.getIpOrigen());
            OracleSql.bindNullableString(statement, 5, accessAuditRecord.getAccion());
            OracleSql.bindNullableString(statement, 6, accessAuditRecord.getEstado());
            OracleSql.bindNullableString(statement, 7, accessAuditRecord.getMensaje());
            statement.executeUpdate();
        }
        accessAuditRecord.setId(id);
        return id;
    }

    @Override
    public long insertMailAudit(MailAuditRecord mailAuditRecord) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return insertMailAudit(connection, mailAuditRecord);
        }
    }

    public long insertMailAudit(Connection connection, MailAuditRecord mailAuditRecord) throws SQLException {
        if (mailAuditRecord == null) {
            throw new IllegalArgumentException("mailAuditRecord es obligatorio");
        }
        long id = mailAuditRecord.getId() != null ? mailAuditRecord.getId() : nextSequenceValue(connection, connectionFactory.getMailAuditSequenceName());
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.insertMailAudit(connectionFactory.getSchema()))) {
            statement.setLong(1, id);
            if (mailAuditRecord.getRunId() == null) {
                statement.setNull(2, java.sql.Types.NUMERIC);
            } else {
                statement.setLong(2, mailAuditRecord.getRunId());
            }
            OracleSql.bindInstant(statement, 3, mailAuditRecord.getFechaEnvio());
            OracleSql.bindNullableString(statement, 4, mailAuditRecord.getDestinatarios());
            OracleSql.bindNullableString(statement, 5, mailAuditRecord.getCopias());
            OracleSql.bindNullableString(statement, 6, mailAuditRecord.getAsunto());
            OracleSql.bindNullableString(statement, 7, mailAuditRecord.getEstado());
            OracleSql.bindNullableString(statement, 8, mailAuditRecord.getMensaje());
            OracleSql.bindNullableString(statement, 9, mailAuditRecord.getDetalleError());
            statement.executeUpdate();
        }
        mailAuditRecord.setId(id);
        return id;
    }

    @Override
    public Optional<RunRecord> findRunById(long runId) throws SQLException {
        String sql = "SELECT RUN_ID, FECHA_INICIO, FECHA_FIN, MODO, PROCESO, USUARIO_EJECUTOR, ORIGEN_EJECUCION, ESTADO, "
                + "TOTAL_MAXIMO, TOTAL_AD, TOTAL_MIGRADOS, TOTAL_CREADOS, TOTAL_INACTIVADOS, TOTAL_EMAIL_ACTUALIZADOS, "
                + "TOTAL_EMAIL_INSERTADOS, TOTAL_SIN_CAMBIOS, TOTAL_OBSERVADOS, TOTAL_ERRORES, MENSAJE "
                + "FROM " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.RUN_TABLE) + " WHERE RUN_ID = ?";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, runId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(OracleSql.mapRun(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<RunRecord> findRuns(int limit) throws SQLException {
        String sql = "SELECT RUN_ID, FECHA_INICIO, FECHA_FIN, MODO, PROCESO, USUARIO_EJECUTOR, ORIGEN_EJECUCION, ESTADO, "
                + "TOTAL_MAXIMO, TOTAL_AD, TOTAL_MIGRADOS, TOTAL_CREADOS, TOTAL_INACTIVADOS, TOTAL_EMAIL_ACTUALIZADOS, "
                + "TOTAL_EMAIL_INSERTADOS, TOTAL_SIN_CAMBIOS, TOTAL_OBSERVADOS, TOTAL_ERRORES, MENSAJE "
                + "FROM " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.RUN_TABLE) + " ORDER BY FECHA_INICIO DESC FETCH FIRST ? ROWS ONLY";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                List<RunRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(OracleSql.mapRun(rs));
                }
                return result;
            }
        }
    }

    @Override
    public List<AuditRecord> findAuditsByRunId(long runId) throws SQLException {
        String sql = "SELECT ID, RUN_ID, FECHA_EVENTO, MODO, PROCESO, CEDULA, PERSONID_MAXIMO_ANTERIOR, PERSONID_MAXIMO_NUEVO, "
                + "PERSONID_AD, EMAIL_ANTERIOR, EMAIL_NUEVO, EMAIL_AD, ESTADO, MENSAJE, DETALLE_ERROR "
                + "FROM " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.AUDIT_TABLE) + " WHERE RUN_ID = ? ORDER BY FECHA_EVENTO";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, runId);
            try (ResultSet rs = statement.executeQuery()) {
                List<AuditRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(OracleSql.mapAudit(rs));
                }
                return result;
            }
        }
    }

    @Override
    public List<AccessAuditRecord> findAccessAudits(int limit) throws SQLException {
        String sql = "SELECT ID, FECHA_EVENTO, USUARIO, IP_ORIGEN, ACCION, ESTADO, MENSAJE FROM "
                + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.ACCESS_AUDIT_TABLE)
                + " ORDER BY FECHA_EVENTO DESC FETCH FIRST ? ROWS ONLY";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                List<AccessAuditRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(OracleSql.mapAccessAudit(rs));
                }
                return result;
            }
        }
    }

    @Override
    public List<MailAuditRecord> findMailAuditsByRunId(long runId) throws SQLException {
        String sql = "SELECT ID, RUN_ID, FECHA_ENVIO, DESTINATARIOS, COPIAS, ASUNTO, ESTADO, MENSAJE, DETALLE_ERROR FROM "
                + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.MAIL_AUDIT_TABLE)
                + " WHERE RUN_ID = ? ORDER BY FECHA_ENVIO";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, runId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MailAuditRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(OracleSql.mapMailAudit(rs));
                }
                return result;
            }
        }
    }

    public long nextSequenceValue(Connection connection, String sequenceName) throws SQLException {
        if (sequenceName == null || sequenceName.trim().isEmpty()) {
            throw new IllegalStateException("La secuencia no está configurada");
        }
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.nextSequenceValueSql(sequenceName));
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("No se pudo leer NEXTVAL de " + sequenceName);
            }
            return resultSet.getLong(1);
        }
    }

    private Long resolveRunId(String runId) {
        if (runId == null || runId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(runId.trim());
        } catch (NumberFormatException ex) {
            return Math.abs((long) runId.trim().hashCode());
        }
    }
}
