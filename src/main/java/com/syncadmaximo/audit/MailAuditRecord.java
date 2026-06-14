package com.syncadmaximo.audit;

import java.time.Instant;

public class MailAuditRecord {
    private Long id;
    private Long runId;
    private Instant fechaEnvio;
    private String destinatarios;
    private String copias;
    private String asunto;
    private String estado;
    private String mensaje;
    private String detalleError;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Instant getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(Instant fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public String getDestinatarios() { return destinatarios; }
    public void setDestinatarios(String destinatarios) { this.destinatarios = destinatarios; }
    public String getCopias() { return copias; }
    public void setCopias(String copias) { this.copias = copias; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getDetalleError() { return detalleError; }
    public void setDetalleError(String detalleError) { this.detalleError = detalleError; }
}
