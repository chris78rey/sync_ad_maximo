package com.syncadmaximo.service;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.testsupport.TestAssertions;
import com.syncadmaximo.web.report.ExecutionPresentationModel;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;

public final class ReportServiceTest {

    public static void runAll() throws Exception {
        rendersHtmlCsvAndMailBodyFromExecutionModel();
    }

    static void rendersHtmlCsvAndMailBodyFromExecutionModel() throws Exception {
        ReportService service = new ReportService(ZoneId.of("UTC"));
        ExecutionPresentationModel model = service.toModel(sampleExecution(), sampleResult());

        StringWriter html = new StringWriter();
        service.writeHtml(html, model, "/sync");
        TestAssertions.contains(html.toString(), "Reporte de", "Debe renderizar HTML");
        TestAssertions.contains(html.toString(), "/sync/report.csv", "Debe incluir el enlace CSV");
        TestAssertions.contains(html.toString(), "/sync/api/reportes/historial", "Debe incluir el enlace al historial");
        TestAssertions.contains(html.toString(), "/sync/mail/preview", "Debe incluir la vista de correo");

        StringWriter csv = new StringWriter();
        service.writeCsv(csv, model);
        TestAssertions.contains(csv.toString(), "section,key,value", "Debe renderizar CSV");
        TestAssertions.contains(csv.toString(), "\"summary\",\"runId\",\"run-report-1\"", "Debe incluir el resumen");
        TestAssertions.contains(csv.toString(), "\"issue\",\"1\",\"VALIDATION_REVIEW\"", "Debe incluir los issues");

        String mailBody = service.renderMailBody(model);
        TestAssertions.contains(mailBody, "Sync AD Maximo - Resumen de", "Debe renderizar el correo");
        TestAssertions.contains(mailBody, "Run ID: run-report-1", "Debe incluir el runId");
        TestAssertions.contains(mailBody, "DRY RUN", "Debe indicar el modo");
        TestAssertions.contains(mailBody, "VALIDATION_REVIEW", "Debe listar el issue");
    }

    private static SyncExecution sampleExecution() {
        SyncExecution execution = new SyncExecution();
        execution.setRunId("run-report-1");
        execution.setProcessName("sync-ad-maximo");
        execution.setInitiatedBy("tester");
        execution.setStartedAt(Instant.parse("2026-01-01T10:00:00Z"));
        execution.setFinishedAt(Instant.parse("2026-01-01T10:05:00Z"));
        execution.setDryRun(true);
        return execution;
    }

    private static SyncResult sampleResult() {
        SyncResult result = new SyncResult();
        result.setRunId("run-report-1");
        result.setSuccess(true);
        result.setMessage("Ejecucion simulada");
        SyncIssue issue = new SyncIssue();
        issue.setCode("VALIDATION_REVIEW");
        issue.setDescription("Revisar correo");
        issue.setReferenceId("jdoe");
        issue.setCreatedAt(Instant.parse("2026-01-01T10:01:00Z"));
        result.addIssue(issue);
        return result;
    }
}
