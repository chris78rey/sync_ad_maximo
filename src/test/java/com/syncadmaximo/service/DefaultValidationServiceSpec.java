package com.syncadmaximo.service;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.testsupport.TestAssertions;

import java.util.Optional;

public final class DefaultValidationServiceSpec {

    public static void runAll() {
        normalizesCedulaAndMatchesByCedula();
        validatesBasicInputs();
    }

    static void normalizesCedulaAndMatchesByCedula() {
        DefaultValidationService service = new DefaultValidationService();
        Optional<String> cedula = service.normalizeCedula("123456789");
        TestAssertions.isTrue(cedula.isPresent(), "La cédula debe normalizarse");
        TestAssertions.equals("0123456789", cedula.get(), "Debe anteponer cero a 9 dígitos");

        AdUser adUser = new AdUser();
        adUser.setPostalCode("0123456789");
        MaximoPerson person = new MaximoPerson();
        person.setEppCedula("0123456789");
        TestAssertions.isTrue(service.matchesByCedula(adUser, person), "Debe coincidir por cédula");
    }

    static void validatesBasicInputs() {
        DefaultValidationService service = new DefaultValidationService();
        TestAssertions.isTrue(service.isValidUserName("jdoe"), "Un username simple debe ser válido");
        TestAssertions.isTrue(service.isValidEmail("jdoe@example.com"), "Un correo simple debe ser válido");
        TestAssertions.isTrue(service.isValidCedula("0123456789"), "Una cédula de 10 dígitos debe ser válida");
        TestAssertions.isFalse(service.isValidUserName("   "), "Un username vacío no debe ser válido");
        TestAssertions.isFalse(service.isValidEmail("correo-invalido"), "Un correo inválido debe rechazarse");
        TestAssertions.isFalse(service.isValidCedula("123"), "Una cédula corta debe rechazarse");
    }
}
