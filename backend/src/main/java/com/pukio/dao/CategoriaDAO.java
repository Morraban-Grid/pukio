package com.pukio.dao;

import com.pukio.model.Categoria;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<Categoria> listarActivas() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM CATEGORIAS WHERE ACTIVO=1 ORDER BY NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void insertar(Categoria c) throws SQLException {
        String sql = "INSERT INTO CATEGORIAS(NOMBRE, DESCRIPCION) VALUES(?,?)";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getDescripcion());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(Categoria c) throws SQLException {
        String sql = "UPDATE CATEGORIAS SET NOMBRE=?, DESCRIPCION=? WHERE ID_CATEGORIA=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getDescripcion());
                ps.setInt(3, c.getIdCategoria());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setIdCategoria(rs.getInt("ID_CATEGORIA"));
        c.setNombre(rs.getString("NOMBRE"));
        c.setDescripcion(rs.getString("DESCRIPCION"));
        c.setActivo(rs.getInt("ACTIVO") == 1);
        return c;
    }
}
