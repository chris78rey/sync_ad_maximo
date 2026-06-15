package com.syncadmaximo.web.servlet;

import com.syncadmaximo.testsupport.TestAssertions;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class HomeServletSpec {

    public static void runAll() {
        rendersUnifiedDashboard();
    }

    static void rendersUnifiedDashboard() {
        StringWriter buffer = new StringWriter();
        HomeServlet.renderDashboard(new PrintWriter(buffer), null);
        String html = buffer.toString();

        TestAssertions.contains(html, "Sync AD Maximo", "Debe renderizar el titulo del dashboard");
        TestAssertions.contains(html, "/report", "Debe enlazar al reporte actual");
        TestAssertions.contains(html, "/api/reportes/historial", "Debe enlazar al historial");
        TestAssertions.contains(html, "/api/reportes/historial.csv", "Debe enlazar al CSV historico");
        TestAssertions.contains(html, "/health", "Debe enlazar a health");
        TestAssertions.contains(html, "/demo", "Debe enlazar al demo");
        TestAssertions.contains(html, "/logout", "Debe enlazar al logout");
    }
}
