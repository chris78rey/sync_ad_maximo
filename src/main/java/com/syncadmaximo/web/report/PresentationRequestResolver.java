package com.syncadmaximo.web.report;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.web.WebRequestContext;

import javax.servlet.http.HttpServletRequest;

public final class PresentationRequestResolver {

    private PresentationRequestResolver() {
    }

    public static SyncExecution resolveExecution(HttpServletRequest request) {
        return WebRequestContext.resolveExecution(request);
    }

    public static SyncResult resolveResult(HttpServletRequest request) {
        return WebRequestContext.resolveResult(request);
    }
}
