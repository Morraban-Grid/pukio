package com.pukio.service;

import com.pukio.dao.UsuarioDAO;
import com.pukio.model.Usuario;
import com.pukio.util.SecurityUtil;

public class AuthService {

    private final UsuarioDAO dao = new UsuarioDAO();

    /**
     * Valida las credenciales del usuario y lo retorna si es correcto.
     * La sesión web se gestiona en la capa del Servlet usando HttpSession.
     */
    public Usuario login(String username, String password) throws Exception {
        if (username == null || username.isBlank()) throw new Exception("Ingrese su usuario.");
        if (password == null || password.isBlank()) throw new Exception("Ingrese su contrasena.");
        
        Usuario u = dao.buscarPorUsername(username.trim());
        if (u == null) throw new Exception("Usuario no encontrado.");
        if (!u.isActivo()) throw new Exception("El usuario se encuentra inactivo.");
        
        if (!SecurityUtil.verificar(password, u.getPasswordHash()))
            throw new Exception("Contrasena incorrecta.");
            
        return u;
    }
}
