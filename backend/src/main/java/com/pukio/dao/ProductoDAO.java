package com.pukio.dao;

import com.pukio.model.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private static final String SELECT_BASE =
        "SELECT P.*, C.NOMBRE AS NOMBRE_CATEGORIA, PR.NOMBRE AS NOMBRE_PROVEEDOR FROM PRODUCTOS P " +
        "LEFT JOIN CATEGORIAS C ON C.ID_CATEGORIA = P.ID_CATEGORIA " +
        "LEFT JOIN PROVEEDORES PR ON PR.ID_PROVEEDOR = P.ID_PROVEEDOR ";

    public List<Producto> listarActivos() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE P.ACTIVO=1 ORDER BY P.NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Producto buscarPorCodigo(String codigo) throws SQLException {
        String sql = SELECT_BASE + "WHERE P.CODIGO = ? AND P.ACTIVO=1";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public List<Producto> buscarPorNombre(String nombre) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE UPPER(P.NOMBRE) LIKE UPPER(?) AND P.ACTIVO=1 ORDER BY P.NOMBRE";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Producto> listarBajoStock() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE P.ACTIVO=1 AND P.STOCK <= P.STOCK_MINIMO ORDER BY P.STOCK";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO PRODUCTOS(CODIGO,NOMBRE,DESCRIPCION,PRECIO_COMPRA,PRECIO_VENTA," +
                     "STOCK,STOCK_MINIMO,ID_CATEGORIA,ID_PROVEEDOR) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getCodigo().toUpperCase());
                ps.setString(2, p.getNombre());
                ps.setString(3, p.getDescripcion());
                ps.setDouble(4, p.getPrecioCompra());
                ps.setDouble(5, p.getPrecioVenta());
                ps.setInt(6, p.getStock());
                ps.setInt(7, p.getStockMinimo());
                if (p.getIdCategoria() > 0) ps.setInt(8, p.getIdCategoria()); else ps.setNull(8, Types.NUMERIC);
                if (p.getIdProveedor() > 0) ps.setInt(9, p.getIdProveedor()); else ps.setNull(9, Types.NUMERIC);
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE PRODUCTOS SET CODIGO=?,NOMBRE=?,DESCRIPCION=?,PRECIO_COMPRA=?," +
                     "PRECIO_VENTA=?,STOCK=?,STOCK_MINIMO=?,ID_CATEGORIA=?,ID_PROVEEDOR=? " +
                     "WHERE ID_PRODUCTO=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getCodigo().toUpperCase());
                ps.setString(2, p.getNombre());
                ps.setString(3, p.getDescripcion());
                ps.setDouble(4, p.getPrecioCompra());
                ps.setDouble(5, p.getPrecioVenta());
                ps.setInt(6, p.getStock());
                ps.setInt(7, p.getStockMinimo());
                if (p.getIdCategoria() > 0) ps.setInt(8, p.getIdCategoria()); else ps.setNull(8, Types.NUMERIC);
                if (p.getIdProveedor() > 0) ps.setInt(9, p.getIdProveedor()); else ps.setNull(9, Types.NUMERIC);
                ps.setInt(10, p.getIdProducto());
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizarStock(int idProducto, int nuevoStock) throws SQLException {
        String sql = "UPDATE PRODUCTOS SET STOCK=? WHERE ID_PRODUCTO=?";
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, nuevoStock);
                ps.setInt(2, idProducto);
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE PRODUCTOS SET ACTIVO=0 WHERE ID_PRODUCTO=?";
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

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("ID_PRODUCTO"));
        p.setCodigo(rs.getString("CODIGO"));
        p.setNombre(rs.getString("NOMBRE"));
        p.setDescripcion(rs.getString("DESCRIPCION"));
        p.setPrecioCompra(rs.getDouble("PRECIO_COMPRA"));
        p.setPrecioVenta(rs.getDouble("PRECIO_VENTA"));
        p.setStock(rs.getInt("STOCK"));
        p.setStockMinimo(rs.getInt("STOCK_MINIMO"));
        p.setIdCategoria(rs.getInt("ID_CATEGORIA"));
        p.setNombreCategoria(rs.getString("NOMBRE_CATEGORIA"));
        p.setCategoriaNombre(rs.getString("NOMBRE_CATEGORIA"));
        p.setIdProveedor(rs.getInt("ID_PROVEEDOR"));
        p.setProveedorNombre(rs.getString("NOMBRE_PROVEEDOR"));
        p.setActivo(rs.getInt("ACTIVO") == 1);
        return p;
    }
}
