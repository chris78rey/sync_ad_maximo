package com.syncadmaximo.web.servlet;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.service.ReportService;
import com.syncadmaximo.web.report.ExecutionPresentationModel;
import com.syncadmaximo.web.report.PresentationRequestResolver;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ReportService reportService = new ReportService(AppConfig.getInstance().getZoneId());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SyncExecution execution = PresentationRequestResolver.resolveExecution(request);
        SyncResult result = PresentationRequestResolver.resolveResult(request);
        ExecutionPresentationModel model = reportService.toModel(execution, result);

        boolean csv = isCsvRequest(request);
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

    private static boolean isCsvRequest(HttpServletRequest request) {
        String format = request.getParameter("format");
        if (format != null && format.trim().equalsIgnoreCase("csv")) {
            return true;
        }
        String path = request.getServletPath();
        return path != null && path.toLowerCase().endsWith(".csv");
    }
}
