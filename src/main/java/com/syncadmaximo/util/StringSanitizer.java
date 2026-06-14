package com.syncadmaximo.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StringSanitizer {

    private static final Pattern NON_DIGITS = Pattern.compile("[^0-9]");
    private static final Pattern BASIC_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private StringSanitizer() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeUserName(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    public static String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    public static String digitsOnly(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : NON_DIGITS.matcher(trimmed).replaceAll("");
    }

    public static boolean isBasicEmail(String value) {
        String normalized = normalizeEmail(value);
        return normalized != null && BASIC_EMAIL.matcher(normalized).matches();
    }
}
