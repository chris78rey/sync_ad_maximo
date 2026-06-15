package com.syncadmaximo.service;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.testsupport.TestAssertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class InactivationServiceTest {

    public static void runAll() {
        inactivatesActivePersonWhenDirectoryUserIsDisabled();
        skipsWhenAlreadyInactive();
        skipsWhenDirectoryUserIsEnabled();
    }

    static void inactivatesActivePersonWhenDirectoryUserIsDisabled() {
        RecordingMaximoRepository repository = new RecordingMaximoRepository();
        InactivationService service = new InactivationService(repository);

        MaximoPerson person = activePerson();
        AdUser adUser = disabledUser();

        InactivationService.Outcome outcome = service.inactivateIfNeeded(person, adUser);

        TestAssertions.equals(InactivationService.Outcome.UPDATED, outcome, "Debe inactivar la persona activa");
        TestAssertions.equals(1, repository.saveCalls, "Debe persistir el cambio");
        TestAssertions.equals("INACTIVO", repository.lastSaved.getStatus(), "Debe cambiar el estado a INACTIVO");
    }

    static void skipsWhenAlreadyInactive() {
        RecordingMaximoRepository repository = new RecordingMaximoRepository();
        InactivationService service = new InactivationService(repository);

        MaximoPerson person = inactivePerson();

        InactivationService.Outcome outcome = service.inactivateIfNeeded(person, disabledUser());

        TestAssertions.equals(InactivationService.Outcome.UNCHANGED, outcome, "No debe tocar una persona ya inactiva");
        TestAssertions.equals(0, repository.saveCalls, "No debe persistir cambios innecesarios");
    }

    static void skipsWhenDirectoryUserIsEnabled() {
        RecordingMaximoRepository repository = new RecordingMaximoRepository();
        InactivationService service = new InactivationService(repository);

        InactivationService.Outcome outcome = service.inactivateIfNeeded(activePerson(), enabledUser());

        TestAssertions.equals(InactivationService.Outcome.SKIPPED, outcome, "Un usuario habilitado no debe inactivarse");
        TestAssertions.equals(0, repository.saveCalls, "No debe persistir nada");
    }

    private static AdUser disabledUser() {
        AdUser user = new AdUser();
        user.setsAMAccountName("jdoe");
        user.setEnabled(false);
        return user;
    }

    private static AdUser enabledUser() {
        AdUser user = new AdUser();
        user.setsAMAccountName("jdoe");
        user.setEnabled(true);
        return user;
    }

    private static MaximoPerson activePerson() {
        MaximoPerson person = new MaximoPerson();
        person.setPersonId("jdoe");
        person.setStatus("ACTIVO");
        return person;
    }

    private static MaximoPerson inactivePerson() {
        MaximoPerson person = new MaximoPerson();
        person.setPersonId("jdoe");
        person.setStatus("INACTIVO");
        return person;
    }

    private static final class RecordingMaximoRepository implements com.syncadmaximo.repository.MaximoRepository {
        private int saveCalls;
        private MaximoPerson lastSaved;

        @Override
        public Optional<String> findActivePersonIdByCedula(String cedula) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findCedulaByPersonId(String personId) {
            return Optional.empty();
        }

        @Override
        public Optional<String> findPrimaryEmailByPersonId(String personId) {
            return Optional.empty();
        }

        @Override
        public boolean isEmailAssignedToDifferentPerson(String emailAddress, String personId) {
            return false;
        }

        @Override
        public int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) {
            return 0;
        }

        @Override
        public int updatePrimaryEmail(String personId, String emailAddress) {
            return 0;
        }

        @Override
        public int insertPrimaryEmail(long emailId, long rowstamp, String personId, String emailAddress, String type, boolean primary) {
            return 0;
        }

        @Override
        public void insertConfiguredPrimaryEmail(String personId, String emailAddress) {
            // no-op
        }

        @Override
        public long nextSequenceValue(String sequenceName) {
            return 0;
        }

        @Override
        public void callStoredProcedure(String procedureName, List<?> parameters) {
            // no-op
        }

        @Override
        public List<MaximoPerson> findPeopleByStatus(String status) {
            return Collections.emptyList();
        }

        @Override
        public Optional<MaximoPerson> findByPersonId(String personId) {
            return Optional.empty();
        }

        @Override
        public void saveOrUpdate(MaximoPerson person) {
            saveCalls++;
            lastSaved = person;
        }
    }
}
