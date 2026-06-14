package com.syncadmaximo.web.demo;

import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.web.security.WebPrincipal;

import java.time.Instant;
import java.util.Objects;

public final class DemoDataFactory {

    private DemoDataFactory() {
    }

    public static DemoData create() {
        return create(Instant.now());
    }

    public static DemoData create(Instant referenceTime) {
        Instant safeReferenceTime = Objects.requireNonNull(referenceTime, "referenceTime");

        WebPrincipal principal = new WebPrincipal("maxadmin", "demo-user", "maxadmin");

        SyncExecution execution = new SyncExecution();
        execution.setRunId("demo-20260614-150000");
        execution.setProcessName("Demo completo sync_ad_maximo");
        execution.setInitiatedBy("demo");
        execution.setDryRun(true);
        execution.setStartedAt(safeReferenceTime);
        execution.setFinishedAt(safeReferenceTime.plusSeconds(173));

        SyncResult result = new SyncResult();
        result.setRunId(execution.getRunId());
        result.setSuccess(true);
        result.setMessage("Demo completo listo para probar reporte, CSV y correo.");

        SyncIssue issue1 = new SyncIssue();
        issue1.setCode("DEMO_CARGA");
        issue1.setDescription("Se cargó un dataset simulado con usuarios activos, inactivos y duplicados controlados.");
        issue1.setReferenceId("demo-ldap");
        issue1.setCreatedAt(safeReferenceTime.plusSeconds(15));
        result.addIssue(issue1);

        SyncIssue issue2 = new SyncIssue();
        issue2.setCode("CEDULA_DUPLICADA");
        issue2.setDescription("Dos usuarios comparten una cédula para mostrar la lógica de validación.");
        issue2.setReferenceId("jdoe2");
        issue2.setCreatedAt(safeReferenceTime.plusSeconds(35));
        result.addIssue(issue2);

        SyncIssue issue3 = new SyncIssue();
        issue3.setCode("DRY_RUN");
        issue3.setDescription("La corrida se mantiene en modo simulación y no persiste cambios en MAXIMO.");
        issue3.setReferenceId("demo-run");
        issue3.setCreatedAt(safeReferenceTime.plusSeconds(55));
        result.addIssue(issue3);

        return new DemoData(principal, execution, result);
    }
}
