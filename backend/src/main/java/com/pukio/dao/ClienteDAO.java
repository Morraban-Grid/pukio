package com.pukio.dao;

import com.pukio.model.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public List<Cliente> listarActivos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM CLIENTES WHERE ACTIVO=1 ORDER BY NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Cliente buscarPorDoc(String doc) throws SQLException {
        String sql = "SELECT * FROM CLIENTES WHERE NUMERO_DOC=? AND ACTIVO=1";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public List<Cliente> buscarPorNombre(String nombre) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM CLIENTES WHERE UPPER(NOMBRE) LIKE UPPER(?) AND ACTIVO=1 ORDER BY NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public void insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO CLIENTES(TIPO_DOC,NUMERO_DOC,NOMBRE,TELEFONO,CORREO,DIRECCION) VALUES(?,?,?,?,?,?)";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getTipoDoc());
                ps.setString(2, c.getNumeroDoc());
                ps.setString(3, c.getNombre());
                ps.setString(4, c.getTelefono());
                ps.setString(5, c.getCorreo());
                ps.setString(6, c.getDireccion());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(Cliente c) throws SQLException {
        String sql = "UPDATE CLIENTES SET TIPO_DOC=?,NUMERO_DOC=?,NOMBRE=?,TELEFONO=?,CORREO=?,DIRECCION=? WHERE ID_CLIENTE=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getTipoDoc());
                ps.setString(2, c.getNumeroDoc());
                ps.setString(3, c.getNombre());
                ps.setString(4, c.getTelefono());
                ps.setString(5, c.getCorreo());
                ps.setString(6, c.getDireccion());
                ps.setInt(7, c.getIdCliente());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE CLIENTES SET ACTIVO=0 WHERE ID_CLIENTE=?";
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

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setIdCliente(rs.getInt("ID_CLIENTE"));
        c.setTipoDoc(rs.getString("TIPO_DOC"));
        c.setNumeroDoc(rs.getString("NUMERO_DOC"));
        c.setNombre(rs.getString("NOMBRE"));
        c.setTelefono(rs.getString("TELEFONO"));
        c.setCorreo(rs.getString("CORREO"));
        c.setDireccion(rs.getString("DIRECCION"));
        c.setActivo(rs.getInt("ACTIVO") == 1);
        return c;
    }
}
