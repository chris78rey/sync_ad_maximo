package com.syncadmaximo.web.servlet;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.service.DailyReportEmailService;
import com.syncadmaximo.service.ReportService;
import com.syncadmaximo.web.WebRequestContext;
import com.syncadmaximo.web.report.ExecutionPresentationModel;
import com.syncadmaximo.web.report.PresentationRequestResolver;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Controlador web para reporte actual, CSV y reenvío de correo.
 */
public class ReportController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ReportService reportService = new ReportService(AppConfig.getInstance().getZoneId());
    private final DailyReportEmailService dailyReportEmailService = new DailyReportEmailService(null);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SyncExecution execution = PresentationRequestResolver.resolveExecution(request);
        SyncResult result = PresentationRequestResolver.resolveResult(request);
        if (execution == null || result == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe una ejecución disponible en la sesión.");
            return;
        }

        ExecutionPresentationModel model = reportService.toModel(execution, result);
        String pathInfo = request.getPathInfo();
        boolean csv = isCsvRequest(request, pathInfo);

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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.contains("/enviar-correo")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Acción no soportada.");
            return;
        }

        SyncExecution execution = WebRequestContext.resolveExecution(request);
        SyncResult result = WebRequestContext.resolveResult(request);
        if (execution == null || result == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No existe una ejecución disponible en la sesión.");
            return;
        }

        dailyReportEmailService.resendExecutionSummary(execution, result);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"OK\",\"message\":\"Correo reenviado desde la ejecución en sesión.\"}");
    }

    private static boolean isCsvRequest(HttpServletRequest request, String pathInfo) {
        String format = request.getParameter("format");
        if (format != null && format.trim().equalsIgnoreCase("csv")) {
            return true;
        }
        String path = request.getServletPath();
        if (path != null && path.toLowerCase().endsWith(".csv")) {
            return true;
        }
        return pathInfo != null && pathInfo.toLowerCase().endsWith("/csv");
    }
}
