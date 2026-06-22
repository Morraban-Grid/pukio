package com.pukio.dao;

import com.pukio.model.Proveedor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    public List<Proveedor> listarActivos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM PROVEEDORES WHERE ACTIVO=1 ORDER BY NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Proveedor buscarPorRuc(String ruc) throws SQLException {
        String sql = "SELECT * FROM PROVEEDORES WHERE RUC=? AND ACTIVO=1";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void insertar(Proveedor p) throws SQLException {
        String sql = "INSERT INTO PROVEEDORES(RUC,NOMBRE,CONTACTO,TELEFONO,CORREO,DIRECCION) VALUES(?,?,?,?,?,?)";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getRuc());
                ps.setString(2, p.getNombre());
                ps.setString(3, p.getContacto());
                ps.setString(4, p.getTelefono());
                ps.setString(5, p.getCorreo());
                ps.setString(6, p.getDireccion());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE PROVEEDORES SET RUC=?,NOMBRE=?,CONTACTO=?,TELEFONO=?,CORREO=?,DIRECCION=? WHERE ID_PROVEEDOR=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getRuc());
                ps.setString(2, p.getNombre());
                ps.setString(3, p.getContacto());
                ps.setString(4, p.getTelefono());
                ps.setString(5, p.getCorreo());
                ps.setString(6, p.getDireccion());
                ps.setInt(7, p.getIdProveedor());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE PROVEEDORES SET ACTIVO=0 WHERE ID_PROVEEDOR=?";
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

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("ID_PROVEEDOR"));
        p.setRuc(rs.getString("RUC"));
        p.setNombre(rs.getString("NOMBRE"));
        p.setContacto(rs.getString("CONTACTO"));
        p.setTelefono(rs.getString("TELEFONO"));
        p.setCorreo(rs.getString("CORREO"));
        p.setDireccion(rs.getString("DIRECCION"));
        p.setActivo(rs.getInt("ACTIVO") == 1);
        return p;
    }
}
