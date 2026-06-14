package com.syncadmaximo.web;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.web.security.WebPrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class WebRequestContext {

    private WebRequestContext() {
    }

    public static SyncExecution resolveExecution(HttpServletRequest request) {
        Object value = request.getAttribute(WebSessionKeys.LAST_EXECUTION);
        if (value instanceof SyncExecution) {
            return (SyncExecution) value;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        value = session.getAttribute(WebSessionKeys.LAST_EXECUTION);
        return value instanceof SyncExecution ? (SyncExecution) value : null;
    }

    public static SyncResult resolveResult(HttpServletRequest request) {
        Object value = request.getAttribute(WebSessionKeys.LAST_RESULT);
        if (value instanceof SyncResult) {
            return (SyncResult) value;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        value = session.getAttribute(WebSessionKeys.LAST_RESULT);
        return value instanceof SyncResult ? (SyncResult) value : null;
    }

    public static void remember(HttpServletRequest request, SyncExecution execution, SyncResult result) {
        HttpSession session = request.getSession(true);
        session.setAttribute(WebSessionKeys.LAST_EXECUTION, execution);
        session.setAttribute(WebSessionKeys.LAST_RESULT, result);
    }

    public static WebPrincipal getPrincipal(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(WebSessionKeys.PRINCIPAL);
        return value instanceof WebPrincipal ? (WebPrincipal) value : null;
    }
}
