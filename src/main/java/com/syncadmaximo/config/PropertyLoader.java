package com.syncadmaximo.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

public final class PropertyLoader {

    public static final String ENV_CONFIG_PATH = "SYNC_AD_MAXIMO_CONFIG";
    public static final String DEFAULT_RESOURCE = "application.properties";

    private PropertyLoader() {
    }

    public static Properties load() {
        String externalPath = trimToNull(System.getenv(ENV_CONFIG_PATH));
        if (externalPath != null) {
            return loadFromPath(Paths.get(externalPath));
        }
        return loadFromClasspath(DEFAULT_RESOURCE);
    }

    public static Properties loadFromPath(Path path) {
        Objects.requireNonNull(path, "path");
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return loadYaml(path);
        }
        return loadProperties(path);
    }

    public static Properties loadProperties(Path path) {
        Objects.requireNonNull(path, "path");
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar la configuracion externa: " + path, ex);
        }
    }

    public static Properties loadYaml(Path path) {
        Objects.requireNonNull(path, "path");
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object parsed = new Yaml().load(inputStream);
            Properties properties = new Properties();
            if (parsed instanceof Map) {
                flattenMap(properties, "", (Map<?, ?>) parsed);
            }
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar la configuracion YAML externa: " + path, ex);
        }
    }

    public static Properties loadFromClasspath(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        Properties properties = new Properties();
        try (InputStream inputStream = PropertyLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("No se encontro el recurso de configuracion: " + resourceName);
            }
            if (resourceName.toLowerCase().endsWith(".yml") || resourceName.toLowerCase().endsWith(".yaml")) {
                Object parsed = new Yaml().load(inputStream);
                if (parsed instanceof Map) {
                    flattenMap(properties, "", (Map<?, ?>) parsed);
                }
            } else {
                properties.load(inputStream);
            }
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar la configuracion desde classpath: " + resourceName, ex);
        }
    }

    private static void flattenMap(Properties properties, String prefix, Map<?, ?> source) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = entry.getKey() == null ? null : String.valueOf(entry.getKey()).trim();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String qualifiedKey = prefix == null || prefix.isEmpty() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenMap(properties, qualifiedKey, (Map<?, ?>) value);
                continue;
            }
            if (value instanceof List) {
                properties.setProperty(qualifiedKey, joinList((List<?>) value));
                continue;
            }
            if (value != null) {
                properties.setProperty(qualifiedKey, String.valueOf(value));
            }
        }
    }

    private static String joinList(List<?> values) {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            if (value != null) {
                builder.append(String.valueOf(value).trim());
            }
        }
        return builder.toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
