package com.syncadmaximo.web.servlet;

import com.syncadmaximo.audit.RunRecord;
import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.service.DailyReportEmailService;
import com.syncadmaximo.service.ReportService;
import com.syncadmaximo.service.RunHistoryService;
import com.syncadmaximo.web.report.ExecutionPresentationModel;
import com.syncadmaximo.web.report.PresentationRequestResolver;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controlador web para reporte actual, historial y reenvio de correo.
 */
public class ReportController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String HISTORY_PREFIX = "/historial";
    private static final String HISTORY_SEND_SUFFIX = "/reenviar";
    private static final DateTimeFormatter HISTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.ROOT);

    private final ZoneId zoneId = AppConfig.getInstance().getZoneId();
    private final ReportService reportService = new ReportService(zoneId);
    private final DailyReportEmailService dailyReportEmailService = new DailyReportEmailService(null);
    private final RunHistoryService runHistoryService = new RunHistoryService(AppConfig.getInstance());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HistoryRoute historyRoute = parseHistoryRoute(request.getPathInfo());
        if (historyRoute != null && historyRoute.isList()) {
            renderHistoryList(request, response, historyRoute.isCsv());
            return;
        }

        if (historyRoute != null && historyRoute.getRunId() != null) {
            HistoricalData historical = resolveHistoricalData(historyRoute.getRunId(), false, request);
            if (historical.execution == null || historical.result == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe la ejecucion solicitada.");
                return;
            }
            renderExecution(request, response, historical.execution, historical.result, historyRoute.isCsv());
            return;
        }

        HistoricalData data = resolveData(request);
        renderExecution(request, response, data.execution, data.result, isCsvRequest(request, request.getPathInfo()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HistoryRoute historyRoute = parseHistoryRoute(request.getPathInfo());
        if (historyRoute != null && historyRoute.isResend() && historyRoute.getRunId() != null) {
            HistoricalData historical = resolveHistoricalData(historyRoute.getRunId(), false, request);
            if (historical.execution == null || historical.result == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe la ejecucion solicitada.");
                return;
            }
            dailyReportEmailService.resendExecutionSummary(historical.execution, historical.result);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"OK\",\"message\":\"Correo reenviado desde el historial.\"}");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.contains("/enviar-correo")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Accion no soportada.");
            return;
        }

        HistoricalData data = resolveData(request);
        if (data.execution == null || data.result == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe una ejecucion disponible en la sesion.");
            return;
        }

        dailyReportEmailService.resendExecutionSummary(data.execution, data.result);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"OK\",\"message\":\"Correo reenviado desde la ejecucion en sesion.\"}");
    }

    private void renderExecution(HttpServletRequest request, HttpServletResponse response, SyncExecution execution, SyncResult result, boolean csv) throws IOException {
        if (execution == null || result == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe una ejecucion disponible.");
            return;
        }

        ExecutionPresentationModel model = reportService.toModel(execution, result);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        if (csv) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"sync-ad-maximo-report.csv\"");
            PrintWriter out = response.getWriter();
            reportService.writeCsv(out, model);
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        reportService.writeHtml(out, model, request.getContextPath());
    }

    private void renderHistoryList(HttpServletRequest request, HttpServletResponse response, boolean csv) throws IOException {
        int limit = parseLimit(request.getParameter("limit"));
        List<RunRecord> runs = runHistoryService.findRecentRuns(limit);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        if (csv) {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"sync-ad-maximo-history.csv\"");
            writeHistoryCsv(response.getWriter(), runs);
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        writeHistoryHtml(response.getWriter(), runs, request.getContextPath());
    }

    private HistoricalData resolveData(HttpServletRequest request) {
        String runId = request.getParameter("runId");
        if (runId != null && !runId.trim().isEmpty()) {
            return resolveHistoricalData(runId, true, request);
        }
        return new HistoricalData(PresentationRequestResolver.resolveExecution(request), PresentationRequestResolver.resolveResult(request));
    }

    private HistoricalData resolveHistoricalData(String runId, boolean allowFallback, HttpServletRequest request) {
        Optional<RunHistoryService.HistoricalExecution> historical = runHistoryService.findExecutionByRunId(runId);
        if (historical.isPresent()) {
            return new HistoricalData(historical.get().getExecution(), historical.get().getResult());
        }
        if (allowFallback) {
            return new HistoricalData(PresentationRequestResolver.resolveExecution(request), PresentationRequestResolver.resolveResult(request));
        }
        return new HistoricalData(null, null);
    }

    private static boolean isCsvRequest(HttpServletRequest request, String pathInfo) {
        String format = request.getParameter("format");
        if (format != null && format.trim().equalsIgnoreCase("csv")) {
            return true;
        }
        String path = request.getServletPath();
        if (path != null && path.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return true;
        }
        return pathInfo != null && pathInfo.toLowerCase(Locale.ROOT).endsWith("/csv");
    }

    private static int parseLimit(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 20;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1) {
                return 1;
            }
            return Math.min(parsed, 100);
        } catch (NumberFormatException ex) {
            return 20;
        }
    }

    static void writeHistoryHtml(PrintWriter out, List<RunRecord> runs, String contextPath) {
        String safeContext = contextPath == null ? "" : contextPath;
        out.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Sync AD Maximo - Historial</title>");
        out.write("<style>body{font-family:Arial,sans-serif;margin:24px;color:#222;}table{border-collapse:collapse;width:100%;margin:12px 0;}th,td{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top;}th{background:#f3f3f3;} .muted{color:#666;} .nav a{margin-right:12px;} .actions a,.actions button{margin-right:8px;}</style>");
        out.write("</head><body>");
        out.write("<h1>Historial de ejecuciones</h1>");
        out.write("<div class=\"nav\"><a href=\"");
        out.write(escapeHtml(safeContext + "/api/reportes/historial.csv"));
        out.write("\">Descargar CSV</a><a href=\"");
        out.write(escapeHtml(safeContext + "/report"));
        out.write("\">Volver al reporte actual</a></div>");
        out.write("<table><thead><tr><th>Run ID</th><th>Inicio</th><th>Fin</th><th>Proceso</th><th>Modo</th><th>Estado</th><th>Usuario</th><th>Resumen</th><th>Acciones</th></tr></thead><tbody>");
        if (runs == null || runs.isEmpty()) {
            out.write("<tr><td colspan=\"9\" class=\"muted\">Sin ejecuciones registradas</td></tr>");
        } else {
            for (RunRecord run : runs) {
                String runId = run == null || run.getRunId() == null ? "" : String.valueOf(run.getRunId());
                String detailUrl = safeContext + "/api/reportes/historial/" + runId;
                String csvUrl = detailUrl + ".csv";
                out.write("<tr><td>");
                out.write(escapeHtml(runId));
                out.write("</td><td>");
                out.write(escapeHtml(formatInstant(run == null ? null : run.getFechaInicio())));
                out.write("</td><td>");
                out.write(escapeHtml(formatInstant(run == null ? null : run.getFechaFin())));
                out.write("</td><td>");
                out.write(escapeHtml(run == null ? null : run.getProceso()));
                out.write("</td><td>");
                out.write(escapeHtml(run == null ? null : run.getModo()));
                out.write("</td><td>");
                out.write(escapeHtml(run == null ? null : run.getEstado()));
                out.write("</td><td>");
                out.write(escapeHtml(run == null ? null : run.getUsuarioEjecutor()));
                out.write("</td><td>");
                out.write(escapeHtml(run == null ? null : run.getMensaje()));
                out.write("</td><td class=\"actions\"><a href=\"");
                out.write(escapeHtml(detailUrl));
                out.write("\">Ver</a><a href=\"");
                out.write(escapeHtml(csvUrl));
                out.write("\">CSV</a><form method=\"post\" action=\"");
                out.write(escapeHtml(detailUrl + "/reenviar"));
                out.write("\" style=\"display:inline\"><button type=\"submit\">Reenviar</button></form></td></tr>");
            }
        }
        out.write("</tbody></table>");
        out.write("</body></html>");
    }

    static void writeHistoryCsv(PrintWriter out, List<RunRecord> runs) {
        out.write("runId,fechaInicio,fechaFin,modo,proceso,usuarioEjecutor,origenEjecucion,estado,totalMaximo,totalAd,totalMigrados,totalCreados,totalInactivados,totalEmailActualizados,totalEmailInsertados,totalSinCambios,totalObservados,totalErrores,mensaje\n");
        if (runs == null) {
            return;
        }
        for (RunRecord run : runs) {
            if (run == null) {
                continue;
            }
            out.write(csv(run.getRunId() == null ? null : String.valueOf(run.getRunId())));
            out.write(',');
            out.write(csv(formatInstant(run.getFechaInicio())));
            out.write(',');
            out.write(csv(formatInstant(run.getFechaFin())));
            out.write(',');
            out.write(csv(run.getModo()));
            out.write(',');
            out.write(csv(run.getProceso()));
            out.write(',');
            out.write(csv(run.getUsuarioEjecutor()));
            out.write(',');
            out.write(csv(run.getOrigenEjecucion()));
            out.write(',');
            out.write(csv(run.getEstado()));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalMaximo())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalAd())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalMigrados())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalCreados())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalInactivados())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalEmailActualizados())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalEmailInsertados())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalSinCambios())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalObservados())));
            out.write(',');
            out.write(csv(String.valueOf(run.getTotalErrores())));
            out.write(',');
            out.write(csv(run.getMensaje()));
            out.write('\n');
        }
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        return HISTORY_FORMAT.format(instant.atZone(ZoneId.of("UTC")));
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\'':
                    builder.append("&#39;");
                    break;
                default:
                    builder.append(ch);
            }
        }
        return builder.toString();
    }

    private HistoryRoute parseHistoryRoute(String pathInfo) {
        if (pathInfo == null || pathInfo.trim().isEmpty() || "/".equals(pathInfo.trim())) {
            return null;
        }
        String normalized = pathInfo.trim();
        if (normalized.equals(HISTORY_PREFIX) || normalized.equals(HISTORY_PREFIX + "/")) {
            return HistoryRoute.list(false);
        }
        if ((HISTORY_PREFIX + ".csv").equalsIgnoreCase(normalized)) {
            return HistoryRoute.list(true);
        }
        if (!normalized.startsWith(HISTORY_PREFIX + "/")) {
            return null;
        }
        String tail = normalized.substring(HISTORY_PREFIX.length() + 1);
        if (tail.endsWith(HISTORY_SEND_SUFFIX)) {
            String runId = tail.substring(0, tail.length() - HISTORY_SEND_SUFFIX.length());
            return HistoryRoute.resend(runId);
        }
        if (tail.endsWith(".csv")) {
            String runId = tail.substring(0, tail.length() - 4);
            return HistoryRoute.detail(runId, true);
        }
        return HistoryRoute.detail(tail, false);
    }

    private static final class HistoricalData {
        private final SyncExecution execution;
        private final SyncResult result;

        private HistoricalData(SyncExecution execution, SyncResult result) {
            this.execution = execution;
            this.result = result;
        }
    }

    private static final class HistoryRoute {
        private final boolean list;
        private final boolean resend;
        private final String runId;
        private final boolean csv;

        private HistoryRoute(boolean list, boolean resend, String runId, boolean csv) {
            this.list = list;
            this.resend = resend;
            this.runId = runId;
            this.csv = csv;
        }

        static HistoryRoute list(boolean csv) {
            return new HistoryRoute(true, false, null, csv);
        }

        static HistoryRoute detail(String runId, boolean csv) {
            return new HistoryRoute(false, false, runId, csv);
        }

        static HistoryRoute resend(String runId) {
            return new HistoryRoute(false, true, runId, false);
        }

        boolean isList() {
            return list;
        }

        boolean isResend() {
            return resend;
        }

        String getRunId() {
            return runId;
        }

        boolean isCsv() {
            return csv;
        }
    }
}
