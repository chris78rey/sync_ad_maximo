package com.syncadmaximo.validation;

/**
 * Normaliza cédulas eliminando espacios, guiones y cualquier carácter no numérico.
 */
public class CedulaNormalizer {

    private final CedulaNineDigitStrategy nineDigitStrategy;

    public CedulaNormalizer() {
        this(CedulaNineDigitStrategy.AGREGAR_CERO);
    }

    public CedulaNormalizer(CedulaNineDigitStrategy nineDigitStrategy) {
        this.nineDigitStrategy = nineDigitStrategy == null ? CedulaNineDigitStrategy.AGREGAR_CERO : nineDigitStrategy;
    }

    public String normalize(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String digits = rawValue.replaceAll("[^0-9]", "");
        if (digits.length() == 9 && nineDigitStrategy == CedulaNineDigitStrategy.AGREGAR_CERO) {
            return '0' + digits;
        }
        return digits;
    }

    public CedulaNineDigitStrategy getNineDigitStrategy() {
        return nineDigitStrategy;
    }
}
