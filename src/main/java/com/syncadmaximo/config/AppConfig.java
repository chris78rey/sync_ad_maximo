package com.syncadmaximo.config;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

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
