package com.syncadmaximo.web.servlet;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.service.DirectoryService;
import com.syncadmaximo.web.WebRequestContext;
import com.syncadmaximo.web.WebSessionKeys;
import com.syncadmaximo.web.security.DirectoryBackedWebAuthenticator;
import com.syncadmaximo.web.security.WebAuthenticator;
import com.syncadmaximo.web.security.WebPrincipal;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        renderLoginPage(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String redirect = sanitizeRedirectTarget(request.getParameter("redirect"), request.getContextPath());
        WebAuthenticator authenticator = resolveAuthenticator(getServletContext());
        Optional<WebPrincipal> principal = authenticator.authenticate(username, password);
        if (!principal.isPresent()) {
            renderLoginPage(request, response, "Usuario o contraseña inválidos, o acceso no autorizado.");
            return;
        }

        HttpSession session = request.getSession(true);
        try {
            request.changeSessionId();
        } catch (IllegalStateException ignored) {
            // Session not yet established in some containers; safe to continue.
        }
        session.setAttribute(WebSessionKeys.PRINCIPAL, principal.get());
        SyncExecution execution = (SyncExecution) session.getAttribute(WebSessionKeys.LAST_EXECUTION);
        SyncResult result = (SyncResult) session.getAttribute(WebSessionKeys.LAST_RESULT);
        if (execution != null || result != null) {
            WebRequestContext.remember(request, execution, result);
        }
        response.sendRedirect(redirect);
    }

    private void renderLoginPage(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");

        String redirect = sanitizeRedirectTarget(request.getParameter("redirect"), request.getContextPath());
        String flashError = errorMessage;
        HttpSession session = request.getSession(false);
        if (flashError == null && session != null) {
            Object value = session.getAttribute(WebSessionKeys.FLASH_ERROR);
            if (value instanceof String) {
                flashError = (String) value;
                session.removeAttribute(WebSessionKeys.FLASH_ERROR);
            }
        }

        PrintWriter out = response.getWriter();
        out.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Sync AD Maximo - Login</title>");
        out.write("<style>body{font-family:Arial,sans-serif;background:#f7f7f7;margin:0;padding:0;} .wrap{max-width:420px;margin:10vh auto;background:#fff;padding:24px;border:1px solid #ddd;border-radius:8px;} label{display:block;margin-top:12px;} input{width:100%;padding:10px;box-sizing:border-box;} button{margin-top:18px;padding:10px 14px;} .error{background:#ffe8e8;color:#a00;padding:10px;border:1px solid #f2b3b3;border-radius:4px;} .muted{color:#666;font-size:0.95em;}</style>");
        out.write("</head><body><div class=\"wrap\"><h1>Ingreso</h1>");
        if (flashError != null && !flashError.trim().isEmpty()) {
            out.write("<div class=\"error\">");
            out.write(escapeHtml(flashError));
            out.write("</div>");
        }
        out.write("<form method=\"post\" action=\"");
        out.write(escapeHtml(request.getContextPath() + "/login"));
        out.write("\">");
        out.write("<input type=\"hidden\" name=\"redirect\" value=\"");
        out.write(escapeHtml(redirect));
        out.write("\"/>");
        out.write("<label for=\"username\">Usuario</label><input id=\"username\" name=\"username\" type=\"text\" autocomplete=\"username\" required />");
        out.write("<label for=\"password\">Contraseña</label><input id=\"password\" name=\"password\" type=\"password\" autocomplete=\"current-password\" required />");
        out.write("<button type=\"submit\">Ingresar</button></form>");
        out.write("<p class=\"muted\">Acceso funcional restringido a maxadmin.</p>");
        out.write("<p><a href=\"");
        out.write(escapeHtml(request.getContextPath() + "/demo"));
        out.write("\">Probar el modo demo completo</a></p>");
        out.write("<p><a href=\"");
        out.write(escapeHtml(request.getContextPath() + "/health"));
        out.write("\">Estado del servicio</a></p>");
        out.write("</div></body></html>");
    }

    private static WebAuthenticator resolveAuthenticator(ServletContext servletContext) {
        Object provided = servletContext.getAttribute("webAuthenticator");
        if (provided instanceof WebAuthenticator) {
            return (WebAuthenticator) provided;
        }
        DirectoryService directoryService = resolveDirectoryService(servletContext);
        return new DirectoryBackedWebAuthenticator(directoryService, AppConfig.getInstance().getAllowedUser());
    }

    private static DirectoryService resolveDirectoryService(ServletContext servletContext) {
        Object provided = servletContext.getAttribute("directoryService");
        if (provided instanceof DirectoryService) {
            return (DirectoryService) provided;
        }
        provided = servletContext.getAttribute("syncDirectoryService");
        if (provided instanceof DirectoryService) {
            return (DirectoryService) provided;
        }
        return null;
    }

    private static String sanitizeRedirectTarget(String redirect, String contextPath) {
        String defaultTarget = contextPath + "/report";
        if (redirect == null) {
            return defaultTarget;
        }
        String trimmed = redirect.trim();
        if (trimmed.isEmpty()) {
            return defaultTarget;
        }
        if (trimmed.contains("://") || trimmed.contains("\\") || trimmed.contains("..")) {
            return defaultTarget;
        }
        if (!trimmed.startsWith("/")) {
            return defaultTarget;
        }
        if (trimmed.startsWith(contextPath)) {
            return trimmed;
        }
        return contextPath + trimmed;
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
