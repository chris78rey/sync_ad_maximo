package com.syncadmaximo.audit;

import java.time.Instant;

public class RunRecord {
    private Long runId;
    private Instant fechaInicio;
    private Instant fechaFin;
    private String modo;
    private String proceso;
    private String usuarioEjecutor;
    private String origenEjecucion;
    private String estado;
    private int totalMaximo;
    private int totalAd;
    private int totalMigrados;
    private int totalCreados;
    private int totalInactivados;
    private int totalEmailActualizados;
    private int totalEmailInsertados;
    private int totalSinCambios;
    private int totalObservados;
    private int totalErrores;
    private String mensaje;

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public String getProceso() { return proceso; }
    public void setProceso(String proceso) { this.proceso = proceso; }
    public String getUsuarioEjecutor() { return usuarioEjecutor; }
    public void setUsuarioEjecutor(String usuarioEjecutor) { this.usuarioEjecutor = usuarioEjecutor; }
    public String getOrigenEjecucion() { return origenEjecucion; }
    public void setOrigenEjecucion(String origenEjecucion) { this.origenEjecucion = origenEjecucion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getTotalMaximo() { return totalMaximo; }
    public void setTotalMaximo(int totalMaximo) { this.totalMaximo = totalMaximo; }
    public int getTotalAd() { return totalAd; }
    public void setTotalAd(int totalAd) { this.totalAd = totalAd; }
    public int getTotalMigrados() { return totalMigrados; }
    public void setTotalMigrados(int totalMigrados) { this.totalMigrados = totalMigrados; }
    public int getTotalCreados() { return totalCreados; }
    public void setTotalCreados(int totalCreados) { this.totalCreados = totalCreados; }
    public int getTotalInactivados() { return totalInactivados; }
    public void setTotalInactivados(int totalInactivados) { this.totalInactivados = totalInactivados; }
    public int getTotalEmailActualizados() { return totalEmailActualizados; }
    public void setTotalEmailActualizados(int totalEmailActualizados) { this.totalEmailActualizados = totalEmailActualizados; }
    public int getTotalEmailInsertados() { return totalEmailInsertados; }
    public void setTotalEmailInsertados(int totalEmailInsertados) { this.totalEmailInsertados = totalEmailInsertados; }
    public int getTotalSinCambios() { return totalSinCambios; }
    public void setTotalSinCambios(int totalSinCambios) { this.totalSinCambios = totalSinCambios; }
    public int getTotalObservados() { return totalObservados; }
    public void setTotalObservados(int totalObservados) { this.totalObservados = totalObservados; }
    public int getTotalErrores() { return totalErrores; }
    public void setTotalErrores(int totalErrores) { this.totalErrores = totalErrores; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
