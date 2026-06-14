package com.syncadmaximo.repository.oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class OracleConnectionFactory {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final String schema;
    private final String runSequenceName;
    private final String auditSequenceName;
    private final String accessAuditSequenceName;
    private final String mailAuditSequenceName;
    private final String emailIdSequenceName;
    private final String rowstampSequenceName;

    public OracleConnectionFactory(String jdbcUrl,
                                   String username,
                                   String password,
                                   String driverClassName,
                                   String schema,
                                   String runSequenceName,
                                   String auditSequenceName,
                                   String accessAuditSequenceName,
                                   String mailAuditSequenceName,
                                   String emailIdSequenceName,
                                   String rowstampSequenceName) {
        this.jdbcUrl = requireText(jdbcUrl, "jdbcUrl");
        this.username = requireText(username, "username");
        this.password = password == null ? "" : password;
        this.driverClassName = driverClassName == null || driverClassName.trim().isEmpty() ? null : driverClassName.trim();
        this.schema = normalizeSchema(schema);
        this.runSequenceName = normalizeOptionalName(runSequenceName);
        this.auditSequenceName = normalizeOptionalName(auditSequenceName);
        this.accessAuditSequenceName = normalizeOptionalName(accessAuditSequenceName);
        this.mailAuditSequenceName = normalizeOptionalName(mailAuditSequenceName);
        this.emailIdSequenceName = normalizeOptionalName(emailIdSequenceName);
        this.rowstampSequenceName = normalizeOptionalName(rowstampSequenceName);
    }

    public static OracleConnectionFactory fromEnvironment() {
        return new OracleConnectionFactory(
                requireText(System.getenv("SYNC_AD_MAXIMO_JDBC_URL"), "SYNC_AD_MAXIMO_JDBC_URL"),
                requireText(System.getenv("SYNC_AD_MAXIMO_JDBC_USER"), "SYNC_AD_MAXIMO_JDBC_USER"),
                System.getenv("SYNC_AD_MAXIMO_JDBC_PASSWORD"),
                System.getenv("SYNC_AD_MAXIMO_JDBC_DRIVER"),
                System.getenv("SYNC_AD_MAXIMO_SCHEMA"),
                System.getenv("SYNC_AD_MAXIMO_RUN_SEQ"),
                System.getenv("SYNC_AD_MAXIMO_AUDIT_SEQ"),
                System.getenv("SYNC_AD_MAXIMO_ACCESS_AUDIT_SEQ"),
                System.getenv("SYNC_AD_MAXIMO_MAIL_AUDIT_SEQ"),
                System.getenv("SYNC_AD_MAXIMO_EMAIL_SEQ"),
                System.getenv("SYNC_AD_MAXIMO_ROWSTAMP_SEQ")
        );
    }

    public static OracleConnectionFactory fromProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        return new OracleConnectionFactory(
                requireText(properties.getProperty("jdbcUrl"), "jdbcUrl"),
                requireText(properties.getProperty("username"), "username"),
                properties.getProperty("password"),
                properties.getProperty("driverClassName"),
                properties.getProperty("schema"),
                properties.getProperty("runSequenceName"),
                properties.getProperty("auditSequenceName"),
                properties.getProperty("accessAuditSequenceName"),
                properties.getProperty("mailAuditSequenceName"),
                properties.getProperty("emailIdSequenceName"),
                properties.getProperty("rowstampSequenceName")
        );
    }

    public Connection openConnection() throws SQLException {
        try {
            if (driverClassName != null) {
                Class.forName(driverClassName);
            }
        } catch (ClassNotFoundException ex) {
            throw new SQLException("No se pudo cargar el driver JDBC Oracle: " + driverClassName, ex);
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public String getSchema() {
        return schema;
    }

    public String getRunSequenceName() {
        return runSequenceName;
    }

    public String getAuditSequenceName() {
        return auditSequenceName;
    }

    public String getAccessAuditSequenceName() {
        return accessAuditSequenceName;
    }

    public String getMailAuditSequenceName() {
        return mailAuditSequenceName;
    }

    public String getEmailIdSequenceName() {
        return emailIdSequenceName;
    }

    public String getRowstampSequenceName() {
        return rowstampSequenceName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        return value.trim();
    }

    private static String normalizeOptionalName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private static String normalizeSchema(String schema) {
        return schema == null || schema.trim().isEmpty() ? "MAXIMO" : schema.trim().toUpperCase();
    }
}
