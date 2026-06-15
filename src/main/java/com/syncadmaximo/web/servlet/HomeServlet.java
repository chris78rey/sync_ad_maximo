package com.syncadmaximo.web.servlet;

import com.syncadmaximo.web.security.WebPrincipal;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        renderDashboard(out, request);
    }

    static void renderDashboard(PrintWriter out, HttpServletRequest request) {
        String contextPath = request == null ? "" : request.getContextPath();
        WebPrincipal principal = request != null && request.getUserPrincipal() instanceof WebPrincipal
                ? (WebPrincipal) request.getUserPrincipal()
                : null;

        out.write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Sync AD Maximo - Panel</title>");
        out.write("<style>");
        out.write("body{font-family:Arial,sans-serif;margin:0;background:linear-gradient(135deg,#102a43 0%,#243b53 48%,#f5f7fa 48%,#f5f7fa 100%);color:#102a43;}");
        out.write(".wrap{max-width:1180px;margin:0 auto;padding:32px 24px 48px;}");
        out.write(".hero{background:rgba(16,42,67,.94);color:#fff;border-radius:24px;padding:32px;box-shadow:0 20px 40px rgba(16,42,67,.2);}");
        out.write(".hero h1{margin:0 0 8px;font-size:34px;}");
        out.write(".hero p{margin:0;color:#d9e2ec;max-width:760px;line-height:1.6;}");
        out.write(".meta{margin-top:18px;display:flex;gap:12px;flex-wrap:wrap;}");
        out.write(".pill{background:rgba(255,255,255,.12);padding:8px 12px;border-radius:999px;font-size:13px;}");
        out.write(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px;margin-top:24px;}");
        out.write(".card{background:#fff;border-radius:18px;padding:18px 18px 16px;box-shadow:0 10px 24px rgba(16,42,67,.08);border:1px solid rgba(16,42,67,.08);}");
        out.write(".card h2{margin:0 0 8px;font-size:18px;}");
        out.write(".card p{margin:0 0 14px;color:#52606d;line-height:1.5;min-height:42px;}");
        out.write(".card a{display:inline-block;text-decoration:none;background:#102a43;color:#fff;padding:10px 14px;border-radius:10px;font-weight:bold;}");
        out.write(".secondary{background:#d9e2ec;color:#102a43;}");
        out.write(".section{margin-top:28px;background:#fff;border-radius:18px;padding:20px;box-shadow:0 10px 24px rgba(16,42,67,.08);}");
        out.write(".section h2{margin-top:0;}");
        out.write(".list{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px;}");
        out.write(".item{border:1px solid #d9e2ec;border-radius:12px;padding:14px;background:#f8fafc;}");
        out.write(".item strong{display:block;margin-bottom:6px;}");
        out.write(".item span{color:#52606d;line-height:1.5;}");
        out.write("</style></head><body><div class=\"wrap\">");
        out.write("<section class=\"hero\"><h1>Sync AD Maximo</h1>");
        out.write("<p>Panel unico para operar la sincronizacion, revisar ejecuciones, abrir reportes, exportar CSV y consultar el historial sin navegar pantallas sueltas.</p>");
        out.write("<div class=\"meta\">");
        out.write("<span class=\"pill\">Usuario: ");
        out.write(escapeHtml(principal == null ? "maxadmin" : principal.getDisplayName()));
        out.write("</span>");
        out.write("<span class=\"pill\">Menu central</span>");
        out.write("<span class=\"pill\">Tomcat 9</span>");
        out.write("</div></section>");

        out.write("<div class=\"grid\">");
        writeCard(out, "Reporte actual", "Abre el resumen de la ultima ejecucion disponible en sesion o por runId.", contextPath + "/report", "Abrir reporte");
        writeCard(out, "Historial", "Lista ejecuciones recientes con acceso a detalle, CSV y reenvio.", contextPath + "/api/reportes/historial", "Ver historial");
        writeCard(out, "CSV actual", "Descarga el reporte actual en formato CSV.", contextPath + "/report.csv", "Descargar CSV");
        writeCard(out, "CSV historial", "Exporta el historial de ejecuciones a CSV.", contextPath + "/api/reportes/historial.csv", "Descargar historial");
        writeCard(out, "Vista correo", "Previsualiza el cuerpo del correo de la ejecucion actual.", contextPath + "/mail/preview", "Abrir vista");
        writeCard(out, "Health", "Verifica el estado basico de la aplicacion.", contextPath + "/health", "Ver health");
        writeCard(out, "Demo", "Abre una ejecucion demo para pruebas locales.", contextPath + "/demo", "Abrir demo");
        writeCard(out, "Salir", "Cierra la sesion del usuario autenticado.", contextPath + "/logout", "Cerrar sesion");
        out.write("</div>");

        out.write("<section class=\"section\"><h2>Acciones frecuentes</h2><div class=\"list\">");
        writeItem(out, "Consultar ejecucion historica", "Usa el historial para entrar a un run especifico, abrir su detalle o reenviar su correo.");
        writeItem(out, "Exportar informacion", "Puedes descargar el CSV actual o el CSV del historial desde el mismo panel.");
        writeItem(out, "Operar y validar", "Health, demo y reporte actual quedan a un clic sin perder autenticacion.");
        out.write("</div></section>");

        out.write("</div></body></html>");
    }

    private static void writeCard(PrintWriter out, String title, String description, String href, String action) {
        out.write("<section class=\"card\"><h2>");
        out.write(escapeHtml(title));
        out.write("</h2><p>");
        out.write(escapeHtml(description));
        out.write("</p><a href=\"");
        out.write(escapeHtml(href));
        out.write("\">");
        out.write(escapeHtml(action));
        out.write("</a></section>");
    }

    private static void writeItem(PrintWriter out, String title, String description) {
        out.write("<div class=\"item\"><strong>");
        out.write(escapeHtml(title));
        out.write("</strong><span>");
        out.write(escapeHtml(description));
        out.write("</span></div>");
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
