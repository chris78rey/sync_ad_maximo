package com.syncadmaximo.service;

import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.orchestration.SynchronizationPlan;
import com.syncadmaximo.util.StringSanitizer;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resuelve coincidencias entre AD y MAXIMO y ejecuta la migración de login cuando corresponde.
 */
public class IdentityMatchingService {

    private static final Logger LOGGER = Logger.getLogger(IdentityMatchingService.class.getName());

    private final MaximoRepository repository;
    private final AuditRepository auditRepository;

    public IdentityMatchingService(MaximoRepository repository, AuditRepository auditRepository) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public MatchResult resolve(String userKey,
                               String cedula,
                               String email,
                               Collection<MaximoPerson> maximoPeople,
                               SynchronizationPlan plan,
                               SyncResult result) {
        MaximoPerson byUser = findByUserKey(maximoPeople, userKey);
        MaximoPerson byCedula = findByCedula(maximoPeople, cedula);
        MaximoPerson byEmail = findByEmail(maximoPeople, email);

        MaximoPerson resolved = byUser;
        MatchType type = MatchType.NONE;
        if (resolved != null) {
            plan.incrementMatchedByUser();
            type = MatchType.USER;
        }
        if (resolved == null && byCedula != null) {
            resolved = byCedula;
            plan.incrementMatchedByCedula();
            type = MatchType.CEDULA;
        }
        if (resolved == null && byEmail != null) {
            resolved = byEmail;
            plan.incrementMatchedByEmail();
            type = MatchType.EMAIL;
        }

        if (resolved != null) {
            if (byUser != null && byCedula != null && !samePersonId(byUser, byCedula)) {
                appendConflict(plan, result, userKey, "La cédula coincide con otra persona distinta.");
            }
            if (byUser != null && byEmail != null && !samePersonId(byUser, byEmail)) {
                appendConflict(plan, result, userKey, "El correo coincide con otra persona distinta.");
            }
            if (byCedula != null && byEmail != null && !samePersonId(byCedula, byEmail)) {
                appendConflict(plan, result, userKey, "La cédula y el correo apuntan a personas distintas.");
            }
        }

        return new MatchResult(resolved, type);
    }

    public boolean migrateByCedula(MatchResult match,
                                   String cedula,
                                   String currentUserKey,
                                   Map<String, MaximoPerson> maximoPeople,
                                   SynchronizationPlan plan,
                                   SyncResult result) {
        if (match == null || match.getPerson() == null || repository == null) {
            return true;
        }
        if (cedula == null || currentUserKey == null) {
            return true;
        }
        String currentPersonId = normalizeUserKey(match.getPerson().getPersonId());
        String newPersonId = normalizeUserKey(currentUserKey);
        if (Objects.equals(currentPersonId, newPersonId)) {
            return true;
        }

        try {
            int affected = repository.updatePersonIdByCedula(cedula, currentPersonId, newPersonId);
            if (affected <= 0) {
                SyncIssue issue = new SyncIssue();
                issue.setCode("MIGRATION_NOT_APPLIED");
                issue.setDescription("No se pudo migrar el usuario por cédula.");
                issue.setReferenceId(newPersonId);
                appendIssue(plan, result, issue);
                return false;
            }
            MaximoPerson migrated = copy(match.getPerson());
            migrated.setPersonId(newPersonId);
            if (maximoPeople != null) {
                maximoPeople.remove(currentPersonId);
                maximoPeople.put(newPersonId, migrated);
            }
            match.setPerson(migrated);
            return true;
        } catch (SQLException ex) {
            plan.incrementFailed();
            appendIssue(plan, result, toIssue("MIGRATION_ERROR", ex.getMessage(), newPersonId, ex));
            LOGGER.log(Level.WARNING, "No se pudo migrar el personId para " + newPersonId, ex);
            return false;
        }
    }

    private void appendConflict(SynchronizationPlan plan, SyncResult result, String referenceId, String description) {
        SyncIssue issue = new SyncIssue();
        issue.setCode("MATCH_CONFLICT");
        issue.setDescription(description);
        issue.setReferenceId(referenceId);
        appendIssue(plan, result, issue);
    }

    private void appendIssue(SynchronizationPlan plan, SyncResult result, SyncIssue issue) {
        if (issue == null) {
            return;
        }
        result.addIssue(issue);
        plan.addIssue(issue);
        if (auditRepository != null) {
            try {
                auditRepository.saveIssue(result == null ? null : result.getRunId(), issue);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.FINE, "No se pudo auditar una novedad", ex);
            }
        }
    }

    private static SyncIssue toIssue(String code, String description, String referenceId, Throwable throwable) {
        SyncIssue issue = new SyncIssue();
        issue.setCode(code);
        issue.setDescription(description == null ? (throwable == null ? code : throwable.getMessage()) : description);
        issue.setReferenceId(referenceId);
        if (throwable != null && (issue.getDescription() == null || issue.getDescription().trim().isEmpty())) {
            issue.setDescription(throwable.getClass().getSimpleName());
        }
        return issue;
    }

    private static MaximoPerson findByUserKey(Collection<MaximoPerson> people, String userKey) {
        if (people == null || userKey == null) {
            return null;
        }
        for (MaximoPerson person : people) {
            if (person != null && Objects.equals(normalizeUserKey(person.getPersonId()), userKey)) {
                return person;
            }
        }
        return null;
    }

    private static MaximoPerson findByCedula(Collection<MaximoPerson> people, String cedula) {
        if (people == null || cedula == null) {
            return null;
        }
        for (MaximoPerson person : people) {
            if (person != null && cedula.equals(normalizeCedula(person.getEppCedula()))) {
                return person;
            }
        }
        return null;
    }

    private static MaximoPerson findByEmail(Collection<MaximoPerson> people, String email) {
        if (people == null || email == null) {
            return null;
        }
        for (MaximoPerson person : people) {
            if (person != null && email.equals(normalizeEmail(person.getEmailAddress()))) {
                return person;
            }
        }
        return null;
    }

    private static boolean samePersonId(MaximoPerson left, MaximoPerson right) {
        return left != null && right != null && Objects.equals(normalizeUserKey(left.getPersonId()), normalizeUserKey(right.getPersonId()));
    }

    private static MaximoPerson copy(MaximoPerson source) {
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

    private static String normalizeUserKey(String value) {
        return StringSanitizer.normalizeUserName(value);
    }

    private static String normalizeCedula(String value) {
        String normalized = StringSanitizer.digitsOnly(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() == 9) {
            return '0' + normalized;
        }
        return normalized;
    }

    private static String normalizeEmail(String value) {
        return StringSanitizer.normalizeEmail(value);
    }

    public enum MatchType {
        NONE,
        USER,
        CEDULA,
        EMAIL
    }

    public static final class MatchResult {
        private MaximoPerson person;
        private final MatchType type;

        private MatchResult(MaximoPerson person, MatchType type) {
            this.person = person;
            this.type = type == null ? MatchType.NONE : type;
        }

        public MaximoPerson getPerson() {
            return person;
        }

        public void setPerson(MaximoPerson person) {
            this.person = person;
        }

        public MatchType getType() {
            return type;
        }
    }
}
