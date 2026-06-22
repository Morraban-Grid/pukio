package com.pukio.model;

public class DetalleVenta {
    private int    idDetalle;
    private int    idVenta;
    private int    idProducto;
    private String codigoProducto;
    private String nombreProducto;
    private int    cantidad;
    private double precioUnit;
    private double descuento;
    private double subtotal;

    public DetalleVenta() {}

    public DetalleVenta(Producto p, int cantidad) {
        this.idProducto     = p.getIdProducto();
        this.codigoProducto = p.getCodigo();
        this.nombreProducto = p.getNombre();
        this.precioUnit     = p.getPrecioVenta();
        this.cantidad       = cantidad;
        this.descuento      = 0;
        calcularSubtotal();
    }

    public void calcularSubtotal() {
        this.subtotal = (precioUnit * cantidad) - descuento;
    }

    public int    getIdDetalle()              { return idDetalle; }
    public void   setIdDetalle(int v)         { this.idDetalle = v; }
    public int    getIdVenta()                { return idVenta; }
    public void   setIdVenta(int v)           { this.idVenta = v; }
    public int    getIdProducto()             { return idProducto; }
    public void   setIdProducto(int v)        { this.idProducto = v; }
    public String getCodigoProducto()         { return codigoProducto; }
    public void   setCodigoProducto(String v) { this.codigoProducto = v; }
    public String getNombreProducto()         { return nombreProducto; }
    public void   setNombreProducto(String v) { this.nombreProducto = v; }
    public int    getCantidad()               { return cantidad; }
    public void   setCantidad(int v)          { this.cantidad = v; calcularSubtotal(); }
    public double getPrecioUnit()             { return precioUnit; }
    public void   setPrecioUnit(double v)     { this.precioUnit = v; calcularSubtotal(); }
    public double getDescuento()              { return descuento; }
    public void   setDescuento(double v)      { this.descuento = v; calcularSubtotal(); }
    public double getSubtotal()               { return subtotal; }
    public void   setSubtotal(double v)       { this.subtotal = v; }
}
