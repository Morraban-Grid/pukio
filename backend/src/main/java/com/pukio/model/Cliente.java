package com.pukio.model;

import java.util.Date;

public class Cliente {
    private int    idCliente;
    private String tipoDoc;
    private String numeroDoc;
    private String nombre;
    private String telefono;
    private String correo;
    private String direccion;
    private boolean activo;
    private Date   fechaRegistro;

    public Cliente() {}
    public Cliente(int id, String tipoDoc, String numeroDoc, String nombre) {
        this.idCliente = id;
        this.tipoDoc   = tipoDoc;
        this.numeroDoc = numeroDoc;
        this.nombre    = nombre;
        this.activo    = true;
    }

    public int     getIdCliente()           { return idCliente; }
    public void    setIdCliente(int v)      { this.idCliente = v; }
    public String  getTipoDoc()             { return tipoDoc; }
    public void    setTipoDoc(String v)     { this.tipoDoc = v; }
    public String  getNumeroDoc()           { return numeroDoc; }
    public void    setNumeroDoc(String v)   { this.numeroDoc = v; }
    public String  getNombre()              { return nombre; }
    public void    setNombre(String v)      { this.nombre = v; }
    public String  getTelefono()            { return telefono; }
    public void    setTelefono(String v)    { this.telefono = v; }
    public String  getCorreo()              { return correo; }
    public void    setCorreo(String v)      { this.correo = v; }
    public String  getDireccion()           { return direccion; }
    public void    setDireccion(String v)   { this.direccion = v; }
    public boolean isActivo()               { return activo; }
    public void    setActivo(boolean v)     { this.activo = v; }
    public Date    getFechaRegistro()       { return fechaRegistro; }
    public void    setFechaRegistro(Date v) { this.fechaRegistro = v; }

    @Override public String toString() { return nombre + " (" + tipoDoc + ": " + numeroDoc + ")"; }
}
