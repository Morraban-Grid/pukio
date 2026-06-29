package com.pukio.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataWarehouseDAO {

    public int cargarDWH() throws SQLException {
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                conn.createStatement().executeUpdate("DELETE FROM PUKIO_DWH.DWH_VENTAS");
                String sql = "INSERT INTO PUKIO_DWH.DWH_VENTAS(ID_VENTA,NUMERO_COMPROBANTE,TIPO_COMPROBANTE," +
                             "FECHA_VENTA,ANIO,MES,DIA,TRIMESTRE,NOMBRE_CLIENTE,NOMBRE_CAJERO," +
                             "NOMBRE_PRODUCTO,CATEGORIA,CANTIDAD,PRECIO_UNIT,SUBTOTAL,IGV,TOTAL_VENTA,METODO_PAGO) " +
                             "SELECT ID_VENTA,NUMERO_COMPROBANTE,TIPO_COMPROBANTE,FECHA_VENTA," +
                             "ANIO,MES,DIA,TRIMESTRE,NOMBRE_CLIENTE,NOMBRE_CAJERO," +
                             "NOMBRE_PRODUCTO,CATEGORIA,CANTIDAD,PRECIO_UNIT,SUBTOTAL,IGV,TOTAL_VENTA,METODO_PAGO " +
                             "FROM PUKIO_DWH.VW_VENTAS_DETALLADA";
                int rows = conn.createStatement().executeUpdate(sql);
                conn.commit();
                return rows;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public int generarCrossTab() throws SQLException {
        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                conn.createStatement().executeUpdate("DELETE FROM PUKIO_DWH.CROSSTAB_VENTAS");
                String sql = "INSERT INTO PUKIO_DWH.CROSSTAB_VENTAS(ANIO,MES,CATEGORIA,TOTAL_UNIDADES,TOTAL_INGRESOS,TOTAL_VENTAS) " +
                             "SELECT ANIO, MES, NVL(CATEGORIA,'SIN CATEGORIA'), " +
                             "SUM(CANTIDAD), SUM(SUBTOTAL), COUNT(DISTINCT ID_VENTA) " +
                             "FROM PUKIO_DWH.DWH_VENTAS GROUP BY ANIO,MES,CATEGORIA";
                int rows = conn.createStatement().executeUpdate(sql);
                conn.commit();
                return rows;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Object[]> obtenerCrossTab() throws SQLException {
        List<Object[]> datos = new ArrayList<>();
        String sql = "SELECT ANIO,MES,CATEGORIA,TOTAL_UNIDADES,TOTAL_INGRESOS,TOTAL_VENTAS " +
                     "FROM PUKIO_DWH.CROSSTAB_VENTAS ORDER BY ANIO,MES,CATEGORIA";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                datos.add(new Object[]{
                    rs.getInt(1), rs.getInt(2), rs.getString(3),
                    rs.getLong(4), rs.getDouble(5), rs.getInt(6)
                });
            }
        }
        return datos;
    }

    public int totalRegistrosDWH() throws SQLException {
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM PUKIO_DWH.DWH_VENTAS")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}
