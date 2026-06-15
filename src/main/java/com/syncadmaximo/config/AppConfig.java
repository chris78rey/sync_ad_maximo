package com.syncadmaximo.config;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig(PropertyLoader.load());

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(Objects.requireNonNull(properties, "properties"));
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public Properties asProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    public String getAppName() {
        return getString("app.name", "sync-ad-maximo");
    }

    public ZoneId getZoneId() {
        return ZoneId.of(getString("app.timezone", "UTC"));
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(getString("app.locale", "es-EC").replace('_', '-'));
    }

    public String getAllowedUser() {
        return getString("sync.web.allowedUser", "maxadmin");
    }

    public String getCedula9DigitsStrategy() {
        return getString("sync.strategy.cedula9Digitos", "AGREGAR_CERO");
    }

    public String getOracleUrl() {
        return getString("oracle.url", getString("jdbcUrl", null));
    }

    public String getOracleUsername() {
        return getString("oracle.username", getString("username", null));
    }

    public String getOraclePassword() {
        return getString("oracle.password", getString("password", null));
    }

    public String getOracleSchema() {
        return getString("oracle.schema", getString("schema", "MAXIMO"));
    }

    public String getOracleDriverClassName() {
        return getString("oracle.driverClassName", getString("driverClassName", null));
    }

    public String getOracleRunSequence() {
        return getString("oracle.runSequenceName", getString("runSequenceName", null));
    }

    public String getOracleAuditSequence() {
        return getString("oracle.auditSequenceName", getString("auditSequenceName", null));
    }

    public String getOracleAccessAuditSequence() {
        return getString("oracle.accessAuditSequenceName", getString("accessAuditSequenceName", null));
    }

    public String getOracleMailAuditSequence() {
        return getString("oracle.mailAuditSequenceName", getString("mailAuditSequenceName", null));
    }

    public String getOracleEmailIdSequence() {
        return getString("oracle.emailIdSequenceName", getString("emailIdSequenceName", null));
    }

    public String getOracleRowstampSequence() {
        return getString("oracle.rowstampSequenceName", getString("rowstampSequenceName", null));
    }

    public String getLdapUrl() {
        return getString("ldap.url", null);
    }

    public String getLdapBindUser() {
        return getString("ldap.bindUser", null);
    }

    public String getLdapBindPassword() {
        return getString("ldap.bindPassword", null);
    }

    public String getLdapBaseDn() {
        return getString("ldap.baseDn", null);
    }

    public String getLdapEnabledFilter() {
        return getString("ad.enabledFilterBase", getString("ldap.enabledFilter", null));
    }

    public String getLdapDisabledFilter() {
        return getString("ad.disabledFilterBase", getString("ldap.disabledFilter", null));
    }

    public boolean isMailEnabled() {
        return getBoolean("mail.enabled", false);
    }

    public String getMailHost() {
        return getString("mail.host", null);
    }

    public int getMailPort() {
        return getInt("mail.port", 25);
    }

    public String getMailUsername() {
        return getString("mail.username", null);
    }

    public String getMailPassword() {
        return getString("mail.password", null);
    }

    public boolean isMailStartTlsEnabled() {
        return getBoolean("mail.starttls", false);
    }

    public String getMailFrom() {
        return getString("mail.from", null);
    }

    public String getMailSubjectPrefix() {
        return getString("mail.subjectPrefix", "[SYNC AD MAXIMO]");
    }

    public List<String> getMailRecipients(String key) {
        String raw = getString("mail.recipients." + key, null);
        if (raw == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isSchedulerEnabled() {
        return getBoolean("scheduler.habilitado", getBoolean("sync.scheduler.enabled", false));
    }

    public String getSchedulerTime() {
        return getString("scheduler.horaEjecucion", getString("sync.scheduler.horaEjecucion", null));
    }

    public String getSchedulerMode() {
        return getString("scheduler.modo", getString("sync.execution.mode", "DRY_RUN"));
    }

    public String getSchedulerProcess() {
        return getString("scheduler.proceso", getString("sync.scheduler.process", getAppName()));
    }

    public List<String> getAllowedUsers() {
        String raw = getString("security.allowedUsers", getAllowedUser());
        if (raw == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    public String getString(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
