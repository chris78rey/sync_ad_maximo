package com.syncadmaximo.service;

import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.orchestration.SynchronizationPlan;
import com.syncadmaximo.testsupport.TestAssertions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class IdentityMatchingServiceSpec {

    public static void runAll() {
        resolvesByCedulaAndMigratesLogin();
        auditsMatchConflicts();
    }

    static void resolvesByCedulaAndMigratesLogin() {
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        RecordingMaximoRepository repository = new RecordingMaximoRepository();
        IdentityMatchingService service = new IdentityMatchingService(repository, auditRepository);
        SynchronizationPlan plan = new SynchronizationPlan();
        SyncResult result = new SyncResult();
        result.setRunId("run-1");

        List<MaximoPerson> people = Collections.singletonList(person("oldlogin", "0123456789", "oldlogin@example.com"));
        IdentityMatchingService.MatchResult match = service.resolve("newlogin", "0123456789", "newlogin@example.com", people, plan, result);

        TestAssertions.equals(IdentityMatchingService.MatchType.CEDULA, match.getType(), "Debe resolver por cédula");
        TestAssertions.equals("oldlogin", match.getPerson().getPersonId(), "Debe encontrar la persona existente");

        boolean migrated = service.migrateByCedula(match, "0123456789", "newlogin", new java.util.LinkedHashMap<String, MaximoPerson>(), plan, result);

        TestAssertions.isTrue(migrated, "Debe migrar el login");
        TestAssertions.equals(1, repository.updateCalls, "Debe ejecutar una actualización");
        TestAssertions.equals("oldlogin", repository.lastCurrentPersonId, "Debe migrar desde el login anterior");
        TestAssertions.equals("newlogin", repository.lastNewPersonId, "Debe migrar hacia el nuevo login");
        TestAssertions.equals("newlogin", match.getPerson().getPersonId(), "El match debe actualizarse");
        TestAssertions.equals(0, auditRepository.savedIssues.size(), "No debe auditar issues en una migración limpia");
    }

    static void auditsMatchConflicts() {
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        RecordingMaximoRepository repository = new RecordingMaximoRepository();
        IdentityMatchingService service = new IdentityMatchingService(repository, auditRepository);
        SynchronizationPlan plan = new SynchronizationPlan();
        SyncResult result = new SyncResult();
        result.setRunId("run-2");

        List<MaximoPerson> people = new ArrayList<>();
        people.add(person("jdoe", "2222222222", "jdoe@example.com"));
        people.add(person("other", "1111111111", "other@example.com"));

        IdentityMatchingService.MatchResult match = service.resolve("jdoe", "1111111111", "jdoe@example.com", people, plan, result);

        TestAssertions.equals(IdentityMatchingService.MatchType.USER, match.getType(), "Debe resolver por usuario");
        TestAssertions.isTrue(result.getIssues().size() >= 1, "Debe registrar al menos un issue");
        TestAssertions.contains(result.getIssues().get(0).getCode(), "MATCH_CONFLICT", "Debe marcar conflicto");
        TestAssertions.equals(2, auditRepository.savedIssues.size(), "Debe auditar ambos conflictos");
    }

    private static MaximoPerson person(String personId, String cedula, String email) {
        MaximoPerson person = new MaximoPerson();
        person.setPersonId(personId);
        person.setStatus("ACTIVO");
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setEppCedula(cedula);
        person.setEmailAddress(email);
        return person;
    }

    private static final class RecordingMaximoRepository implements com.syncadmaximo.repository.MaximoRepository {
        private int updateCalls;
        private String lastCurrentPersonId;
        private String lastNewPersonId;

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
        public int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) throws SQLException {
            updateCalls++;
            lastCurrentPersonId = currentPersonId;
            lastNewPersonId = newPersonId;
            return 1;
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
            // no-op
        }
    }

    private static final class RecordingAuditRepository implements AuditRepository {
        private final List<SyncIssue> savedIssues = new ArrayList<>();

        @Override
        public void saveExecutionStart(SyncExecution execution) {
            // no-op
        }

        @Override
        public void saveExecutionEnd(SyncExecution execution) {
            // no-op
        }

        @Override
        public void saveIssue(String runId, SyncIssue issue) {
            savedIssues.add(issue);
        }

        @Override
        public List<SyncIssue> findIssuesByRunId(String runId) {
            return new ArrayList<>(savedIssues);
        }
    }
}
