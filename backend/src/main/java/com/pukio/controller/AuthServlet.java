package com.pukio.controller;

import com.pukio.model.Usuario;
import com.pukio.service.AuthService;
import com.pukio.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/login", "/api/auth/logout", "/api/auth/session"})
public class AuthServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        if (path.endsWith("/login")) {
            handleLogin(request, response);
        } else if (path.endsWith("/logout")) {
            handleLogout(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        if (path.endsWith("/session")) {
            handleSessionCheck(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        try {
            // Leer credenciales del request body
            Map<String, String> credenciales = JsonUtil.fromJson(request.getReader(), Map.class);
            String username = credenciales.get("username");
            String password = credenciales.get("password");

            Usuario usuario = authService.login(username, password);

            // Crear sesión en el servidor
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", usuario);

            // Respuesta exitosa
            Map<String, Object> res = new HashMap<>();
            res.put("idUsuario", usuario.getIdUsuario());
            res.put("username", usuario.getUsername());
            res.put("nombre", usuario.getNombre());
            res.put("rol", usuario.getRol());

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtil.toJson(res));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.setStatus(HttpServletResponse.SC_OK);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Sesion cerrada correctamente");
        response.getWriter().write(JsonUtil.toJson(res));
    }

    private void handleSessionCheck(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuario") != null) {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            Map<String, Object> res = new HashMap<>();
            res.put("idUsuario", usuario.getIdUsuario());
            res.put("username", usuario.getUsername());
            res.put("nombre", usuario.getNombre());
            res.put("rol", usuario.getRol());

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtil.toJson(res));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> err = new HashMap<>();
            err.put("error", "No hay sesion activa");
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }
}
