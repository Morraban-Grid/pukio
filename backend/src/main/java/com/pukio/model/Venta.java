package com.pukio.model;

import com.pukio.config.AppConfig;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Venta {
    private int    idVenta;
    private String numeroComprobante;
    private String tipoComprobante;
    private int    idCliente;
    private String nombreCliente;
    private int    idUsuario;
    private String nombreUsuario;
    private String nombreCajero;
    private Date   fechaVenta;
    private double subtotal;
    private double igv;
    private double descuento;
    private double total;
    private String metodoPago;
    private String estado;
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
        this.tipoComprobante = "BOLETA";
        this.metodoPago      = "EFECTIVO";
        this.estado          = "COMPLETADA";
        this.fechaVenta      = new Date();
        this.descuento       = 0;
    }

    public void calcularTotales() {
        double base = detalles.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
        this.subtotal = base - this.descuento;
        this.igv      = this.subtotal * AppConfig.IGV_TASA;
        this.total    = this.subtotal + this.igv;
    }

    public void agregarDetalle(DetalleVenta d)  { detalles.add(d); }
    public void eliminarDetalle(int index)       { detalles.remove(index); }

    public int    getIdVenta()                   { return idVenta; }
    public void   setIdVenta(int v)              { this.idVenta = v; }
    public String getNumeroComprobante()         { return numeroComprobante; }
    public void   setNumeroComprobante(String v) { this.numeroComprobante = v; }
    public String getTipoComprobante()           { return tipoComprobante; }
    public void   setTipoComprobante(String v)   { this.tipoComprobante = v; }
    public int    getIdCliente()                 { return idCliente; }
    public void   setIdCliente(int v)            { this.idCliente = v; }
    public String getNombreCliente()             { return nombreCliente; }
    public void   setNombreCliente(String v)     { this.nombreCliente = v; }
    public int    getIdUsuario()                 { return idUsuario; }
    public void   setIdUsuario(int v)            { this.idUsuario = v; }
    public String getNombreUsuario()             { return nombreUsuario; }
    public void   setNombreUsuario(String v)     { this.nombreUsuario = v; }
    public String getNombreCajero()              { return nombreCajero != null ? nombreCajero : nombreUsuario; }
    public void   setNombreCajero(String v)      { this.nombreCajero = v; }
    public Date   getFechaVenta()                { return fechaVenta; }
    public void   setFechaVenta(Date v)          { this.fechaVenta = v; }
    public double getSubtotal()                  { return subtotal; }
    public void   setSubtotal(double v)          { this.subtotal = v; }
    public double getIgv()                       { return igv; }
    public void   setIgv(double v)               { this.igv = v; }
    public double getDescuento()                 { return descuento; }
    public void   setDescuento(double v)         { this.descuento = v; }
    public double getTotal()                     { return total; }
    public void   setTotal(double v)             { this.total = v; }
    public String getMetodoPago()                { return metodoPago; }
    public void   setMetodoPago(String v)        { this.metodoPago = v; }
    public String getEstado()                    { return estado; }
    public void   setEstado(String v)            { this.estado = v; }
    public List<DetalleVenta> getDetalles()      { return detalles; }
    public void   setDetalles(List<DetalleVenta> v){ this.detalles = v; }
}
