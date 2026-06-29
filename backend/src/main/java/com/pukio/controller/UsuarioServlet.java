package com.pukio.controller;

import com.pukio.dao.UsuarioDAO;
import com.pukio.model.Usuario;
import com.pukio.util.JsonUtil;
import com.pukio.util.SecurityUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet REST para la gestión de usuarios (RF-51, RF-52, RF-53).
 * Acceso restringido exclusivamente al rol ADMIN (reforzado en SecurityFilter).
 *
 * GET    /api/usuarios           → listar todos los usuarios
 * POST   /api/usuarios           → registrar nuevo usuario
 * PUT    /api/usuarios           → actualizar usuario (nombre, rol, activo)
 * PUT    /api/usuarios/password  → restablecer contraseña
 */
@WebServlet(
    name = "UsuarioServlet",
    urlPatterns = {
        "/api/usuarios",
        "/api/usuarios/password"
    }
)
public class UsuarioServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            List<Usuario> lista = usuarioDAO.listarTodos();
            // No exponer password_hash en la respuesta
            lista.forEach(u -> u.setPasswordHash(null));
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtil.toJson(lista));
        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al listar usuarios: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        try {
            // PUT /api/usuarios/password se enruta aquí si llega como POST por compatibilidad
            // El flujo normal usa doPut
            String body = new String(request.getInputStream().readAllBytes(), "UTF-8");
            Map<String, Object> data = JsonUtil.fromJson(body);

            String username = (String) data.get("username");
            String password = (String) data.get("password");
            String nombre   = (String) data.get("nombre");
            String rol      = (String) data.get("rol");

            if (username == null || username.isBlank()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'username' es obligatorio.");
                return;
            }
            if (password == null || password.isBlank()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'password' es obligatorio.");
                return;
            }
            if (nombre == null || nombre.isBlank()) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombre' es obligatorio.");
                return;
            }
            if (!"ADMIN".equals(rol) && !"CAJERO".equals(rol)) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "El rol debe ser 'ADMIN' o 'CAJERO'.");
                return;
            }

            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPasswordHash(SecurityUtil.hash(password));
            u.setNombre(nombre);
            u.setRol(rol);
            u.setActivo(true);

            usuarioDAO.insertar(u);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Usuario registrado correctamente.");
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(JsonUtil.toJson(res));

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("ORA-00001")) {
                sendError(response, HttpServletResponse.SC_CONFLICT,
                        "El nombre de usuario ya existe. Elija otro.");
            } else {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error al registrar usuario: " + e.getMessage());
            }
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Peticion invalida: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();

        try {
            String body = new String(request.getInputStream().readAllBytes(), "UTF-8");
            Map<String, Object> data = JsonUtil.fromJson(body);

            if (path.endsWith("/usuarios/password")) {
                // Restablecer contraseña (RF-53)
                Object idObj = data.get("idUsuario");
                String nuevaPassword = (String) data.get("password");

                if (idObj == null || nuevaPassword == null || nuevaPassword.isBlank()) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Se requieren 'idUsuario' y 'password'.");
                    return;
                }

                int idUsuario = ((Number) idObj).intValue();
                String nuevoHash = SecurityUtil.hash(nuevaPassword);
                usuarioDAO.actualizarPassword(idUsuario, nuevoHash);

                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("message", "Contrasena actualizada correctamente.");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.toJson(res));

            } else {
                // Actualizar nombre, rol y estado activo (RF-51, RF-52)
                Object idObj   = data.get("idUsuario");
                String nombre  = (String) data.get("nombre");
                String rol     = (String) data.get("rol");
                Object activoObj = data.get("activo");

                if (idObj == null) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "'idUsuario' es obligatorio.");
                    return;
                }

                Usuario u = new Usuario();
                u.setIdUsuario(((Number) idObj).intValue());
                u.setNombre(nombre != null ? nombre : "");
                u.setRol(rol != null ? rol : "CAJERO");
                u.setActivo(activoObj == null || Boolean.parseBoolean(activoObj.toString()));
                usuarioDAO.actualizar(u);

                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("message", "Usuario actualizado correctamente.");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.toJson(res));
            }

        } catch (SQLException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar usuario: " + e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Peticion invalida: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        response.getWriter().write(JsonUtil.toJson(err));
    }
}
