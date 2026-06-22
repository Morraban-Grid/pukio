package com.pukio.controller;

import com.pukio.model.Producto;
import com.pukio.service.ProductoService;
import com.pukio.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ProductoServlet", urlPatterns = {"/api/productos", "/api/productos/buscar", "/api/productos/bajo-stock"})
public class ProductoServlet extends HttpServlet {

    private final ProductoService service = new ProductoService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        try {
            if (path.endsWith("/buscar")) {
                String q = request.getParameter("q");
                String code = request.getParameter("codigo");
                if (code != null && !code.isBlank()) {
                    Producto p = service.buscarPorCodigo(code);
                    if (p != null) {
                        response.getWriter().write(JsonUtil.toJson(p));
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        Map<String, String> err = new HashMap<>();
                        err.put("error", "Producto no encontrado");
                        response.getWriter().write(JsonUtil.toJson(err));
                    }
                } else {
                    List<Producto> lista = service.buscarPorNombre(q == null ? "" : q);
                    response.getWriter().write(JsonUtil.toJson(lista));
                }
            } else if (path.endsWith("/bajo-stock")) {
                List<Producto> lista = service.bajoStock();
                response.getWriter().write(JsonUtil.toJson(lista));
            } else {
                List<Producto> lista = service.listar();
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
            Producto p = JsonUtil.fromJson(request.getReader(), Producto.class);
            service.guardar(p);
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(JsonUtil.toJson(p));
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
            Producto p = JsonUtil.fromJson(request.getReader(), Producto.class);
            service.guardar(p);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtil.toJson(p));
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
            service.eliminar(id);
            response.setStatus(HttpServletResponse.SC_OK);
            Map<String, String> res = new HashMap<>();
            res.put("message", "Producto eliminado correctamente");
            response.getWriter().write(JsonUtil.toJson(res));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }
}
