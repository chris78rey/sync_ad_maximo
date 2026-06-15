package com.syncadmaximo.service;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.orchestration.SynchronizationPlan;
import com.syncadmaximo.testsupport.TestAssertions;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public final class SyncDataLoadServiceSpec {

    public static void runAll() {
        loadsAndAuditsDirectoryAndMaximoData();
    }

    static void loadsAndAuditsDirectoryAndMaximoData() {
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        RecordingDirectoryService directoryService = new RecordingDirectoryService();
        RecordingMaximoRepository maximoRepository = new RecordingMaximoRepository();
        SyncDataLoadService service = new SyncDataLoadService(directoryService, maximoRepository, appConfig(), auditRepository);

        SynchronizationPlan plan = new SynchronizationPlan();
        SyncResult result = new SyncResult();
        result.setRunId("run-load-1");

        SyncDataLoadService.LoadedData loaded = service.load(plan, result);

        TestAssertions.equals(2, loaded.getAdUsers().size(), "Debe cargar dos usuarios válidos");
        TestAssertions.equals(1, loaded.getMaximoPeople().size(), "Debe consolidar MAXIMO por personId");
        TestAssertions.equals(2, plan.getAdLoaded(), "Debe contar dos usuarios AD válidos");
        TestAssertions.equals(1, plan.getMaximoLoaded(), "Debe contar una persona MAXIMO");
        TestAssertions.equals(1, result.getIssues().size(), "Debe registrar un usuario AD inválido");
        TestAssertions.equals(1, auditRepository.savedIssues.size(), "Debe auditar el issue");
        TestAssertions.equals("AD_USER_INVALID", result.getIssues().get(0).getCode(), "Debe marcar usuario inválido");
    }

    private static AppConfig appConfig() {
        try {
            Properties properties = new Properties();
            properties.setProperty("sync.maximo.statuses", "ACTIVO,INACTIVO");
            Constructor<AppConfig> constructor = AppConfig.class.getDeclaredConstructor(Properties.class);
            constructor.setAccessible(true);
            return constructor.newInstance(properties);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo construir un AppConfig de prueba", ex);
        }
    }

    private static AdUser user(String samAccountName, boolean enabled) {
        AdUser user = new AdUser();
        user.setsAMAccountName(samAccountName);
        user.setGivenName("John");
        user.setSn("Doe");
        user.setMail("jdoe@example.com");
        user.setPostalCode("0123456789");
        user.setEnabled(enabled);
        return user;
    }

    private static MaximoPerson person(String personId, String status) {
        MaximoPerson person = new MaximoPerson();
        person.setPersonId(personId);
        person.setStatus(status);
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setEppCedula("0123456789");
        person.setEmailAddress("jdoe@example.com");
        return person;
    }

    private static final class RecordingDirectoryService implements DirectoryService {
        @Override
        public List<AdUser> findEnabledUsers() {
            List<AdUser> users = new ArrayList<>();
            users.add(user("jdoe", true));
            users.add(user("   ", true));
            return users;
        }

        @Override
        public List<AdUser> findDisabledUsers() {
            return Collections.singletonList(user("jane", false));
        }

        @Override
        public Optional<AdUser> findByUserName(String userName) {
            return Optional.empty();
        }
    }

    private static final class RecordingMaximoRepository implements com.syncadmaximo.repository.MaximoRepository {
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
            if ("ACTIVO".equalsIgnoreCase(status)) {
                return Collections.singletonList(person("jdoe", "ACTIVO"));
            }
            if ("INACTIVO".equalsIgnoreCase(status)) {
                return Collections.singletonList(person("jdoe", "INACTIVO"));
            }
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
