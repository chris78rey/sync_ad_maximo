package com.syncadmaximo.service;

import com.syncadmaximo.repository.MaximoRepository;
import com.syncadmaximo.util.StringSanitizer;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Sincroniza el correo primario de MAXIMO contra el correo de AD.
 * Si no hay repositorio Oracle disponible, opera como no-op.
 */
public class EmailSyncService {

    public enum Outcome {
        SKIPPED,
        UNCHANGED,
        UPDATED,
        INSERTED
    }

    private final MaximoRepository repository;

    public EmailSyncService(MaximoRepository repository) {
        this.repository = repository;
    }

    public Outcome syncPrimaryEmail(String personId, String emailAddress) throws SQLException {
        if (repository == null) {
            return Outcome.SKIPPED;
        }
        String normalizedPersonId = StringSanitizer.trimToNull(personId);
        String normalizedEmail = StringSanitizer.normalizeEmail(emailAddress);
        if (normalizedPersonId == null || normalizedEmail == null || normalizedEmail.isEmpty()) {
            return Outcome.SKIPPED;
        }

        if (repository.isEmailAssignedToDifferentPerson(normalizedEmail, normalizedPersonId)) {
            throw new SQLException("El correo ya pertenece a otra persona: " + normalizedEmail);
        }

        Optional<String> currentEmail = repository.findPrimaryEmailByPersonId(normalizedPersonId);
        if (currentEmail.isPresent()) {
            String normalizedCurrent = StringSanitizer.normalizeEmail(currentEmail.get());
            if (normalizedEmail.equals(normalizedCurrent)) {
                return Outcome.UNCHANGED;
            }
            repository.updatePrimaryEmail(normalizedPersonId, normalizedEmail);
            return Outcome.UPDATED;
        }

        repository.insertConfiguredPrimaryEmail(normalizedPersonId, normalizedEmail);
        return Outcome.INSERTED;
    }
}
