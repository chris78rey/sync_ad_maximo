package com.syncadmaximo.model;

import java.time.Instant;
import java.util.Objects;

public class SyncIssue {

    private String code;
    private String description;
    private String referenceId;
    private Instant createdAt = Instant.now();

    public SyncIssue() {
    }

    public SyncIssue(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SyncIssue{" +
                "code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", referenceId='" + referenceId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncIssue)) {
            return false;
        }
        SyncIssue syncIssue = (SyncIssue) o;
        return Objects.equals(code, syncIssue.code)
                && Objects.equals(description, syncIssue.description)
                && Objects.equals(referenceId, syncIssue.referenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, description, referenceId);
    }
}
