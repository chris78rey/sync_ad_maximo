package com.syncadmaximo.web.servlet;

import com.syncadmaximo.audit.RunRecord;
import com.syncadmaximo.testsupport.TestAssertions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;

public final class ReportControllerHistorySpec {

    public static void runAll() {
        rendersRecentRunsHtmlAndCsv();
    }

    static void rendersRecentRunsHtmlAndCsv() {
        RunRecord record = new RunRecord();
        record.setRunId(77L);
        record.setFechaInicio(Instant.parse("2026-01-01T10:00:00Z"));
        record.setFechaFin(Instant.parse("2026-01-01T10:05:00Z"));
        record.setModo("PRODUCTION");
        record.setProceso("sync-ad-maximo");
        record.setUsuarioEjecutor("jdoe");
        record.setOrigenEjecucion("MANUAL");
        record.setEstado("FINISHED");
        record.setMensaje("OK");

        StringWriter html = new StringWriter();
        ReportController.writeHistoryHtml(new PrintWriter(html), Collections.singletonList(record), "/sync");
        TestAssertions.contains(html.toString(), "Historial de ejecuciones", "Debe renderizar el encabezado");
        TestAssertions.contains(html.toString(), "/sync/api/reportes/historial/77", "Debe enlazar al detalle");
        TestAssertions.contains(html.toString(), "/sync/api/reportes/historial/77.csv", "Debe enlazar al CSV");
        TestAssertions.contains(html.toString(), "/sync/api/reportes/historial/77/reenviar", "Debe enlazar al reenvio");

        StringWriter csv = new StringWriter();
        ReportController.writeHistoryCsv(new PrintWriter(csv), Collections.singletonList(record));
        TestAssertions.contains(csv.toString(), "runId,fechaInicio,fechaFin", "Debe incluir el encabezado CSV");
        TestAssertions.contains(csv.toString(), "\"77\"", "Debe incluir el runId");
        TestAssertions.contains(csv.toString(), "\"sync-ad-maximo\"", "Debe incluir el proceso");
    }
}
