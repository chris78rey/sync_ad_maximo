package com.syncadmaximo.service;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.util.StringSanitizer;

/**
 * Construye el candidato de MAXIMO para un usuario de AD.
 * Mantiene esta lógica fuera del orquestador para reducir acoplamiento.
 */
public class CreationService {

    public MaximoPerson buildCandidate(MaximoPerson existing,
                                       AdUser adUser,
                                       String targetPersonId,
                                       String normalizedCedula,
                                       String normalizedEmail) {
        MaximoPerson candidate = existing == null ? new MaximoPerson() : copy(existing);
        candidate.setPersonId(normalizeOrFallback(targetPersonId, existing == null ? null : existing.getPersonId()));
        candidate.setStatus(adUser != null && adUser.isEnabled() ? "ACTIVO" : "INACTIVO");
        candidate.setFirstName(normalizeOrFallback(adUser == null ? null : adUser.getGivenName(), candidate.getFirstName()));
        candidate.setLastName(normalizeOrFallback(adUser == null ? null : adUser.getSn(), candidate.getLastName()));
        candidate.setEppCedula(normalizedCedula == null ? candidate.getEppCedula() : normalizedCedula);
        candidate.setEmailAddress(normalizedEmail == null || normalizedEmail.isEmpty() ? candidate.getEmailAddress() : normalizedEmail);
        return candidate;
    }

    private MaximoPerson copy(MaximoPerson source) {
        MaximoPerson copy = new MaximoPerson();
        copy.setPersonId(source.getPersonId());
        copy.setStatus(source.getStatus());
        copy.setFirstName(source.getFirstName());
        copy.setLastName(source.getLastName());
        copy.setEppCedula(source.getEppCedula());
        copy.setEppNumRol(source.getEppNumRol());
        copy.setEmailAddress(source.getEmailAddress());
        return copy;
    }

    private String normalizeOrFallback(String value, String fallback) {
        String trimmed = StringSanitizer.trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }
}
