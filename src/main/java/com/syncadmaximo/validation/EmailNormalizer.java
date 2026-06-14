package com.syncadmaximo.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza y valida correos electrónicos con una validación básica.
 */
public class EmailNormalizer {

    private static final Pattern BASIC_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return normalized;
    }

    public boolean isBasicFormatValid(String rawValue) {
        String normalized = normalize(rawValue);
        return normalized != null && !normalized.isEmpty() && BASIC_EMAIL_PATTERN.matcher(normalized).matches();
    }
}
