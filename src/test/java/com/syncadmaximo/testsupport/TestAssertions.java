package com.syncadmaximo.testsupport;

import java.util.Objects;

public final class TestAssertions {

    private TestAssertions() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void equals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | esperado=" + expected + " | actual=" + actual);
        }
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    public static void contains(String text, String needle, String message) {
        if (text == null || needle == null || !text.contains(needle)) {
            throw new AssertionError(message + " | texto=" + text + " | buscado=" + needle);
        }
    }
}
