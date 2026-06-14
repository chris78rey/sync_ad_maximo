package com.syncadmaximo.sql;

import com.syncadmaximo.audit.AccessAuditRecord;
import com.syncadmaximo.audit.AuditRecord;
import com.syncadmaximo.audit.MailAuditRecord;
import com.syncadmaximo.audit.RunRecord;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public final class OracleSql {
    public static final String DEFAULT_SCHEMA = "MAXIMO";
    public static final String PERSON_TABLE = "PERSON";
    public static final String EMAIL_TABLE = "EMAIL";
    public static final String RUN_TABLE = "SYNC_AD_MAXIMO_RUN";
    public static final String AUDIT_TABLE = "SYNC_AD_MAXIMO_AUDIT";
    public static final String ACCESS_AUDIT_TABLE = "SYNC_AD_MAXIMO_ACCESS_AUDIT";
    public static final String MAIL_AUDIT_TABLE = "SYNC_AD_MAXIMO_MAIL_AUDIT";

    private OracleSql() {
    }

    public static String qualifiedTable(String schema, String tableName) {
        String normalizedSchema = normalizeSchema(schema);
        return normalizedSchema + "." + tableName;
    }

    public static String normalizeSchema(String schema) {
        if (schema == null || schema.trim().isEmpty()) {
            return DEFAULT_SCHEMA;
        }
        return schema.trim().toUpperCase();
    }

    public static String selectPersonByCedula(String schema) {
        return "SELECT PERSONID, STATUS, FIRSTNAME, LASTNAME, EPP_CEDULA, EPP_NUM_ROL "
                + "FROM " + qualifiedTable(schema, PERSON_TABLE) + " "
                + "WHERE EPP_CEDULA = ? AND STATUS IN ('ACTIVO', 'INACTIVO')";
    }

    public static String selectPersonByPersonId(String schema) {
        return "SELECT PERSONID, STATUS, FIRSTNAME, LASTNAME, EPP_CEDULA, EPP_NUM_ROL "
                + "FROM " + qualifiedTable(schema, PERSON_TABLE) + " WHERE PERSONID = ?";
    }

    public static String selectPrimaryEmailByPersonId(String schema) {
        return "SELECT EMAILID, PERSONID, EMAILADDRESS, TYPE, ISPRIMARY, ROWSTAMP "
                + "FROM " + qualifiedTable(schema, EMAIL_TABLE) + " WHERE PERSONID = ? AND ISPRIMARY = 1";
    }

    public static String emailBelongsToOtherPerson(String schema) {
        return "SELECT COUNT(1) FROM " + qualifiedTable(schema, EMAIL_TABLE) + " WHERE LOWER(TRIM(EMAILADDRESS)) = LOWER(TRIM(?)) AND PERSONID <> ?";
    }

    public static String updatePersonIdByCedula(String schema) {
        return "UPDATE " + qualifiedTable(schema, PERSON_TABLE) + " SET PERSONID = ? WHERE EPP_CEDULA = ? AND PERSONID = ?";
    }

    public static String updatePrimaryEmail(String schema) {
        return "UPDATE " + qualifiedTable(schema, EMAIL_TABLE) + " SET EMAILADDRESS = ? WHERE PERSONID = ? AND ISPRIMARY = 1";
    }

    public static String insertPrimaryEmail(String schema) {
        return "INSERT INTO " + qualifiedTable(schema, EMAIL_TABLE)
                + " (EMAILID, PERSONID, EMAILADDRESS, TYPE, ISPRIMARY, ROWSTAMP) VALUES (?, ?, ?, ?, ?, ?)";
    }

    public static String nextSequenceValueSql(String sequenceName) {
        return "SELECT " + sequenceName.trim() + ".NEXTVAL FROM DUAL";
    }

    public static String insertRun(String schema) {
        return "INSERT INTO " + qualifiedTable(schema, RUN_TABLE)
                + " (RUN_ID, FECHA_INICIO, FECHA_FIN, MODO, PROCESO, USUARIO_EJECUTOR, ORIGEN_EJECUCION, ESTADO,"
                + " TOTAL_MAXIMO, TOTAL_AD, TOTAL_MIGRADOS, TOTAL_CREADOS, TOTAL_INACTIVADOS, TOTAL_EMAIL_ACTUALIZADOS,"
                + " TOTAL_EMAIL_INSERTADOS, TOTAL_SIN_CAMBIOS, TOTAL_OBSERVADOS, TOTAL_ERRORES, MENSAJE)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String updateRun(String schema) {
        return "UPDATE " + qualifiedTable(schema, RUN_TABLE)
                + " SET FECHA_INICIO = ?, FECHA_FIN = ?, MODO = ?, PROCESO = ?, USUARIO_EJECUTOR = ?, ORIGEN_EJECUCION = ?,"
                + " ESTADO = ?, TOTAL_MAXIMO = ?, TOTAL_AD = ?, TOTAL_MIGRADOS = ?, TOTAL_CREADOS = ?, TOTAL_INACTIVADOS = ?,"
                + " TOTAL_EMAIL_ACTUALIZADOS = ?, TOTAL_EMAIL_INSERTADOS = ?, TOTAL_SIN_CAMBIOS = ?, TOTAL_OBSERVADOS = ?,"
                + " TOTAL_ERRORES = ?, MENSAJE = ? WHERE RUN_ID = ?";
    }

    public static String insertAudit(String schema) {
        return "INSERT INTO " + qualifiedTable(schema, AUDIT_TABLE)
                + " (ID, RUN_ID, FECHA_EVENTO, MODO, PROCESO, CEDULA, PERSONID_MAXIMO_ANTERIOR, PERSONID_MAXIMO_NUEVO,"
                + " PERSONID_AD, EMAIL_ANTERIOR, EMAIL_NUEVO, EMAIL_AD, ESTADO, MENSAJE, DETALLE_ERROR)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static String insertAccessAudit(String schema) {
        return "INSERT INTO " + qualifiedTable(schema, ACCESS_AUDIT_TABLE)
                + " (ID, FECHA_EVENTO, USUARIO, IP_ORIGEN, ACCION, ESTADO, MENSAJE) VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    public static String insertMailAudit(String schema) {
        return "INSERT INTO " + qualifiedTable(schema, MAIL_AUDIT_TABLE)
                + " (ID, RUN_ID, FECHA_ENVIO, DESTINATARIOS, COPIAS, ASUNTO, ESTADO, MENSAJE, DETALLE_ERROR)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    public static void bindInstant(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setTimestamp(index, null);
        } else {
            statement.setTimestamp(index, Timestamp.from(instant));
        }
    }

    public static Instant readInstant(ResultSet resultSet, String columnLabel) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnLabel);
        return timestamp == null ? null : timestamp.toInstant();
    }

    public static void bindNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    public static RunRecord mapRun(ResultSet rs) throws SQLException {
        RunRecord record = new RunRecord();
        record.setRunId(rs.getLong("RUN_ID"));
        record.setFechaInicio(readInstant(rs, "FECHA_INICIO"));
        record.setFechaFin(readInstant(rs, "FECHA_FIN"));
        record.setModo(rs.getString("MODO"));
        record.setProceso(rs.getString("PROCESO"));
        record.setUsuarioEjecutor(rs.getString("USUARIO_EJECUTOR"));
        record.setOrigenEjecucion(rs.getString("ORIGEN_EJECUCION"));
        record.setEstado(rs.getString("ESTADO"));
        record.setTotalMaximo(rs.getInt("TOTAL_MAXIMO"));
        record.setTotalAd(rs.getInt("TOTAL_AD"));
        record.setTotalMigrados(rs.getInt("TOTAL_MIGRADOS"));
        record.setTotalCreados(rs.getInt("TOTAL_CREADOS"));
        record.setTotalInactivados(rs.getInt("TOTAL_INACTIVADOS"));
        record.setTotalEmailActualizados(rs.getInt("TOTAL_EMAIL_ACTUALIZADOS"));
        record.setTotalEmailInsertados(rs.getInt("TOTAL_EMAIL_INSERTADOS"));
        record.setTotalSinCambios(rs.getInt("TOTAL_SIN_CAMBIOS"));
        record.setTotalObservados(rs.getInt("TOTAL_OBSERVADOS"));
        record.setTotalErrores(rs.getInt("TOTAL_ERRORES"));
        record.setMensaje(rs.getString("MENSAJE"));
        return record;
    }

    public static AuditRecord mapAudit(ResultSet rs) throws SQLException {
        AuditRecord record = new AuditRecord();
        record.setId(rs.getLong("ID"));
        record.setRunId(rs.getLong("RUN_ID"));
        record.setFechaEvento(readInstant(rs, "FECHA_EVENTO"));
        record.setModo(rs.getString("MODO"));
        record.setProceso(rs.getString("PROCESO"));
        record.setCedula(rs.getString("CEDULA"));
        record.setPersonIdMaximoAnterior(rs.getString("PERSONID_MAXIMO_ANTERIOR"));
        record.setPersonIdMaximoNuevo(rs.getString("PERSONID_MAXIMO_NUEVO"));
        record.setPersonIdAd(rs.getString("PERSONID_AD"));
        record.setEmailAnterior(rs.getString("EMAIL_ANTERIOR"));
        record.setEmailNuevo(rs.getString("EMAIL_NUEVO"));
        record.setEmailAd(rs.getString("EMAIL_AD"));
        record.setEstado(rs.getString("ESTADO"));
        record.setMensaje(rs.getString("MENSAJE"));
        record.setDetalleError(rs.getString("DETALLE_ERROR"));
        return record;
    }

    public static AccessAuditRecord mapAccessAudit(ResultSet rs) throws SQLException {
        AccessAuditRecord record = new AccessAuditRecord();
        record.setId(rs.getLong("ID"));
        record.setFechaEvento(readInstant(rs, "FECHA_EVENTO"));
        record.setUsuario(rs.getString("USUARIO"));
        record.setIpOrigen(rs.getString("IP_ORIGEN"));
        record.setAccion(rs.getString("ACCION"));
        record.setEstado(rs.getString("ESTADO"));
        record.setMensaje(rs.getString("MENSAJE"));
        return record;
    }

    public static MailAuditRecord mapMailAudit(ResultSet rs) throws SQLException {
        MailAuditRecord record = new MailAuditRecord();
        record.setId(rs.getLong("ID"));
        record.setRunId(rs.getLong("RUN_ID"));
        record.setFechaEnvio(readInstant(rs, "FECHA_ENVIO"));
        record.setDestinatarios(rs.getString("DESTINATARIOS"));
        record.setCopias(rs.getString("COPIAS"));
        record.setAsunto(rs.getString("ASUNTO"));
        record.setEstado(rs.getString("ESTADO"));
        record.setMensaje(rs.getString("MENSAJE"));
        record.setDetalleError(rs.getString("DETALLE_ERROR"));
        return record;
    }
}
