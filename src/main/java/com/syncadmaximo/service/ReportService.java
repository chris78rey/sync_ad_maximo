package com.syncadmaximo.service;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.web.report.ExecutionPresentationModel;
import com.syncadmaximo.web.report.ExecutionPresentationService;

import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;

/**
 * Servicio de presentación y exportación de reportes.
 */
public class ReportService {

    private final ExecutionPresentationService presentationService;

    public ReportService(ZoneId zoneId) {
        this.presentationService = new ExecutionPresentationService(zoneId);
    }

    public ExecutionPresentationModel toModel(SyncExecution execution, SyncResult result) {
        return presentationService.toModel(execution, result);
    }

    public void writeHtml(Writer writer, ExecutionPresentationModel model, String contextPath) throws IOException {
        presentationService.writeHtml(writer, model, contextPath);
    }

    public void writeCsv(Writer writer, ExecutionPresentationModel model) throws IOException {
        presentationService.writeCsv(writer, model);
    }

    public String renderMailBody(ExecutionPresentationModel model) {
        return presentationService.renderMailBody(model);
    }
}
