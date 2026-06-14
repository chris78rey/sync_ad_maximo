package com.syncadmaximo.validation;

/**
 * Regla de validación extensible para la capa de normalización/validación.
 */
public interface ValidationRule<T> {
    void validate(T value, ValidationReport report);
}
