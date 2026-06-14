package com.syncadmaximo.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.syncadmaximo.ldap.LdapUser;

/**
 * Resultado detallado de una validación sobre un usuario LDAP.
 */
public class ValidationReport {

    public enum Status {
        ACCEPTED,
        REVIEW,
        REJECTED
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }

    public static final class Issue {
        private final Severity severity;
        private final String field;
        private final String code;
        private final String message;
        private final String originalValue;
        private final String normalizedValue;

        public Issue(Severity severity,
                     String field,
                     String code,
                     String message,
                     String originalValue,
                     String normalizedValue) {
            this.severity = severity;
            this.field = field;
            this.code = code;
            this.message = message;
            this.originalValue = originalValue;
            this.normalizedValue = normalizedValue;
        }

        public Severity getSeverity() {
            return severity;
        }

        public String getField() {
            return field;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getOriginalValue() {
            return originalValue;
        }

        public String getNormalizedValue() {
            return normalizedValue;
        }
    }

    private final LdapUser sourceUser;
    private final List<Issue> issues = new ArrayList<Issue>();

    private String normalizedCedula;
    private String normalizedUser;
    private String normalizedEmail;
    private boolean identityBlocked;
    private boolean emailBlocked;
    private boolean manualReview;
    private boolean duplicate;
    private boolean conflict;
    private String cedulaStrategyApplied;

    public ValidationReport(LdapUser sourceUser) {
        this.sourceUser = sourceUser;
    }

    public LdapUser getSourceUser() {
        return sourceUser;
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public String getNormalizedCedula() {
        return normalizedCedula;
    }

    public void setNormalizedCedula(String normalizedCedula) {
        this.normalizedCedula = normalizedCedula;
    }

    public String getNormalizedUser() {
        return normalizedUser;
    }

    public void setNormalizedUser(String normalizedUser) {
        this.normalizedUser = normalizedUser;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public void setNormalizedEmail(String normalizedEmail) {
        this.normalizedEmail = normalizedEmail;
    }

    public String getCedulaStrategyApplied() {
        return cedulaStrategyApplied;
    }

    public void setCedulaStrategyApplied(String cedulaStrategyApplied) {
        this.cedulaStrategyApplied = cedulaStrategyApplied;
    }

    public boolean isIdentityBlocked() {
        return identityBlocked;
    }

    public boolean isEmailBlocked() {
        return emailBlocked;
    }

    public boolean isManualReview() {
        return manualReview;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void addInfo(String field,
                        String code,
                        String message,
                        String originalValue,
                        String normalizedValue) {
        issues.add(new Issue(Severity.INFO, field, code, message, originalValue, normalizedValue));
    }

    public void addWarning(String field,
                          String code,
                          String message,
                          String originalValue,
                          String normalizedValue) {
        issues.add(new Issue(Severity.WARNING, field, code, message, originalValue, normalizedValue));
        manualReview = true;
    }

    public void addError(String field,
                        String code,
                        String message,
                        String originalValue,
                        String normalizedValue) {
        issues.add(new Issue(Severity.ERROR, field, code, message, originalValue, normalizedValue));
    }

    public void blockIdentity(String field,
                             String code,
                             String message,
                             String originalValue,
                             String normalizedValue) {
        identityBlocked = true;
        addError(field, code, message, originalValue, normalizedValue);
    }

    public void blockEmail(String field,
                           String code,
                           String message,
                           String originalValue,
                           String normalizedValue) {
        emailBlocked = true;
        addWarning(field, code, message, originalValue, normalizedValue);
    }

    public void markEmailUnavailable(String field,
                                     String code,
                                     String message,
                                     String originalValue,
                                     String normalizedValue) {
        emailBlocked = true;
        addInfo(field, code, message, originalValue, normalizedValue);
    }

    public void markDuplicate(String field,
                              String code,
                              String message,
                              String originalValue,
                              String normalizedValue) {
        duplicate = true;
        manualReview = true;
        identityBlocked = true;
        addWarning(field, code, message, originalValue, normalizedValue);
    }

    public void markConflict(String field,
                             String code,
                             String message,
                             String originalValue,
                             String normalizedValue) {
        conflict = true;
        identityBlocked = true;
        addError(field, code, message, originalValue, normalizedValue);
    }

    public void requireManualReview(String field,
                                    String code,
                                    String message,
                                    String originalValue,
                                    String normalizedValue) {
        manualReview = true;
        addWarning(field, code, message, originalValue, normalizedValue);
    }

    public boolean canProcessIdentity() {
        return !identityBlocked;
    }

    public boolean canSyncEmail() {
        return !emailBlocked;
    }

    public Status getStatus() {
        if (identityBlocked) {
            return Status.REJECTED;
        }
        if (manualReview || duplicate || conflict) {
            return Status.REVIEW;
        }
        return Status.ACCEPTED;
    }
}
