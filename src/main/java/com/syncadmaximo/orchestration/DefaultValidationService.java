package com.syncadmaximo.orchestration;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.service.ValidationService;
import com.syncadmaximo.util.StringSanitizer;

import java.util.Optional;

/**
 * Implementación mínima y nula-segura de ValidationService para la capa de orquestación.
 * Mantiene la lógica de validación básica sin depender de frameworks externos.
 */
public final class DefaultValidationService implements ValidationService {

    @Override
    public boolean isValidCedula(String cedula) {
        String normalized = StringSanitizer.digitsOnly(cedula);
        return normalized != null && normalized.length() == 10;
    }

    @Override
    public boolean isValidEmail(String email) {
        return StringSanitizer.isBasicEmail(email);
    }

    @Override
    public boolean isValidUserName(String userName) {
        String normalized = StringSanitizer.normalizeUserName(userName);
        return normalized != null && !normalized.isEmpty();
    }

    @Override
    public Optional<String> normalizeCedula(String cedula) {
        String normalized = StringSanitizer.digitsOnly(cedula);
        if (normalized == null || normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.length() == 9) {
            normalized = '0' + normalized;
        }
        return Optional.of(normalized);
    }

    @Override
    public boolean matchesByCedula(AdUser adUser, MaximoPerson maximoPerson) {
        if (adUser == null || maximoPerson == null) {
            return false;
        }
        String adCedula = normalizeCedula(adUser.getPostalCode()).orElse(null);
        String maximoCedula = StringSanitizer.digitsOnly(maximoPerson.getEppCedula());
        return adCedula != null && adCedula.equals(maximoCedula);
    }
}
