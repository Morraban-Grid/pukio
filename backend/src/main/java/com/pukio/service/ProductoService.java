package com.pukio.service;

import com.pukio.dao.ProductoDAO;
import com.pukio.model.Producto;
import java.sql.SQLException;
import java.util.List;

public class ProductoService {

    private final ProductoDAO dao = new ProductoDAO();

    public List<Producto> listar() throws SQLException { return dao.listarActivos(); }

    public List<Producto> buscarPorNombre(String q) throws SQLException { return dao.buscarPorNombre(q); }

    public Producto buscarPorCodigo(String c) throws SQLException { return dao.buscarPorCodigo(c); }

    public List<Producto> bajoStock() throws SQLException { return dao.listarBajoStock(); }

    public void guardar(Producto p) throws Exception {
        validar(p);
        if (p.getIdProducto() == 0) dao.insertar(p);
        else dao.actualizar(p);
    }

    public void eliminar(int id) throws SQLException { dao.eliminar(id); }

    private void validar(Producto p) throws Exception {
        if (p.getCodigo() == null || p.getCodigo().isBlank()) throw new Exception("El codigo es obligatorio.");
        if (p.getNombre() == null || p.getNombre().isBlank()) throw new Exception("El nombre es obligatorio.");
        if (p.getPrecioVenta() <= 0) throw new Exception("El precio de venta debe ser mayor a 0.");
        if (p.getStock() < 0) throw new Exception("El stock no puede ser negativo.");
    }
}
