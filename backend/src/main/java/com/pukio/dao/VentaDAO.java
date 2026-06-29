package com.pukio.dao;

import com.pukio.model.DetalleVenta;
import com.pukio.model.Venta;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    /** Inserta venta + detalles en una sola transaccion. */
    public void insertar(Venta v) throws SQLException {
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idVenta;
                String numComp;

                // 1. Registrar cabecera llamando al procedimiento almacenado SP_REGISTRAR_VENTA
                String sqlCall = "{call SP_REGISTRAR_VENTA(?,?,?,?,?,?,?,?,?,?)}";
                try (CallableStatement cstmt = conn.prepareCall(sqlCall)) {
                    cstmt.setString(1, v.getTipoComprobante());
                    if (v.getIdCliente() > 0) {
                        cstmt.setInt(2, v.getIdCliente());
                    } else {
                        cstmt.setNull(2, Types.NUMERIC);
                    }
                    cstmt.setInt(3, v.getIdUsuario());
                    cstmt.setDouble(4, v.getSubtotal());
                    cstmt.setDouble(5, v.getIgv());
                    cstmt.setDouble(6, v.getDescuento());
                    cstmt.setDouble(7, v.getTotal());
                    cstmt.setString(8, v.getMetodoPago());
                    cstmt.registerOutParameter(9, Types.NUMERIC);
                    cstmt.registerOutParameter(10, Types.VARCHAR);
                    cstmt.execute();
                    idVenta = cstmt.getInt(9);
                    numComp = cstmt.getString(10);
                }

                v.setIdVenta(idVenta);
                v.setNumeroComprobante(numComp);

                // 2. Insertar detalles (trigger TRG_DETALLE_VENTA_STOCK valida y resta stock)
                String sqlD = "INSERT INTO DETALLE_VENTA(ID_VENTA,ID_PRODUCTO,CANTIDAD,PRECIO_UNIT,DESCUENTO,SUBTOTAL) " +
                              "VALUES(?,?,?,?,?,?)";
                try (PreparedStatement psD = conn.prepareStatement(sqlD)) {
                    for (DetalleVenta d : v.getDetalles()) {
                        d.setIdVenta(idVenta);
                        psD.setInt(1, idVenta);
                        psD.setInt(2, d.getIdProducto());
                        psD.setInt(3, d.getCantidad());
                        psD.setDouble(4, d.getPrecioUnit());
                        psD.setDouble(5, d.getDescuento());
                        psD.setDouble(6, d.getSubtotal());
                        psD.addBatch();
                    }
                    psD.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 20001 || e.getMessage().contains("20001")) {
                    throw new SQLException("Error de inventario: " + e.getMessage(), e);
                }
                throw e;
            }
        }
    }

    public List<Venta> listarPorFecha(Date desde, Date hasta) throws SQLException {
        List<Venta> lista = new ArrayList<>();
        String sql = "SELECT V.*, C.NOMBRE AS NOMBRE_CLIENTE, U.NOMBRE AS NOMBRE_USUARIO " +
                     "FROM VENTAS V " +
                     "LEFT JOIN CLIENTES C ON C.ID_CLIENTE=V.ID_CLIENTE " +
                     "JOIN USUARIOS U ON U.ID_USUARIO=V.ID_USUARIO " +
                     "WHERE TRUNC(V.FECHA_VENTA) BETWEEN ? AND ? ORDER BY V.FECHA_VENTA DESC";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(desde.getTime()));
            ps.setDate(2, new java.sql.Date(hasta.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearCabecera(rs));
            }
        }
        return lista;
    }

    public double totalVentasHoy() throws SQLException {
        String sql = "SELECT NVL(SUM(TOTAL),0) FROM VENTAS WHERE TRUNC(FECHA_VENTA)=TRUNC(SYSDATE)";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public int cantidadVentasHoy() throws SQLException {
        String sql = "SELECT COUNT(*) FROM VENTAS WHERE TRUNC(FECHA_VENTA)=TRUNC(SYSDATE)";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public List<Object[]> productosMasVendidos(int top) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT P.NOMBRE, SUM(DV.CANTIDAD) AS TOTAL_UNIC, SUM(DV.SUBTOTAL) AS TOTAL_S " +
                     "FROM DETALLE_VENTA DV JOIN PRODUCTOS P ON P.ID_PRODUCTO=DV.ID_PRODUCTO " +
                     "GROUP BY P.NOMBRE ORDER BY TOTAL_UNIC DESC FETCH FIRST ? ROWS ONLY";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, top);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getDouble(3)});
                }
            }
        }
        return lista;
    }

    private Venta mapearCabecera(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getInt("ID_VENTA"));
        v.setNumeroComprobante(rs.getString("NUMERO_COMPROBANTE"));
        v.setTipoComprobante(rs.getString("TIPO_COMPROBANTE"));
        v.setIdCliente(rs.getInt("ID_CLIENTE"));
        v.setNombreCliente(rs.getString("NOMBRE_CLIENTE"));
        v.setIdUsuario(rs.getInt("ID_USUARIO"));
        v.setNombreUsuario(rs.getString("NOMBRE_USUARIO"));
        v.setNombreCajero(rs.getString("NOMBRE_USUARIO"));
        v.setFechaVenta(rs.getDate("FECHA_VENTA"));
        v.setSubtotal(rs.getDouble("SUBTOTAL"));
        v.setIgv(rs.getDouble("IGV"));
        v.setDescuento(rs.getDouble("DESCUENTO"));
        v.setTotal(rs.getDouble("TOTAL"));
        v.setMetodoPago(rs.getString("METODO_PAGO"));
        v.setEstado(rs.getString("ESTADO"));
        return v;
    }
}
