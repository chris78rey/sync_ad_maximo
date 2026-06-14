package com.syncadmaximo.validation;

/**
 * Estrategia aplicada cuando la cédula normalizada tiene 9 dígitos.
 */
public enum CedulaNineDigitStrategy {
    AGREGAR_CERO,
    REPORTAR_NOVEDAD
}
