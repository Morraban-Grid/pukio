package com.pukio.model;

public class Categoria {
    private int    idCategoria;
    private String nombre;
    private String descripcion;
    private boolean activo;

    public Categoria() {}
    public Categoria(int id, String nombre) {
        this.idCategoria = id;
        this.nombre = nombre;
        this.activo = true;
    }

    public int     getIdCategoria()     { return idCategoria; }
    public void    setIdCategoria(int v){ this.idCategoria = v; }
    public String  getNombre()          { return nombre; }
    public void    setNombre(String v)  { this.nombre = v; }
    public String  getDescripcion()     { return descripcion; }
    public void    setDescripcion(String v){ this.descripcion = v; }
    public boolean isActivo()           { return activo; }
    public void    setActivo(boolean v) { this.activo = v; }

    @Override public String toString()  { return nombre; }
}
