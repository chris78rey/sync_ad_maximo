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
        out.write("body{margin:0;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;background:");
        out.write("radial-gradient(circle at top left,rgba(58,90,122,.18),transparent 30%),");
        out.write("radial-gradient(circle at top right,rgba(16,42,67,.16),transparent 26%),");
        out.write("linear-gradient(180deg,#f7fafc 0%,#eef4fb 100%);color:#102a43;min-height:100vh;}");
        out.write("body:before{content:'';position:fixed;inset:-160px -120px auto auto;width:360px;height:360px;border-radius:50%;");
        out.write("background:rgba(16,42,67,.08);filter:blur(10px);pointer-events:none;}");
        out.write(".wrap{max-width:1220px;margin:0 auto;padding:28px 22px 52px;position:relative;z-index:1;}");
        out.write(".hero{position:relative;overflow:hidden;background:linear-gradient(135deg,#102a43 0%,#1d3557 55%,#243b53 100%);color:#fff;border-radius:28px;padding:34px 34px 30px;box-shadow:0 24px 60px rgba(16,42,67,.22);border:1px solid rgba(255,255,255,.08);}");
        out.write(".hero:after{content:'';position:absolute;inset:auto -70px -90px auto;width:220px;height:220px;border-radius:50%;background:rgba(255,255,255,.08);}");
        out.write(".kicker{margin:0 0 10px;letter-spacing:.18em;text-transform:uppercase;font-size:12px;color:#d9e2ec;}");
        out.write(".hero h1{margin:0 0 10px;font-size:clamp(34px,4vw,52px);line-height:1.05;}");
        out.write(".hero p{margin:0;color:#d9e2ec;max-width:760px;line-height:1.65;font-size:16px;}");
        out.write(".meta{margin-top:20px;display:flex;gap:10px;flex-wrap:wrap;}");
        out.write(".pill{background:rgba(255,255,255,.12);backdrop-filter:blur(8px);padding:9px 13px;border-radius:999px;font-size:13px;border:1px solid rgba(255,255,255,.12);}");
        out.write(".hero-grid{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(260px,.8fr);gap:18px;align-items:end;}");
        out.write(".hero-note{background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.1);border-radius:20px;padding:18px 18px 16px;backdrop-filter:blur(8px);}");
        out.write(".hero-note strong{display:block;font-size:18px;margin-bottom:8px;}");
        out.write(".hero-note span{display:block;color:#d9e2ec;line-height:1.55;}");
        out.write(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px;margin-top:24px;}");
        out.write(".card{position:relative;background:rgba(255,255,255,.88);backdrop-filter:blur(10px);border-radius:20px;padding:20px 20px 18px;box-shadow:0 14px 34px rgba(16,42,67,.08);border:1px solid rgba(16,42,67,.08);transition:transform .18s ease, box-shadow .18s ease;}");
        out.write(".card:hover{transform:translateY(-2px);box-shadow:0 18px 40px rgba(16,42,67,.12);}");
        out.write(".card h2{margin:0 0 8px;font-size:18px;}");
        out.write(".card p{margin:0 0 14px;color:#52606d;line-height:1.55;min-height:48px;}");
        out.write(".card a{display:inline-flex;align-items:center;gap:8px;text-decoration:none;background:#102a43;color:#fff;padding:10px 14px;border-radius:12px;font-weight:600;box-shadow:0 8px 16px rgba(16,42,67,.16);}");
        out.write(".card a.secondary{background:#d9e2ec;color:#102a43;box-shadow:none;}");
        out.write(".section{margin-top:28px;background:rgba(255,255,255,.86);backdrop-filter:blur(10px);border-radius:20px;padding:22px;box-shadow:0 14px 34px rgba(16,42,67,.08);border:1px solid rgba(16,42,67,.06);}");
        out.write(".section h2{margin-top:0;margin-bottom:14px;font-size:22px;}");
        out.write(".list{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px;}");
        out.write(".item{border:1px solid #d9e2ec;border-radius:14px;padding:16px;background:linear-gradient(180deg,#ffffff 0%,#f8fafc 100%);}");
        out.write(".item strong{display:block;margin-bottom:6px;font-size:15px;}");
        out.write(".item span{color:#52606d;line-height:1.55;}");
        out.write(".footer{margin-top:18px;color:#627d98;font-size:13px;text-align:center;}");
        out.write("</style></head><body><div class=\"wrap\">");
        out.write("<section class=\"hero\"><div class=\"hero-grid\"><div><p class=\"kicker\">Operational console</p><h1>Sync AD Maximo</h1>");
        out.write("<p>Panel unico para operar la sincronizacion, revisar ejecuciones, abrir reportes, exportar CSV y consultar el historial sin navegar pantallas sueltas.</p>");
        out.write("<div class=\"meta\">");
        out.write("<span class=\"pill\">Usuario: ");
        out.write(escapeHtml(principal == null ? "maxadmin" : principal.getDisplayName()));
        out.write("</span>");
        out.write("<span class=\"pill\">Menu central</span>");
        out.write("<span class=\"pill\">Tomcat 9</span>");
        out.write("</div></div><aside class=\"hero-note\"><strong>Acceso rapido</strong><span>Todo lo importante queda a un clic: reporte, historial, CSV, demo, health y salida segura.</span></aside></div></section>");

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

        out.write("<div class=\"footer\">Panel principal de operacion de Sync AD Maximo</div>");
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
