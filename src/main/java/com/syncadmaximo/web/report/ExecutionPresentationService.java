package com.syncadmaximo.web.report;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.util.StringSanitizer;

import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExecutionPresentationService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ZoneId zoneId;

    public ExecutionPresentationService(ZoneId zoneId) {
        this.zoneId = zoneId == null ? ZoneId.of("UTC") : zoneId;
    }

    public ExecutionPresentationModel toModel(SyncExecution execution, SyncResult result) {
        return new ExecutionPresentationModel(execution, result, zoneId);
    }

    public void writeHtml(Writer writer, ExecutionPresentationModel model, String contextPath) throws IOException {
        String safeContextPath = contextPath == null ? "" : contextPath;
        writer.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Sync AD Maximo - Reporte</title>");
        writer.write("<style>body{font-family:Arial,sans-serif;margin:24px;color:#222;}table{border-collapse:collapse;width:100%;margin:12px 0;}th,td{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top;}th{background:#f3f3f3;} .ok{color:#0a7a0a;font-weight:bold;} .err{color:#a00;font-weight:bold;} .muted{color:#666;} .nav a{margin-right:12px;}</style>");
        writer.write("</head><body>");
        writer.write("<h1>Reporte de ejecución</h1>");
        writer.write("<div class=\"nav\"><a href=\"");
        writer.write(escapeHtml(safeContextPath + "/report.csv"));
        writer.write("\">Descargar CSV</a><a href=\"");
        writer.write(escapeHtml(safeContextPath + "/mail/preview"));
        writer.write("\">Vista para correo</a><a href=\"");
        writer.write(escapeHtml(safeContextPath + "/logout"));
        writer.write("\">Salir</a></div>");

        writer.write("<table><tbody>");
        writeRow(writer, "Run ID", model.getRunId());
        writeRow(writer, "Proceso", model.getProcessName());
        writeRow(writer, "Iniciado por", model.getInitiatedBy());
        writeRow(writer, "Modo", model.isDryRun() ? "DRY RUN" : "EJECUCIÓN REAL");
        writeRow(writer, "Estado", model.getSummaryState());
        writeRow(writer, "Mensaje", model.getMessage());
        writeRow(writer, "Inicio", model.getStartedAtText());
        writeRow(writer, "Fin", model.getFinishedAtText());
        writeRow(writer, "Duración", model.getDurationText());
        writeRow(writer, "Total de issues", String.valueOf(model.getIssueCount()));
        writer.write("</tbody></table>");

        writer.write("<h2>Conteo por código</h2>");
        writer.write("<table><thead><tr><th>Código</th><th>Cuenta</th></tr></thead><tbody>");
        for (Map.Entry<String, Integer> entry : model.getIssueCountsByCode().entrySet()) {
            writer.write("<tr><td>");
            writer.write(escapeHtml(entry.getKey()));
            writer.write("</td><td>");
            writer.write(String.valueOf(entry.getValue()));
            writer.write("</td></tr>");
        }
        if (model.getIssueCountsByCode().isEmpty()) {
            writer.write("<tr><td colspan=\"2\" class=\"muted\">Sin issues</td></tr>");
        }
        writer.write("</tbody></table>");

        writer.write("<h2>Lista de issues</h2>");
        writer.write("<table><thead><tr><th>#</th><th>Código</th><th>Descripción</th><th>Referencia</th><th>Creado</th></tr></thead><tbody>");
        List<SyncIssue> issues = model.getIssues();
        for (int index = 0; index < issues.size(); index++) {
            SyncIssue issue = issues.get(index);
            writer.write("<tr><td>");
            writer.write(String.valueOf(index + 1));
            writer.write("</td><td>");
            writer.write(escapeHtml(issue == null ? null : issue.getCode()));
            writer.write("</td><td>");
            writer.write(escapeHtml(issue == null ? null : issue.getDescription()));
            writer.write("</td><td>");
            writer.write(escapeHtml(issue == null ? null : issue.getReferenceId()));
            writer.write("</td><td>");
            writer.write(escapeHtml(issue == null || issue.getCreatedAt() == null ? null : ISO.format(issue.getCreatedAt().atZone(zoneId))));
            writer.write("</td></tr>");
        }
        if (issues.isEmpty()) {
            writer.write("<tr><td colspan=\"5\" class=\"muted\">Sin issues</td></tr>");
        }
        writer.write("</tbody></table>");
        writer.write("</body></html>");
    }

    public void writeCsv(Writer writer, ExecutionPresentationModel model) throws IOException {
        writer.write("section,key,value\n");
        writeCsvRow(writer, "summary", "runId", model.getRunId());
        writeCsvRow(writer, "summary", "processName", model.getProcessName());
        writeCsvRow(writer, "summary", "initiatedBy", model.getInitiatedBy());
        writeCsvRow(writer, "summary", "dryRun", String.valueOf(model.isDryRun()));
        writeCsvRow(writer, "summary", "state", model.getSummaryState());
        writeCsvRow(writer, "summary", "message", model.getMessage());
        writeCsvRow(writer, "summary", "startedAt", model.getStartedAtText());
        writeCsvRow(writer, "summary", "finishedAt", model.getFinishedAtText());
        writeCsvRow(writer, "summary", "duration", model.getDurationText());
        writeCsvRow(writer, "summary", "issueCount", String.valueOf(model.getIssueCount()));
        for (Map.Entry<String, Integer> entry : model.getIssueCountsByCode().entrySet()) {
            writeCsvRow(writer, "countByCode", entry.getKey(), String.valueOf(entry.getValue()));
        }
        writer.write("section,index,code,description,referenceId,createdAt\n");
        List<SyncIssue> issues = model.getIssues();
        for (int index = 0; index < issues.size(); index++) {
            SyncIssue issue = issues.get(index);
            writer.write(escapeCsv("issue"));
            writer.write(',');
            writer.write(escapeCsv(String.valueOf(index + 1)));
            writer.write(',');
            writer.write(escapeCsv(issue == null ? null : issue.getCode()));
            writer.write(',');
            writer.write(escapeCsv(issue == null ? null : issue.getDescription()));
            writer.write(',');
            writer.write(escapeCsv(issue == null ? null : issue.getReferenceId()));
            writer.write(',');
            writer.write(escapeCsv(issue == null || issue.getCreatedAt() == null ? null : ISO.format(issue.getCreatedAt().atZone(zoneId))));
            writer.write('\n');
        }
    }

    public String renderMailBody(ExecutionPresentationModel model) {
        StringBuilder builder = new StringBuilder(1024);
        builder.append("Sync AD Maximo - Resumen de ejecución\n");
        builder.append("Run ID: ").append(nullToDash(model.getRunId())).append('\n');
        builder.append("Proceso: ").append(nullToDash(model.getProcessName())).append('\n');
        builder.append("Iniciado por: ").append(nullToDash(model.getInitiatedBy())).append('\n');
        builder.append("Modo: ").append(model.isDryRun() ? "DRY RUN" : "EJECUCIÓN REAL").append('\n');
        builder.append("Estado: ").append(model.getSummaryState()).append('\n');
        builder.append("Mensaje: ").append(nullToDash(model.getMessage())).append('\n');
        builder.append("Inicio: ").append(model.getStartedAtText()).append('\n');
        builder.append("Fin: ").append(model.getFinishedAtText()).append('\n');
        builder.append("Duración: ").append(model.getDurationText()).append('\n');
        builder.append("Total issues: ").append(model.getIssueCount()).append('\n');
        builder.append('\n');
        builder.append("Conteo por código:\n");
        for (Map.Entry<String, Integer> entry : model.getIssueCountsByCode().entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        if (model.getIssueCountsByCode().isEmpty()) {
            builder.append("- Sin issues\n");
        }
        builder.append('\n');
        builder.append("Issues:\n");
        List<SyncIssue> issues = model.getIssues();
        for (int index = 0; index < issues.size(); index++) {
            SyncIssue issue = issues.get(index);
            builder.append(index + 1).append(") ");
            builder.append(nullToDash(issue == null ? null : issue.getCode()));
            builder.append(" | ");
            builder.append(nullToDash(issue == null ? null : issue.getDescription()));
            builder.append(" | ref=").append(nullToDash(issue == null ? null : issue.getReferenceId()));
            builder.append(" | created=").append(issue == null || issue.getCreatedAt() == null ? "-" : ISO.format(issue.getCreatedAt().atZone(zoneId)));
            builder.append('\n');
        }
        if (issues.isEmpty()) {
            builder.append("Sin issues\n");
        }
        return builder.toString();
    }

    private static void writeRow(Writer writer, String key, String value) throws IOException {
        writer.write("<tr><th>");
        writer.write(escapeHtml(key));
        writer.write("</th><td>");
        writer.write(escapeHtml(value));
        writer.write("</td></tr>");
    }

    private static void writeCsvRow(Writer writer, String section, String key, String value) throws IOException {
        writer.write(escapeCsv(section));
        writer.write(',');
        writer.write(escapeCsv(key));
        writer.write(',');
        writer.write(escapeCsv(value));
        writer.write('\n');
    }

    private static String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&': builder.append("&amp;"); break;
                case '<': builder.append("&lt;"); break;
                case '>': builder.append("&gt;"); break;
                case '"': builder.append("&quot;"); break;
                case '\'': builder.append("&#39;"); break;
                default: builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        String escaped = safe.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
