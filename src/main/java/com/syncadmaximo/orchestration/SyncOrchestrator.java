package com.syncadmaximo.orchestration;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.service.AuditRepository;
import com.syncadmaximo.service.DirectoryService;
import com.syncadmaximo.service.MailService;
import com.syncadmaximo.service.MaximoRepository;
import com.syncadmaximo.service.SyncService;
import com.syncadmaximo.service.ValidationService;
import com.syncadmaximo.util.StringSanitizer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orquestador principal del proceso de sincronización AD -> MAXIMO.
 * Diseñado para ser invocado desde la capa web o desde un scheduler independiente.
 */
public class SyncOrchestrator implements SyncService {

    private static final Logger LOGGER = Logger.getLogger(SyncOrchestrator.class.getName());

    private final AppConfig config;
    private final DirectoryService directoryService;
    private final MaximoRepository maximoRepository;
    private final ValidationService validationService;
    private final AuditRepository auditRepository;
    private final MailService mailService;

    public SyncOrchestrator() {
        this(AppConfig.getInstance(), null, null, null, null, null);
    }

    public SyncOrchestrator(AppConfig config,
                            DirectoryService directoryService,
                            MaximoRepository maximoRepository,
                            ValidationService validationService,
                            AuditRepository auditRepository,
                            MailService mailService) {
        this.config = config == null ? AppConfig.getInstance() : config;
        this.directoryService = directoryService == null ? new NoOpDirectoryService() : directoryService;
        this.maximoRepository = maximoRepository == null ? new NoOpMaximoRepository() : maximoRepository;
        this.validationService = validationService == null ? new DefaultValidationService() : validationService;
        this.auditRepository = auditRepository == null ? new NoOpAuditRepository() : auditRepository;
        this.mailService = mailService == null ? new NoOpMailService() : mailService;
    }

    @Override
    public SyncResult execute(SyncExecution execution) {
        SyncExecution effectiveExecution = initializeExecution(execution);
        SyncResult result = new SyncResult();
        result.setRunId(effectiveExecution.getRunId());

        SynchronizationPlan plan = new SynchronizationPlan();
        boolean fatalFailure = false;

        try {
            auditStart(effectiveExecution);

            Map<String, AdUser> adUsers = loadDirectoryUsers(plan, result);
            Map<String, MaximoPerson> maximoPeople = loadMaximoPeople(plan);

            for (AdUser adUser : adUsers.values()) {
                processUser(effectiveExecution, adUser, maximoPeople, plan, result);
            }

            result.setSuccess(!fatalFailure && plan.getFailed() == 0);
            result.setMessage(buildResultMessage(effectiveExecution, plan));
        } catch (RuntimeException ex) {
            fatalFailure = true;
            SyncIssue issue = toIssue("EXECUTION_FAILURE", ex.getMessage(), effectiveExecution.getRunId(), ex);
            appendIssue(result, plan, issue);
            result.setSuccess(false);
            result.setMessage(buildResultMessage(effectiveExecution, plan));
            LOGGER.log(Level.SEVERE, "Fallo la ejecucion de sincronizacion", ex);
        } finally {
            finalizeExecution(effectiveExecution, plan, result);
        }

        result.setSuccess(!fatalFailure && plan.getFailed() == 0);
        if (result.getMessage() == null || result.getMessage().trim().isEmpty()) {
            result.setMessage(buildResultMessage(effectiveExecution, plan));
        }
        return result;
    }

    private SyncExecution initializeExecution(SyncExecution execution) {
        SyncExecution effective = execution == null ? new SyncExecution() : execution;
        if (StringSanitizer.trimToNull(effective.getRunId()) == null) {
            effective.setRunId(UUID.randomUUID().toString());
        }
        if (StringSanitizer.trimToNull(effective.getProcessName()) == null) {
            effective.setProcessName(config.getAppName());
        }
        if (effective.getStartedAt() == null) {
            effective.setStartedAt(Instant.now());
        }
        if (effective.getFinishedAt() == null) {
            effective.setFinishedAt(null);
        }
        if (effective.getInitiatedBy() == null || effective.getInitiatedBy().trim().isEmpty()) {
            effective.setInitiatedBy("scheduler");
        }
        effective.setDryRun(ExecutionMode.fromConfig(config).isDryRun());
        return effective;
    }

    private void auditStart(SyncExecution execution) {
        try {
            auditRepository.saveExecutionStart(execution);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "No se pudo registrar el inicio de la ejecucion", ex);
        }
    }

    private void finalizeExecution(SyncExecution execution, SynchronizationPlan plan, SyncResult result) {
        execution.setFinishedAt(Instant.now());
        try {
            auditRepository.saveExecutionEnd(execution);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "No se pudo registrar el fin de la ejecucion", ex);
        }

        sendSummaryMail(execution, plan, result);
    }

    private Map<String, AdUser> loadDirectoryUsers(SynchronizationPlan plan, SyncResult result) {
        Map<String, AdUser> users = new LinkedHashMap<String, AdUser>();
        List<AdUser> enabled = safeList(directoryService.findEnabledUsers());
        List<AdUser> disabled = safeList(directoryService.findDisabledUsers());

        for (AdUser user : enabled) {
            addDirectoryUser(users, user, plan, result);
        }
        for (AdUser user : disabled) {
            addDirectoryUser(users, user, plan, result);
        }
        plan.setAdLoaded(users.size());
        return users;
    }

    private void addDirectoryUser(Map<String, AdUser> users, AdUser user, SynchronizationPlan plan, SyncResult result) {
        if (user == null) {
            return;
        }
        String key = normalizeUserKey(user.getsAMAccountName());
        if (key == null) {
            SyncIssue issue = new SyncIssue();
            issue.setCode("AD_USER_INVALID");
            issue.setDescription("Se omitió un usuario de AD sin sAMAccountName válido.");
            appendIssue(result, plan, issue);
            plan.incrementSkipped();
            return;
        }
        if (users.containsKey(key)) {
            SyncIssue issue = new SyncIssue();
            issue.setCode("AD_USER_DUPLICATE");
            issue.setDescription("Se encontró un usuario de AD duplicado en la carga.");
            issue.setReferenceId(key);
            appendIssue(result, plan, issue);
            return;
        }
        users.put(key, user);
    }

    private Map<String, MaximoPerson> loadMaximoPeople(SynchronizationPlan plan) {
        Map<String, MaximoPerson> people = new LinkedHashMap<String, MaximoPerson>();
        Set<String> statuses = resolveMaximoStatuses();
        for (String status : statuses) {
            List<MaximoPerson> list = safeList(maximoRepository.findPeopleByStatus(status));
            for (MaximoPerson person : list) {
                if (person == null) {
                    continue;
                }
                String key = normalizeUserKey(person.getPersonId());
                if (key == null) {
                    continue;
                }
                if (!people.containsKey(key)) {
                    people.put(key, person);
                }
            }
        }
        plan.setMaximoLoaded(people.size());
        return people;
    }

    private Set<String> resolveMaximoStatuses() {
        String configured = StringSanitizer.trimToNull(config.getString("sync.maximo.statuses", null));
        LinkedHashSet<String> statuses = new LinkedHashSet<String>();
        if (configured != null) {
            String[] parts = configured.split(",");
            for (String part : parts) {
                String trimmed = StringSanitizer.trimToNull(part);
                if (trimmed != null) {
                    statuses.add(trimmed.toUpperCase(Locale.ROOT));
                }
            }
        }
        if (statuses.isEmpty()) {
            statuses.add("ACTIVO");
            statuses.add("INACTIVO");
        }
        return statuses;
    }

    private void processUser(SyncExecution execution,
                             AdUser adUser,
                             Map<String, MaximoPerson> maximoPeople,
                             SynchronizationPlan plan,
                             SyncResult result) {
        if (adUser == null) {
            plan.incrementSkipped();
            SyncIssue issue = new SyncIssue();
            issue.setCode("AD_USER_NULL");
            issue.setDescription("Se recibió un usuario nulo desde AD.");
            appendIssue(result, plan, issue);
            return;
        }

        ValidationSnapshot snapshot = validate(adUser);
        if (!snapshot.isUserValid()) {
            plan.setValidatedRejected(plan.getValidatedRejected() + 1);
            plan.incrementSkipped();
            SyncIssue issue = new SyncIssue();
            issue.setCode("VALIDATION_REJECTED");
            issue.setDescription(snapshot.buildMessage());
            issue.setReferenceId(snapshot.getUserKey());
            appendIssue(result, plan, issue);
            return;
        }
        if (!snapshot.isReviewFree()) {
            plan.setValidatedReview(plan.getValidatedReview() + 1);
            SyncIssue issue = new SyncIssue();
            issue.setCode("VALIDATION_REVIEW");
            issue.setDescription("El usuario requiere revisión manual por datos no normalizados o incompletos.");
            issue.setReferenceId(snapshot.getUserKey());
            appendIssue(result, plan, issue);
        } else {
            plan.setValidatedAccepted(plan.getValidatedAccepted() + 1);
        }

        MaximoPerson existing = findMatchingPerson(snapshot, maximoPeople, plan, result);
        MaximoPerson candidate = buildCandidate(existing, adUser, snapshot);
        if (candidate == null) {
            plan.incrementSkipped();
            SyncIssue issue = new SyncIssue();
            issue.setCode("CANDIDATE_EMPTY");
            issue.setDescription("No se pudo construir el registro objetivo.");
            issue.setReferenceId(snapshot.getUserKey());
            appendIssue(result, plan, issue);
            return;
        }

        boolean changed = existing == null || !samePerson(existing, candidate);
        if (execution.isDryRun()) {
            if (changed) {
                if (existing == null) {
                    plan.incrementCreated();
                } else {
                    plan.incrementUpdated();
                }
                SyncIssue issue = new SyncIssue();
                issue.setCode("DRY_RUN");
                issue.setDescription(describePlannedAction(existing, candidate));
                issue.setReferenceId(candidate.getPersonId());
                appendIssue(result, plan, issue);
            } else {
                plan.incrementUnchanged();
            }
            return;
        }

        if (!changed) {
            plan.incrementUnchanged();
            return;
        }

        try {
            maximoRepository.saveOrUpdate(candidate);
            maximoPeople.put(normalizeUserKey(candidate.getPersonId()), candidate);
            if (existing == null) {
                plan.incrementCreated();
            } else {
                plan.incrementUpdated();
            }
        } catch (RuntimeException ex) {
            plan.incrementFailed();
            appendIssue(result, plan, toIssue("PERSISTENCE_ERROR", ex.getMessage(), candidate.getPersonId(), ex));
            LOGGER.log(Level.WARNING, "No se pudo persistir el cambio para " + candidate.getPersonId(), ex);
        }
    }

    private ValidationSnapshot validate(AdUser user) {
        ValidationSnapshot snapshot = new ValidationSnapshot(user);
        snapshot.setUserKey(normalizeUserKey(user.getsAMAccountName()));
        snapshot.setCedula(validationService.normalizeCedula(user.getPostalCode()).orElse(null));
        snapshot.setEmail(StringSanitizer.normalizeEmail(user.getMail()));
        snapshot.setUserValid(validationService.isValidUserName(user.getsAMAccountName()));
        snapshot.setCedulaValid(snapshot.getCedula() != null && validationService.isValidCedula(snapshot.getCedula()));
        snapshot.setEmailValid(snapshot.getEmail() == null || snapshot.getEmail().isEmpty() || validationService.isValidEmail(snapshot.getEmail()));
        snapshot.setEnabled(user.isEnabled());
        return snapshot;
    }

    private MaximoPerson findMatchingPerson(ValidationSnapshot snapshot,
                                            Map<String, MaximoPerson> maximoPeople,
                                            SynchronizationPlan plan,
                                            SyncResult result) {
        MaximoPerson byUser = maximoPeople.get(snapshot.getUserKey());
        MaximoPerson byCedula = findByCedula(maximoPeople.values(), snapshot.getCedula());
        MaximoPerson byEmail = findByEmail(maximoPeople.values(), snapshot.getEmail());

        MaximoPerson resolved = byUser;
        if (resolved != null) {
            plan.incrementMatchedByUser();
        }
        if (resolved == null && byCedula != null) {
            resolved = byCedula;
            plan.incrementMatchedByCedula();
        }
        if (resolved == null && byEmail != null) {
            resolved = byEmail;
            plan.incrementMatchedByEmail();
        }

        if (resolved != null) {
            if (byUser != null && byCedula != null && !samePersonId(byUser, byCedula)) {
                SyncIssue issue = new SyncIssue();
                issue.setCode("MATCH_CONFLICT");
                issue.setDescription("La cédula coincide con otra persona distinta.");
                issue.setReferenceId(snapshot.getUserKey());
                appendIssue(result, plan, issue);
            }
            if (byUser != null && byEmail != null && !samePersonId(byUser, byEmail)) {
                SyncIssue issue = new SyncIssue();
                issue.setCode("MATCH_CONFLICT");
                issue.setDescription("El correo coincide con otra persona distinta.");
                issue.setReferenceId(snapshot.getUserKey());
                appendIssue(result, plan, issue);
            }
            if (byCedula != null && byEmail != null && !samePersonId(byCedula, byEmail)) {
                SyncIssue issue = new SyncIssue();
                issue.setCode("MATCH_CONFLICT");
                issue.setDescription("La cédula y el correo apuntan a personas distintas.");
                issue.setReferenceId(snapshot.getUserKey());
                appendIssue(result, plan, issue);
            }
        }

        return resolved;
    }

    private MaximoPerson buildCandidate(MaximoPerson existing, AdUser adUser, ValidationSnapshot snapshot) {
        MaximoPerson candidate = existing == null ? new MaximoPerson() : copy(existing);
        candidate.setPersonId(existing != null ? existing.getPersonId() : snapshot.getUserKey());
        candidate.setStatus(adUser.isEnabled() ? "ACTIVO" : "INACTIVO");
        candidate.setFirstName(normalizeValue(adUser.getGivenName(), candidate.getFirstName()));
        candidate.setLastName(normalizeValue(adUser.getSn(), candidate.getLastName()));
        candidate.setEppCedula(snapshot.getCedula() == null ? candidate.getEppCedula() : snapshot.getCedula());
        candidate.setEmailAddress(snapshot.getEmail() == null || snapshot.getEmail().isEmpty() ? candidate.getEmailAddress() : snapshot.getEmail());
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

    private boolean samePerson(MaximoPerson left, MaximoPerson right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(normalizeValue(left.getPersonId(), null), normalizeValue(right.getPersonId(), null))
                && Objects.equals(normalizeValue(left.getStatus(), null), normalizeValue(right.getStatus(), null))
                && Objects.equals(normalizeValue(left.getFirstName(), null), normalizeValue(right.getFirstName(), null))
                && Objects.equals(normalizeValue(left.getLastName(), null), normalizeValue(right.getLastName(), null))
                && Objects.equals(normalizeCedula(left.getEppCedula()), normalizeCedula(right.getEppCedula()))
                && Objects.equals(normalizeValue(left.getEmailAddress(), null), normalizeValue(right.getEmailAddress(), null));
    }

    private boolean samePersonId(MaximoPerson left, MaximoPerson right) {
        return left != null && right != null && Objects.equals(normalizeUserKey(left.getPersonId()), normalizeUserKey(right.getPersonId()));
    }

    private MaximoPerson findByCedula(Collection<MaximoPerson> people, String cedula) {
        if (cedula == null) {
            return null;
        }
        for (MaximoPerson person : people) {
            if (person == null) {
                continue;
            }
            if (cedula.equals(normalizeCedula(person.getEppCedula()))) {
                return person;
            }
        }
        return null;
    }

    private MaximoPerson findByEmail(Collection<MaximoPerson> people, String email) {
        if (email == null) {
            return null;
        }
        for (MaximoPerson person : people) {
            if (person == null) {
                continue;
            }
            if (email.equals(normalizeEmail(person.getEmailAddress()))) {
                return person;
            }
        }
        return null;
    }

    private void appendIssue(SyncResult result, SynchronizationPlan plan, SyncIssue issue) {
        if (issue == null) {
            return;
        }
        result.addIssue(issue);
        plan.addIssue(issue);
        try {
            auditRepository.saveIssue(result.getRunId(), issue);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "No se pudo auditar una novedad", ex);
        }
    }

    private SyncIssue toIssue(String code, String description, String referenceId, Throwable throwable) {
        SyncIssue issue = new SyncIssue();
        issue.setCode(code);
        issue.setDescription(description == null ? (throwable == null ? code : throwable.getMessage()) : description);
        issue.setReferenceId(referenceId);
        if (throwable != null && (issue.getDescription() == null || issue.getDescription().trim().isEmpty())) {
            issue.setDescription(throwable.getClass().getSimpleName());
        }
        return issue;
    }

    private String describePlannedAction(MaximoPerson existing, MaximoPerson candidate) {
        if (existing == null) {
            return "Se crearía/actualizaría la persona " + candidate.getPersonId() + " en modo dry-run.";
        }
        return "Se actualizaría la persona " + candidate.getPersonId() + " en modo dry-run.";
    }

    private String buildResultMessage(SyncExecution execution, SynchronizationPlan plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ejecucion ").append(execution.getRunId())
                .append(" (").append(execution.isDryRun() ? "DRY_RUN" : "PRODUCTION").append(")")
                .append(" - ").append(plan.buildSummary());
        return builder.toString();
    }

    private void sendSummaryMail(SyncExecution execution, SynchronizationPlan plan, SyncResult result) {
        try {
            String body = buildMailBody(execution, plan, result);
            mailService.sendExecutionSummary(execution, body);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "No se pudo enviar el resumen por correo", ex);
        }
    }

    private String buildMailBody(SyncExecution execution, SynchronizationPlan plan, SyncResult result) {
        StringBuilder body = new StringBuilder();
        body.append("RunId: ").append(execution.getRunId()).append('\n');
        body.append("Modo: ").append(execution.isDryRun() ? "DRY_RUN" : "PRODUCTION").append('\n');
        body.append("Proceso: ").append(execution.getProcessName()).append('\n');
        body.append("Inicio: ").append(execution.getStartedAt()).append('\n');
        body.append("Fin: ").append(execution.getFinishedAt()).append('\n');
        body.append('\n').append(plan.buildSummary()).append('\n');
        if (result != null && !result.getIssues().isEmpty()) {
            body.append('\n').append("Issues:\n");
            for (SyncIssue issue : result.getIssues()) {
                body.append("- ").append(issue.getCode()).append(" | ")
                        .append(issue.getReferenceId()).append(" | ")
                        .append(issue.getDescription()).append('\n');
            }
        }
        return body.toString();
    }

    private String normalizeUserKey(String value) {
        return StringSanitizer.normalizeUserName(value);
    }

    private String normalizeCedula(String value) {
        String normalized = StringSanitizer.digitsOnly(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() == 9) {
            return '0' + normalized;
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        return StringSanitizer.normalizeEmail(value);
    }

    private String normalizeValue(String value, String fallback) {
        String trimmed = StringSanitizer.trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    private static final class ValidationSnapshot {
        private final AdUser user;
        private String userKey;
        private String cedula;
        private String email;
        private boolean userValid;
        private boolean cedulaValid;
        private boolean emailValid;
        private boolean enabled;

        private ValidationSnapshot(AdUser user) {
            this.user = user;
        }

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }

        public String getCedula() {
            return cedula;
        }

        public void setCedula(String cedula) {
            this.cedula = cedula;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setUserValid(boolean userValid) {
            this.userValid = userValid;
        }

        public void setCedulaValid(boolean cedulaValid) {
            this.cedulaValid = cedulaValid;
        }

        public void setEmailValid(boolean emailValid) {
            this.emailValid = emailValid;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isUserValid() {
            return userValid;
        }

        public boolean isReviewFree() {
            return cedulaValid && emailValid;
        }

        public String buildMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Usuario invalido o no procesable: ").append(user == null ? "null" : user.toString());
            return builder.toString();
        }
    }

    private static final class NoOpDirectoryService implements DirectoryService {
        @Override
        public List<AdUser> findEnabledUsers() {
            return Collections.emptyList();
        }

        @Override
        public List<AdUser> findDisabledUsers() {
            return Collections.emptyList();
        }

        @Override
        public java.util.Optional<AdUser> findByUserName(String userName) {
            return java.util.Optional.empty();
        }
    }

    private static final class NoOpMaximoRepository implements MaximoRepository {
        @Override
        public List<MaximoPerson> findPeopleByStatus(String status) {
            return Collections.emptyList();
        }

        @Override
        public java.util.Optional<MaximoPerson> findByPersonId(String personId) {
            return java.util.Optional.empty();
        }

        @Override
        public void saveOrUpdate(MaximoPerson person) {
            // no-op
        }
    }

    private static final class NoOpAuditRepository implements AuditRepository {
        @Override
        public void saveExecutionStart(SyncExecution execution) {
            // no-op
        }

        @Override
        public void saveExecutionEnd(SyncExecution execution) {
            // no-op
        }

        @Override
        public void saveIssue(String runId, SyncIssue issue) {
            // no-op
        }

        @Override
        public List<SyncIssue> findIssuesByRunId(String runId) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpMailService implements MailService {
        @Override
        public void sendExecutionSummary(SyncExecution execution, String body) {
            // no-op
        }
    }
}
