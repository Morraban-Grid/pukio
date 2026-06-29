package com.pukio.controller;

import com.pukio.model.Usuario;
import com.pukio.model.Venta;
import com.pukio.service.VentaService;
import com.pukio.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "VentaServlet", urlPatterns = {"/api/ventas", "/api/ventas/resumen-hoy"})
public class VentaServlet extends HttpServlet {

    private final VentaService service = new VentaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        if (path.endsWith("/resumen-hoy")) {
            try {
                double total = service.totalVentasHoy();
                int cantidad = service.cantidadVentasHoy();

                Map<String, Object> res = new HashMap<>();
                res.put("total", total);
                res.put("cantidad", cantidad);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.toJson(res));
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                Map<String, String> err = new HashMap<>();
                err.put("error", "Error en base de datos: " + e.getMessage());
                response.getWriter().write(JsonUtil.toJson(err));
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Validar sesión del usuario
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> err = new HashMap<>();
            err.put("error", "No autorizado. Inicie sesion primero.");
            response.getWriter().write(JsonUtil.toJson(err));
            return;
        }

        Usuario usuarioSesion = (Usuario) session.getAttribute("usuario");

        try {
            Venta v = JsonUtil.fromJson(request.getReader(), Venta.class);
            
            // Asignar el cajero desde la sesión del servidor
            v.setIdUsuario(usuarioSesion.getIdUsuario());

            Venta ventaRegistrada = service.registrar(v);

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(JsonUtil.toJson(ventaRegistrada));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }
}
