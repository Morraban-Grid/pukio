package com.pukio.controller;

import com.pukio.dao.ClienteDAO;
import com.pukio.model.Cliente;
import com.pukio.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ClienteServlet", urlPatterns = {"/api/clientes", "/api/clientes/buscar"})
public class ClienteServlet extends HttpServlet {

    private final ClienteDAO dao = new ClienteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        try {
            if (path.endsWith("/buscar")) {
                String q = request.getParameter("q");
                if (q == null || q.isBlank()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                // Intentar buscar primero por documento, si no por nombre
                Cliente c = dao.buscarPorDoc(q);
                if (c != null) {
                    response.getWriter().write(JsonUtil.toJson(List.of(c)));
                } else {
                    List<Cliente> lista = dao.buscarPorNombre(q);
                    response.getWriter().write(JsonUtil.toJson(lista));
                }
            } else {
                List<Cliente> lista = dao.listarActivos();
                response.getWriter().write(JsonUtil.toJson(lista));
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Error en base de datos: " + e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Cliente c = JsonUtil.fromJson(request.getReader(), Cliente.class);
            validar(c);
            dao.insertar(c);
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(JsonUtil.toJson(c));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Cliente c = JsonUtil.fromJson(request.getReader(), Cliente.class);
            validar(c);
            dao.actualizar(c);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtil.toJson(c));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Falta el parametro ID");
            response.getWriter().write(JsonUtil.toJson(err));
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            dao.eliminar(id);
            response.setStatus(HttpServletResponse.SC_OK);
            Map<String, String> res = new HashMap<>();
            res.put("message", "Cliente desactivado correctamente");
            response.getWriter().write(JsonUtil.toJson(res));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }

    private void validar(Cliente c) throws Exception {
        if (c.getNumeroDoc() == null || c.getNumeroDoc().isBlank()) {
            throw new Exception("El numero de documento es obligatorio.");
        }
        if (c.getNombre() == null || c.getNombre().isBlank()) {
            throw new Exception("El nombre es obligatorio.");
        }
    }
}
