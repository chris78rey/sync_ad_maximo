package com.syncadmaximo.audit;

import java.time.Instant;

public class AccessAuditRecord {
    private Long id;
    private Instant fechaEvento;
    private String usuario;
    private String ipOrigen;
    private String accion;
    private String estado;
    private String mensaje;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(Instant fechaEvento) { this.fechaEvento = fechaEvento; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
