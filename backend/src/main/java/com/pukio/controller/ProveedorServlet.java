package com.pukio.controller;

import com.pukio.dao.ProveedorDAO;
import com.pukio.model.Proveedor;
import com.pukio.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ProveedorServlet", urlPatterns = {"/api/proveedores"})
public class ProveedorServlet extends HttpServlet {

    private final ProveedorDAO dao = new ProveedorDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            List<Proveedor> lista = dao.listarActivos();
            response.getWriter().write(JsonUtil.toJson(lista));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Error en base de datos: " + e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }
}
