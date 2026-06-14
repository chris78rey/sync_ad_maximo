package com.syncadmaximo.web.demo;

import com.syncadmaximo.testsupport.TestAssertions;

import java.time.Instant;

public final class DemoDataFactoryMockTest {

    public static void runAll() {
        new DemoDataFactoryMockTest().createsACompleteDemoSnapshot();
    }

    void createsACompleteDemoSnapshot() {
        Instant referenceTime = Instant.parse("2026-06-14T15:00:00Z");
        DemoData demoData = DemoDataFactory.create(referenceTime);

        TestAssertions.notNull(demoData, "Debe crearse un demo completo");
        TestAssertions.equals("maxadmin", demoData.getPrincipal().getUserName(), "El demo debe entrar como maxadmin");
        TestAssertions.equals("demo-user", demoData.getPrincipal().getDisplayName(), "El demo debe tener nombre de visualización demo");
        TestAssertions.equals("maxadmin", demoData.getPrincipal().getRole(), "El demo debe conservar el rol permitido");

        TestAssertions.equals("demo-20260614-150000", demoData.getExecution().getRunId(), "El runId del demo debe ser estable");
        TestAssertions.equals("Demo completo sync_ad_maximo", demoData.getExecution().getProcessName(), "El proceso del demo debe identificarse claramente");
        TestAssertions.equals("demo", demoData.getExecution().getInitiatedBy(), "El demo debe indicar quién inició la sesión");
        TestAssertions.isTrue(demoData.getExecution().isDryRun(), "El demo debe correr en dry-run");
        TestAssertions.equals(referenceTime, demoData.getExecution().getStartedAt(), "El demo debe usar el instante de referencia como inicio");
        TestAssertions.equals(referenceTime.plusSeconds(173), demoData.getExecution().getFinishedAt(), "El demo debe fijar un fin reproducible");

        TestAssertions.isTrue(demoData.getResult().isSuccess(), "El demo debe mostrarse como exitoso");
        TestAssertions.equals("Demo completo listo para probar reporte, CSV y correo.", demoData.getResult().getMessage(), "El mensaje debe explicar el demo");
        TestAssertions.equals(3, demoData.getResult().getIssueCount(), "El demo debe incluir issues representativos");
    }
}
