package com.pukio.model;

import java.util.Date;

public class Producto {
    private int      idProducto;
    private String   codigo;
    private String   nombre;
    private String   descripcion;
    private double   precioCompra;
    private double   precioVenta;
    private int      stock;
    private int      stockMinimo;
    private int      idCategoria;
    private String   nombreCategoria;
    private String   categoriaNombre;
    private int      idProveedor;
    private String   proveedorNombre;
    private boolean  activo;
    private Date     fechaRegistro;

    public Producto() {}

    public int     getIdProducto()           { return idProducto; }
    public void    setIdProducto(int v)      { this.idProducto = v; }
    public String  getCodigo()               { return codigo; }
    public void    setCodigo(String v)       { this.codigo = v; }
    public String  getNombre()               { return nombre; }
    public void    setNombre(String v)       { this.nombre = v; }
    public String  getDescripcion()          { return descripcion; }
    public void    setDescripcion(String v)  { this.descripcion = v; }
    public double  getPrecioCompra()         { return precioCompra; }
    public void    setPrecioCompra(double v) { this.precioCompra = v; }
    public double  getPrecioVenta()          { return precioVenta; }
    public void    setPrecioVenta(double v)  { this.precioVenta = v; }
    public int     getStock()                { return stock; }
    public void    setStock(int v)           { this.stock = v; }
    public int     getStockMinimo()          { return stockMinimo; }
    public void    setStockMinimo(int v)     { this.stockMinimo = v; }
    public int     getIdCategoria()          { return idCategoria; }
    public void    setIdCategoria(int v)     { this.idCategoria = v; }
    public String  getNombreCategoria()      { return nombreCategoria; }
    public void    setNombreCategoria(String v){ this.nombreCategoria = v; }
    public String  getCategoriaNombre()      { return categoriaNombre; }
    public void    setCategoriaNombre(String v){ this.categoriaNombre = v; }
    public int     getIdProveedor()          { return idProveedor; }
    public void    setIdProveedor(int v)     { this.idProveedor = v; }
    public String  getProveedorNombre()      { return proveedorNombre; }
    public void    setProveedorNombre(String v){ this.proveedorNombre = v; }
    public boolean isActivo()                { return activo; }
    public void    setActivo(boolean v)      { this.activo = v; }
    public Date    getFechaRegistro()        { return fechaRegistro; }
    public void    setFechaRegistro(Date v)  { this.fechaRegistro = v; }

    public boolean isStockBajo() { return stock <= stockMinimo; }

    @Override public String toString() { return "[" + codigo + "] " + nombre; }
}
