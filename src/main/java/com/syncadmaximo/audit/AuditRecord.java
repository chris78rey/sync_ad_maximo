package com.syncadmaximo.audit;

import java.time.Instant;

public class AuditRecord {
    private Long id;
    private Long runId;
    private Instant fechaEvento;
    private String modo;
    private String proceso;
    private String cedula;
    private String personIdMaximoAnterior;
    private String personIdMaximoNuevo;
    private String personIdAd;
    private String emailAnterior;
    private String emailNuevo;
    private String emailAd;
    private String estado;
    private String mensaje;
    private String detalleError;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Instant getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(Instant fechaEvento) { this.fechaEvento = fechaEvento; }
    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public String getProceso() { return proceso; }
    public void setProceso(String proceso) { this.proceso = proceso; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getPersonIdMaximoAnterior() { return personIdMaximoAnterior; }
    public void setPersonIdMaximoAnterior(String personIdMaximoAnterior) { this.personIdMaximoAnterior = personIdMaximoAnterior; }
    public String getPersonIdMaximoNuevo() { return personIdMaximoNuevo; }
    public void setPersonIdMaximoNuevo(String personIdMaximoNuevo) { this.personIdMaximoNuevo = personIdMaximoNuevo; }
    public String getPersonIdAd() { return personIdAd; }
    public void setPersonIdAd(String personIdAd) { this.personIdAd = personIdAd; }
    public String getEmailAnterior() { return emailAnterior; }
    public void setEmailAnterior(String emailAnterior) { this.emailAnterior = emailAnterior; }
    public String getEmailNuevo() { return emailNuevo; }
    public void setEmailNuevo(String emailNuevo) { this.emailNuevo = emailNuevo; }
    public String getEmailAd() { return emailAd; }
    public void setEmailAd(String emailAd) { this.emailAd = emailAd; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getDetalleError() { return detalleError; }
    public void setDetalleError(String detalleError) { this.detalleError = detalleError; }
}
