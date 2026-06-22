package com.pukio.controller;

import com.pukio.config.DatabaseConfig;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro CORS con whitelist explicita de origen configurable.
 * El origen permitido se lee desde config.properties (clave cors.allowed.origin).
 */
@WebFilter(urlPatterns = {"/api/*"})
public class CorsFilter implements Filter {

    private String allowedOrigin;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.allowedOrigin = DatabaseConfig.getCorsAllowedOrigin();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req   = (HttpServletRequest)  request;
        HttpServletResponse res  = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");

        if (allowedOrigin != null && allowedOrigin.equals(origin)) {
            res.setHeader("Access-Control-Allow-Origin",      allowedOrigin);
            res.setHeader("Access-Control-Allow-Methods",     "GET, POST, PUT, DELETE, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers",     "Content-Type, Authorization");
            res.setHeader("Access-Control-Allow-Credentials", "true");
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
