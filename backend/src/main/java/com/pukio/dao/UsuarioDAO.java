package com.pukio.dao;

import com.pukio.model.Usuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Usuario buscarPorUsername(String username) throws SQLException {
        String sql = "SELECT * FROM USUARIOS WHERE USERNAME = ? AND ACTIVO = 1";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM USUARIOS ORDER BY NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO USUARIOS (USERNAME, PASSWORD_HASH, NOMBRE, ROL) VALUES (?,?,?,?)";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getPasswordHash());
                ps.setString(3, u.getNombre());
                ps.setString(4, u.getRol());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE USUARIOS SET NOMBRE=?, ROL=?, ACTIVO=? WHERE ID_USUARIO=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, u.getNombre());
                ps.setString(2, u.getRol());
                ps.setInt(3, u.isActivo() ? 1 : 0);
                ps.setInt(4, u.getIdUsuario());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizarPassword(int idUsuario, String nuevoHash) throws SQLException {
        String sql = "UPDATE USUARIOS SET PASSWORD_HASH=? WHERE ID_USUARIO=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nuevoHash);
                ps.setInt(2, idUsuario);
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE USUARIOS SET ACTIVO=0 WHERE ID_USUARIO=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("ID_USUARIO"));
        u.setUsername(rs.getString("USERNAME"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        u.setNombre(rs.getString("NOMBRE"));
        u.setRol(rs.getString("ROL"));
        u.setActivo(rs.getInt("ACTIVO") == 1);
        return u;
    }
}
