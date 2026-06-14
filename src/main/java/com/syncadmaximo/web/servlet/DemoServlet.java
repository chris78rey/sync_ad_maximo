package com.syncadmaximo.web.servlet;

import com.syncadmaximo.web.WebRequestContext;
import com.syncadmaximo.web.WebSessionKeys;
import com.syncadmaximo.web.demo.DemoData;
import com.syncadmaximo.web.demo.DemoDataFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class DemoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DemoData demoData = DemoDataFactory.create();
        HttpSession session = request.getSession(true);
        session.setAttribute(WebSessionKeys.PRINCIPAL, demoData.getPrincipal());
        WebRequestContext.remember(request, demoData.getExecution(), demoData.getResult());
        session.setAttribute(WebSessionKeys.FLASH_MESSAGE, "Modo demo cargado. Ya puedes revisar reporte, CSV y correo.");
        response.sendRedirect(request.getContextPath() + "/report");
    }
}
