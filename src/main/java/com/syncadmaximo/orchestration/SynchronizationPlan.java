package com.syncadmaximo.orchestration;

import com.syncadmaximo.model.SyncIssue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plan y resumen de una ejecución de sincronización.
 */
public final class SynchronizationPlan {

    private final List<SyncIssue> issues = new ArrayList<SyncIssue>();
    private int adLoaded;
    private int maximoLoaded;
    private int validatedAccepted;
    private int validatedReview;
    private int validatedRejected;
    private int matchedByUser;
    private int matchedByCedula;
    private int matchedByEmail;
    private int created;
    private int updated;
    private int unchanged;
    private int skipped;
    private int failed;

    public List<SyncIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void addIssue(SyncIssue issue) {
        if (issue != null) {
            issues.add(issue);
        }
    }

    public int getAdLoaded() {
        return adLoaded;
    }

    public void setAdLoaded(int adLoaded) {
        this.adLoaded = adLoaded;
    }

    public int getMaximoLoaded() {
        return maximoLoaded;
    }

    public void setMaximoLoaded(int maximoLoaded) {
        this.maximoLoaded = maximoLoaded;
    }

    public int getValidatedAccepted() {
        return validatedAccepted;
    }

    public void setValidatedAccepted(int validatedAccepted) {
        this.validatedAccepted = validatedAccepted;
    }

    public int getValidatedReview() {
        return validatedReview;
    }

    public void setValidatedReview(int validatedReview) {
        this.validatedReview = validatedReview;
    }

    public int getValidatedRejected() {
        return validatedRejected;
    }

    public void setValidatedRejected(int validatedRejected) {
        this.validatedRejected = validatedRejected;
    }

    public int getMatchedByUser() {
        return matchedByUser;
    }

    public void incrementMatchedByUser() {
        this.matchedByUser++;
    }

    public int getMatchedByCedula() {
        return matchedByCedula;
    }

    public void incrementMatchedByCedula() {
        this.matchedByCedula++;
    }

    public int getMatchedByEmail() {
        return matchedByEmail;
    }

    public void incrementMatchedByEmail() {
        this.matchedByEmail++;
    }

    public int getCreated() {
        return created;
    }

    public void incrementCreated() {
        this.created++;
    }

    public int getUpdated() {
        return updated;
    }

    public void incrementUpdated() {
        this.updated++;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public void incrementUnchanged() {
        this.unchanged++;
    }

    public int getSkipped() {
        return skipped;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public int getFailed() {
        return failed;
    }

    public void incrementFailed() {
        this.failed++;
    }

    public String buildSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("AD=").append(adLoaded)
                .append(", MAXIMO=").append(maximoLoaded)
                .append(", validos=").append(validatedAccepted)
                .append(", revisar=").append(validatedReview)
                .append(", rechazados=").append(validatedRejected)
                .append(", matchUsuario=").append(matchedByUser)
                .append(", matchCedula=").append(matchedByCedula)
                .append(", matchCorreo=").append(matchedByEmail)
                .append(", creados=").append(created)
                .append(", actualizados=").append(updated)
                .append(", sinCambios=").append(unchanged)
                .append(", omitidos=").append(skipped)
                .append(", fallidos=").append(failed)
                .append(", issues=").append(issues.size());
        return builder.toString();
    }
}
