package com.syncadmaximo.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public final class PropertyLoader {

    public static final String ENV_CONFIG_PATH = "SYNC_AD_MAXIMO_CONFIG";
    public static final String DEFAULT_RESOURCE = "application.properties";

    private PropertyLoader() {
    }

    public static Properties load() {
        String externalPath = trimToNull(System.getenv(ENV_CONFIG_PATH));
        if (externalPath != null) {
            return loadFromExternal(Paths.get(externalPath));
        }
        return loadFromClasspath(DEFAULT_RESOURCE);
    }

    public static Properties loadFromExternal(Path path) {
        Objects.requireNonNull(path, "path");
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar la configuracion externa: " + path, ex);
        }
    }

    public static Properties loadFromClasspath(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        Properties properties = new Properties();
        try (InputStream inputStream = PropertyLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("No se encontro el recurso de configuracion: " + resourceName);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar la configuracion desde classpath: " + resourceName, ex);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
