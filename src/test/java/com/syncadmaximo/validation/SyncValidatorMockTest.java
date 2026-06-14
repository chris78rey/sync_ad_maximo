package com.syncadmaximo.validation;

import com.syncadmaximo.ldap.LdapUser;
import com.syncadmaximo.testsupport.TestAssertions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SyncValidatorMockTest {

    private final SyncValidator validator = new SyncValidator();

    public static void runAll() {
        SyncValidatorMockTest test = new SyncValidatorMockTest();
        test.marksDuplicateWhenSameUserSameCedulaSameEmailAreRepeated();
        test.marksConflictWhenSameCedulaBelongsToDifferentUsers();
    }

    void marksDuplicateWhenSameUserSameCedulaSameEmailAreRepeated() {
        List<ValidationReport> reports = validator.validate(Arrays.asList(
                user("JDOE", "123456789", "JDOE@example.com", true),
                user(" jdoe ", "123456789", "jdoe@example.com", true)
        ));

        TestAssertions.equals(2, reports.size(), "Deben generarse dos reportes");
        for (ValidationReport report : reports) {
            TestAssertions.equals(ValidationReport.Status.REJECTED, report.getStatus(), "El duplicado debe quedar rechazado porque bloquea identidad");
            TestAssertions.isTrue(report.isDuplicate(), "El reporte debe quedar marcado como duplicado");
            TestAssertions.isTrue(report.isManualReview(), "El reporte duplicado debe requerir revisión manual");
            TestAssertions.isTrue(report.isIdentityBlocked(), "El duplicado debe bloquear identidad");
            TestAssertions.equals("0123456789", report.getNormalizedCedula(), "La cédula de 9 dígitos debe normalizarse con cero inicial");
            TestAssertions.contains(codes(report), "CEDULA_9_DIGITOS_NORMALIZADA", "Debe registrar la normalización de cédula");
            TestAssertions.contains(codes(report), "CEDULA_DUPLICADA", "Debe registrar cédula duplicada");
            TestAssertions.contains(codes(report), "USUARIO_DUPLICADO", "Debe registrar usuario duplicado");
            TestAssertions.contains(codes(report), "CORREO_DUPLICADO", "Debe registrar correo duplicado");
        }
    }

    void marksConflictWhenSameCedulaBelongsToDifferentUsers() {
        List<ValidationReport> reports = validator.validate(Arrays.asList(
                user("jdoe1", "123456789", "alpha@example.com", true),
                user("jdoe2", "123456789", "beta@example.com", true)
        ));

        TestAssertions.equals(2, reports.size(), "Deben generarse dos reportes");
        for (ValidationReport report : reports) {
            TestAssertions.equals(ValidationReport.Status.REJECTED, report.getStatus(), "El conflicto de cédula debe rechazar el registro");
            TestAssertions.isTrue(report.isConflict(), "El reporte debe quedar marcado como conflicto");
            TestAssertions.isTrue(report.isIdentityBlocked(), "El conflicto debe bloquear identidad");
            TestAssertions.contains(codes(report), "CEDULA_EN_CONFLICTO", "Debe registrar conflicto de cédula");
        }
    }

    private static String codes(ValidationReport report) {
        return report.getIssues().stream().map(ValidationReport.Issue::getCode).collect(Collectors.joining(","));
    }

    private static LdapUser user(String samAccountName, String postalCode, String mail, boolean enabled) {
        return new LdapUser(
                "cn=" + samAccountName + ",ou=people,dc=example,dc=local",
                samAccountName,
                mail,
                postalCode,
                samAccountName,
                "John",
                "Doe",
                enabled,
                Map.of()
        );
    }
}
