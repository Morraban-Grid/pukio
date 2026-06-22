package com.pukio.service;

import com.pukio.dao.ProductoDAO;
import com.pukio.dao.VentaDAO;
import com.pukio.model.Producto;
import com.pukio.model.Venta;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class ReporteService {

    private final VentaDAO ventaDAO       = new VentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    public List<Venta> ventasPorFecha(Date desde, Date hasta) throws SQLException {
        java.sql.Date sqlDesde = new java.sql.Date(desde.getTime());
        java.sql.Date sqlHasta = new java.sql.Date(hasta.getTime());
        return ventaDAO.listarPorFecha(sqlDesde, sqlHasta);
    }

    public List<Object[]> productosMasVendidos(int top) throws SQLException {
        return ventaDAO.productosMasVendidos(top);
    }

    public List<Producto> productosConBajoStock() throws SQLException {
        return productoDAO.listarBajoStock();
    }
}
