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

public class MailPreviewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ReportService reportService = new ReportService(AppConfig.getInstance().getZoneId());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SyncExecution execution = PresentationRequestResolver.resolveExecution(request);
        SyncResult result = PresentationRequestResolver.resolveResult(request);
        ExecutionPresentationModel model = reportService.toModel(execution, result);
        String body = reportService.renderMailBody(model);

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        if (isTextRequest(request)) {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(body);
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Sync AD Maximo - Vista de correo</title>");
        out.write("<style>body{font-family:Arial,sans-serif;margin:24px;} pre{white-space:pre-wrap;background:#f7f7f7;border:1px solid #ddd;padding:16px;}</style>");
        out.write("</head><body><h1>Vista de correo</h1><p><a href=\"");
        out.write(escapeHtml(request.getContextPath() + "/report"));
        out.write("\">Volver al reporte</a></p><pre>");
        out.write(escapeHtml(body));
        out.write("</pre></body></html>");
    }

    private static boolean isTextRequest(HttpServletRequest request) {
        String format = request.getParameter("format");
        if (format != null && (format.trim().equalsIgnoreCase("txt") || format.trim().equalsIgnoreCase("text"))) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains("text/plain");
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
}
