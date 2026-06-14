package com.syncadmaximo.web.filter;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.web.WebRequestContext;
import com.syncadmaximo.web.WebSessionKeys;
import com.syncadmaximo.web.security.WebPrincipal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthFilter implements Filter {

    private String allowedUser;

    @Override
    public void init(FilterConfig filterConfig) {
        allowedUser = AppConfig.getInstance().getAllowedUser();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = resolvePath(httpRequest);
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        WebPrincipal principal = WebRequestContext.getPrincipal(httpRequest);
        if (principal != null && principal.isUser(allowedUser)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (isApiPath(path)) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String redirect = httpRequest.getContextPath() + "/login?redirect=" + URLEncoder.encode(path, StandardCharsets.UTF_8.name());
        httpResponse.sendRedirect(redirect);
    }

    @Override
    public void destroy() {
    }

    private static String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        StringBuilder builder = new StringBuilder();
        if (servletPath != null) {
            builder.append(servletPath);
        }
        if (pathInfo != null) {
            builder.append(pathInfo);
        }
        String path = builder.toString();
        return path.isEmpty() ? "/" : path;
    }

    private static boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        if ("/".equals(path)) {
            return false;
        }
        return path.equals("/login")
                || path.equals("/demo")
                || path.equals("/logout")
                || path.equals("/health")
                || path.equals("/favicon.ico")
                || path.startsWith("/assets/")
                || path.startsWith("/static/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/img/")
                || path.startsWith("/fonts/");
    }

    private static boolean isApiPath(String path) {
        return path != null && path.startsWith("/api/");
    }
}
