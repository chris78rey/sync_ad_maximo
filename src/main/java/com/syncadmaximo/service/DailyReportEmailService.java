package com.syncadmaximo.service;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.orchestration.SynchronizationPlan;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Construye y envía el resumen diario de una ejecución.
 */
public class DailyReportEmailService {

    private static final Logger LOGGER = Logger.getLogger(DailyReportEmailService.class.getName());

    private final MailService mailService;

    public DailyReportEmailService(MailService mailService) {
        this.mailService = mailService == null ? new NoOpMailService() : mailService;
    }

    public void sendExecutionSummary(SyncExecution execution, SynchronizationPlan plan, SyncResult result) {
        try {
            mailService.sendExecutionSummary(execution, buildMailBody(execution, plan, result));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.FINE, "No se pudo enviar el resumen por correo", ex);
        }
    }

    public String buildMailBody(SyncExecution execution, SynchronizationPlan plan, SyncResult result) {
        StringBuilder body = new StringBuilder();
        body.append("RunId: ").append(execution == null ? "-" : execution.getRunId()).append('\n');
        body.append("Modo: ").append(execution != null && execution.isDryRun() ? "DRY_RUN" : "PRODUCTION").append('\n');
        body.append("Proceso: ").append(execution == null ? "-" : execution.getProcessName()).append('\n');
        body.append("Inicio: ").append(execution == null ? "-" : execution.getStartedAt()).append('\n');
        body.append("Fin: ").append(execution == null ? "-" : execution.getFinishedAt()).append('\n');
        body.append('\n').append(plan == null ? "-" : plan.buildSummary()).append('\n');
        if (result != null && !result.getIssues().isEmpty()) {
            body.append('\n').append("Issues:\n");
            for (SyncIssue issue : result.getIssues()) {
                body.append("- ").append(issue == null ? "-" : issue.getCode()).append(" | ")
                        .append(issue == null ? "-" : issue.getReferenceId()).append(" | ")
                        .append(issue == null ? "-" : issue.getDescription()).append('\n');
            }
        }
        return body.toString();
    }

    private static final class NoOpMailService implements MailService {
        @Override
        public void sendExecutionSummary(SyncExecution execution, String body) {
            // no-op
        }
    }
}
