package com.pukio.model;

public class Proveedor {
    private int    idProveedor;
    private String ruc;
    private String nombre;
    private String contacto;
    private String telefono;
    private String correo;
    private String direccion;
    private boolean activo;

    public Proveedor() {}

    public int     getIdProveedor()          { return idProveedor; }
    public void    setIdProveedor(int v)     { this.idProveedor = v; }
    public String  getRuc()                  { return ruc; }
    public void    setRuc(String v)          { this.ruc = v; }
    public String  getNombre()               { return nombre; }
    public void    setNombre(String v)       { this.nombre = v; }
    public String  getContacto()             { return contacto; }
    public void    setContacto(String v)     { this.contacto = v; }
    public String  getTelefono()             { return telefono; }
    public void    setTelefono(String v)     { this.telefono = v; }
    public String  getCorreo()               { return correo; }
    public void    setCorreo(String v)       { this.correo = v; }
    public String  getDireccion()            { return direccion; }
    public void    setDireccion(String v)    { this.direccion = v; }
    public boolean isActivo()                { return activo; }
    public void    setActivo(boolean v)      { this.activo = v; }

    @Override public String toString()       { return nombre + " (" + ruc + ")"; }
}
