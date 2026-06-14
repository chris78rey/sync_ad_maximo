package com.syncadmaximo.web.demo;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.web.security.WebPrincipal;

public final class DemoData {

    private final WebPrincipal principal;
    private final SyncExecution execution;
    private final SyncResult result;

    DemoData(WebPrincipal principal, SyncExecution execution, SyncResult result) {
        this.principal = principal;
        this.execution = execution;
        this.result = result;
    }

    public WebPrincipal getPrincipal() {
        return principal;
    }

    public SyncExecution getExecution() {
        return execution;
    }

    public SyncResult getResult() {
        return result;
    }
}
