package com.pukio.model;

import java.util.Date;

public class Usuario {
    private int    idUsuario;
    private String username;
    private String passwordHash;
    private String nombre;
    private String rol;
    private boolean activo;
    private Date   fechaCreacion;

    public Usuario() {}

    public Usuario(int idUsuario, String username, String nombre, String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.username  = username;
        this.nombre    = nombre;
        this.rol       = rol;
        this.activo    = activo;
    }

    public int     getIdUsuario()    { return idUsuario; }
    public void    setIdUsuario(int v){ this.idUsuario = v; }
    public String  getUsername()     { return username; }
    public void    setUsername(String v){ this.username = v; }
    public String  getPasswordHash() { return passwordHash; }
    public void    setPasswordHash(String v){ this.passwordHash = v; }
    public String  getNombre()       { return nombre; }
    public void    setNombre(String v){ this.nombre = v; }
    public String  getRol()          { return rol; }
    public void    setRol(String v)  { this.rol = v; }
    public boolean isActivo()        { return activo; }
    public void    setActivo(boolean v){ this.activo = v; }
    public Date    getFechaCreacion(){ return fechaCreacion; }
    public void    setFechaCreacion(Date v){ this.fechaCreacion = v; }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(rol); }

    @Override
    public String toString() { return nombre + " (" + rol + ")"; }
}
