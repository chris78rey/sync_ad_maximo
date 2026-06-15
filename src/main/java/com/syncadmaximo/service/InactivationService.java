package com.syncadmaximo.service;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.repository.MaximoRepository;
import com.syncadmaximo.util.StringSanitizer;

import java.util.Optional;

/**
 * Maneja la inactivación de personas de MAXIMO cuando AD reporta la cuenta deshabilitada.
 */
public class InactivationService {

    public enum Outcome {
        SKIPPED,
        UNCHANGED,
        UPDATED
    }

    private final MaximoRepository repository;

    public InactivationService(MaximoRepository repository) {
        this.repository = repository;
    }

    public Outcome inactivateIfNeeded(MaximoPerson existing, AdUser adUser) {
        if (repository == null || existing == null || adUser == null) {
            return Outcome.SKIPPED;
        }
        if (adUser.isEnabled()) {
            return Outcome.SKIPPED;
        }

        String personId = StringSanitizer.trimToNull(existing.getPersonId());
        if (personId == null) {
            return Outcome.SKIPPED;
        }
        if ("INACTIVO".equalsIgnoreCase(StringSanitizer.trimToNull(existing.getStatus()))) {
            return Outcome.UNCHANGED;
        }

        existing.setStatus("INACTIVO");
        repository.saveOrUpdate(existing);
        return Outcome.UPDATED;
    }

    public boolean isDisabled(AdUser adUser) {
        return adUser != null && !adUser.isEnabled();
    }
}
