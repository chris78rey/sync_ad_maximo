package com.syncadmaximo.validation;

import java.util.Locale;

/**
 * Normaliza el usuario para comparaciones lógicas sin alterar el valor original de escritura.
 */
public class UserNormalizer {

    public String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String compact = rawValue.trim().replaceAll("\\s+", "");
        if (compact.isEmpty()) {
            return "";
        }
        return compact.toLowerCase(Locale.ROOT);
    }
}
