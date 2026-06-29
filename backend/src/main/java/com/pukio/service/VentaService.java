package com.pukio.service;

import com.pukio.dao.ProductoDAO;
import com.pukio.dao.VentaDAO;
import com.pukio.model.DetalleVenta;
import com.pukio.model.Producto;
import com.pukio.model.Venta;
import java.sql.SQLException;

public class VentaService {

    private final VentaDAO ventaDAO       = new VentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    public Venta registrar(Venta v) throws Exception {
        if (v.getDetalles().isEmpty())
            throw new Exception("La venta debe tener al menos un producto.");

        // Verificar stock disponible
        for (DetalleVenta d : v.getDetalles()) {
            Producto p = productoDAO.buscarPorCodigo(
                d.getCodigoProducto() != null ? d.getCodigoProducto() : "");
            if (p == null) {
                // buscar por id
                continue;
            }
            if (p.getStock() < d.getCantidad())
                throw new Exception("Stock insuficiente para: " + p.getNombre() +
                        " (disponible: " + p.getStock() + ")");
        }

        v.calcularTotales();
        if (v.getIdUsuario() <= 0) {
            throw new Exception("Usuario no autenticado para realizar la venta.");
        }
        ventaDAO.insertar(v);
        return v;
    }

    public double totalVentasHoy() throws SQLException { return ventaDAO.totalVentasHoy(); }
    public int    cantidadVentasHoy() throws SQLException { return ventaDAO.cantidadVentasHoy(); }
}
