package com.pukio.controller;

import com.pukio.model.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filtro de seguridad que intercepta peticiones web para validar sesiones
 * activas y reforzar restricciones de rol.
 *
 * Restricciones de rol implementadas:
 *  - /api/reportes/* y /api/dwh/* → solo ADMIN
 *  - /api/usuarios/*              → solo ADMIN
 */
@WebFilter(urlPatterns = { "/api/*" })
public class SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Permitir login y verificación de sesión sin validar sesión
        if (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/session")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"error\": \"Acceso no autorizado. Inicie sesion primero.\"}");
            return;
        }

        // Restricción de rol: solo ADMIN puede acceder a reportes, DWH y usuarios
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        boolean esRutaAdmin = path.contains("/api/reportes") 
                           || path.contains("/api/dwh")
                           || path.contains("/api/usuarios");

        if (esRutaAdmin && !"ADMIN".equals(usuario.getRol())) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"error\": \"Acceso denegado. Se requiere rol ADMIN para esta operacion.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
