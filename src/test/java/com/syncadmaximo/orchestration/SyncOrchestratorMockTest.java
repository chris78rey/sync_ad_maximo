package com.syncadmaximo.orchestration;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.model.SyncExecution;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.service.AuditRepository;
import com.syncadmaximo.service.DirectoryService;
import com.syncadmaximo.service.MailService;
import com.syncadmaximo.service.MaximoRepository;
import com.syncadmaximo.service.ValidationService;
import com.syncadmaximo.testsupport.TestAssertions;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public final class SyncOrchestratorMockTest {

    public static void runAll() {
        dryRunDoesNotPersistButAuditsAndSendsMail();
        productionPersistsAChangedCandidate();
        productionMigratesPersonIdWhenCedulaMatches();
    }

    static void dryRunDoesNotPersistButAuditsAndSendsMail() {
        RecordingMaximoRepository maximoRepository = new RecordingMaximoRepository();
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        RecordingMailService mailService = new RecordingMailService();
        SyncOrchestrator orchestrator = new SyncOrchestrator(
                appConfig(mode("DRY_RUN")),
                new SingleUserDirectoryService(sampleUser()),
                maximoRepository,
                new FixedValidationService(),
                auditRepository,
                mailService
        );

        SyncExecution execution = new SyncExecution();
        execution.setRunId("run-dry-1");
        execution.setStartedAt(Instant.parse("2026-01-01T10:00:00Z"));
        execution.setInitiatedBy("tester");

        SyncResult result = orchestrator.execute(execution);

        TestAssertions.isTrue(result.isSuccess(), "El dry-run debe terminar con éxito");
        TestAssertions.contains(result.getMessage(), "DRY_RUN", "El resumen debe indicar dry-run");
        TestAssertions.contains(result.getMessage(), "creados=1", "El resumen debe contar el alta simulada");
        TestAssertions.contains(result.getMessage(), "issues=1", "El resumen debe contar una novedad de dry-run");
        TestAssertions.equals(1, result.getIssues().size(), "Debe haber una novedad en dry-run");
        TestAssertions.equals("DRY_RUN", result.getIssues().get(0).getCode(), "La novedad debe ser de dry-run");
        TestAssertions.equals(0, maximoRepository.saveCalls, "No debe persistir en dry-run");
        TestAssertions.equals(1, auditRepository.startCalls, "Debe auditar el inicio");
        TestAssertions.equals(1, auditRepository.endCalls, "Debe auditar el fin");
        TestAssertions.equals(1, auditRepository.savedIssues.size(), "Debe auditar la novedad");
        TestAssertions.equals(1, mailService.sentBodies.size(), "Debe enviar un correo resumen");
        TestAssertions.contains(mailService.sentBodies.get(0), "Modo: DRY_RUN", "El correo debe indicar dry-run");
        TestAssertions.contains(mailService.sentBodies.get(0), "RunId: run-dry-1", "El correo debe incluir el runId");
    }

    static void productionPersistsAChangedCandidate() {
        RecordingMaximoRepository maximoRepository = new RecordingMaximoRepository();
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        RecordingMailService mailService = new RecordingMailService();
        SyncOrchestrator orchestrator = new SyncOrchestrator(
                appConfig(mode("PRODUCTION")),
                new SingleUserDirectoryService(sampleUser()),
                maximoRepository,
                new FixedValidationService(),
                auditRepository,
                mailService
        );

        SyncExecution execution = new SyncExecution();
        execution.setRunId("run-prod-1");
        execution.setStartedAt(Instant.parse("2026-01-01T11:00:00Z"));
        execution.setInitiatedBy("tester");

        SyncResult result = orchestrator.execute(execution);

        TestAssertions.isTrue(result.isSuccess(), "La ejecución en producción debe terminar con éxito");
        TestAssertions.contains(result.getMessage(), "PRODUCTION", "El resumen debe indicar producción");
        TestAssertions.contains(result.getMessage(), "creados=1", "Debe registrar una creación");
        TestAssertions.contains(result.getMessage(), "issues=0", "No debe haber novedades en producción limpia");
        TestAssertions.equals(0, result.getIssues().size(), "No debe haber issues en producción limpia");
        TestAssertions.equals(1, maximoRepository.saveCalls, "Debe persistir una vez");
        TestAssertions.notNull(maximoRepository.lastSaved, "Debe guardar un candidato");
        TestAssertions.equals("jdoe", maximoRepository.lastSaved.getPersonId(), "La persona debe normalizar el personId");
        TestAssertions.equals("ACTIVO", maximoRepository.lastSaved.getStatus(), "El usuario habilitado debe quedar activo");
        TestAssertions.equals("John", maximoRepository.lastSaved.getFirstName(), "Debe copiar el nombre");
        TestAssertions.equals("Doe", maximoRepository.lastSaved.getLastName(), "Debe copiar el apellido");
        TestAssertions.equals("0123456789", maximoRepository.lastSaved.getEppCedula(), "Debe copiar la cédula normalizada");
        TestAssertions.equals("jdoe@example.com", maximoRepository.lastSaved.getEmailAddress(), "Debe normalizar el correo a minúsculas");
        TestAssertions.equals(1, auditRepository.startCalls, "Debe auditar el inicio");
        TestAssertions.equals(1, auditRepository.endCalls, "Debe auditar el fin");
        TestAssertions.equals(0, auditRepository.savedIssues.size(), "No debe auditar novedad alguna");
        TestAssertions.equals(1, mailService.sentBodies.size(), "Debe enviar un correo resumen");
        TestAssertions.contains(mailService.sentBodies.get(0), "Modo: PRODUCTION", "El correo debe indicar producción");
    }

    static void productionMigratesPersonIdWhenCedulaMatches() {
        RecordingMaximoRepository maximoRepository = new RecordingMaximoRepository();
        maximoRepository.peopleByStatus = Collections.singletonList(existingMigratablePerson());
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        RecordingMailService mailService = new RecordingMailService();
        SyncOrchestrator orchestrator = new SyncOrchestrator(
                appConfig(mode("PRODUCTION")),
                new SingleUserDirectoryService(migratedUser()),
                maximoRepository,
                new FixedValidationService(),
                auditRepository,
                mailService
        );

        SyncExecution execution = new SyncExecution();
        execution.setRunId("run-migrate-1");
        execution.setStartedAt(Instant.parse("2026-01-01T12:00:00Z"));
        execution.setInitiatedBy("tester");

        SyncResult result = orchestrator.execute(execution);

        TestAssertions.isTrue(result.isSuccess(), "La migración debe terminar con éxito");
        TestAssertions.contains(result.getMessage(), "matchCedula=1", "Debe contar un match por cédula");
        TestAssertions.contains(result.getMessage(), "actualizados=1", "Debe registrar una actualización");
        TestAssertions.equals(1, maximoRepository.updatePersonIdCalls, "Debe migrar el personId una vez");
        TestAssertions.equals("oldlogin", maximoRepository.lastCurrentPersonId, "Debe migrar desde el login anterior");
        TestAssertions.equals("newlogin", maximoRepository.lastNewPersonId, "Debe migrar hacia el nuevo login");
        TestAssertions.equals(1, maximoRepository.saveCalls, "Debe persistir el candidato migrado");
        TestAssertions.notNull(maximoRepository.lastSaved, "Debe guardar el candidato migrado");
        TestAssertions.equals("newlogin", maximoRepository.lastSaved.getPersonId(), "El candidato debe usar el nuevo login");
        TestAssertions.equals("0123456789", maximoRepository.lastSaved.getEppCedula(), "Debe conservar la cédula");
        TestAssertions.equals(1, auditRepository.startCalls, "Debe auditar el inicio");
        TestAssertions.equals(1, auditRepository.endCalls, "Debe auditar el fin");
        TestAssertions.equals(1, mailService.sentBodies.size(), "Debe enviar un correo resumen");
        TestAssertions.contains(mailService.sentBodies.get(0), "RunId: run-migrate-1", "El correo debe incluir el runId");
    }

    private static AdUser sampleUser() {
        AdUser user = new AdUser();
        user.setsAMAccountName("jdoe");
        user.setGivenName("John");
        user.setSn("Doe");
        user.setPostalCode("0123456789");
        user.setMail("JDOE@EXAMPLE.COM");
        user.setEnabled(true);
        return user;
    }

    private static AdUser migratedUser() {
        AdUser user = new AdUser();
        user.setsAMAccountName("newlogin");
        user.setGivenName("John");
        user.setSn("Doe");
        user.setPostalCode("0123456789");
        user.setMail("newlogin@example.com");
        user.setEnabled(true);
        return user;
    }

    private static MaximoPerson existingMigratablePerson() {
        MaximoPerson person = new MaximoPerson();
        person.setPersonId("oldlogin");
        person.setStatus("ACTIVO");
        person.setFirstName("John");
        person.setLastName("Doe");
        person.setEppCedula("0123456789");
        person.setEmailAddress("oldlogin@example.com");
        return person;
    }

    private static Properties mode(String value) {
        Properties properties = new Properties();
        properties.setProperty("sync.execution.mode", value);
        properties.setProperty("app.name", "sync-ad-maximo");
        properties.setProperty("sync.maximo.statuses", "ACTIVO,INACTIVO");
        return properties;
    }

    private static AppConfig appConfig(Properties properties) {
        try {
            Constructor<AppConfig> constructor = AppConfig.class.getDeclaredConstructor(Properties.class);
            constructor.setAccessible(true);
            return constructor.newInstance(properties);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo construir un AppConfig de prueba", ex);
        }
    }

    private static final class SingleUserDirectoryService implements DirectoryService {
        private final List<AdUser> users;

        private SingleUserDirectoryService(AdUser user) {
            this.users = Collections.singletonList(user);
        }

        @Override
        public List<AdUser> findEnabledUsers() {
            return users;
        }

        @Override
        public List<AdUser> findDisabledUsers() {
            return Collections.emptyList();
        }

        @Override
        public Optional<AdUser> findByUserName(String userName) {
            return users.stream().filter(user -> user.getsAMAccountName().equalsIgnoreCase(userName)).findFirst();
        }
    }

    private static final class FixedValidationService implements ValidationService {
        @Override
        public boolean isValidCedula(String cedula) {
            return cedula != null && cedula.length() == 10;
        }

        @Override
        public boolean isValidEmail(String email) {
            return email != null && email.contains("@");
        }

        @Override
        public boolean isValidUserName(String userName) {
            return userName != null && !userName.trim().isEmpty();
        }

        @Override
        public Optional<String> normalizeCedula(String cedula) {
            return Optional.ofNullable(cedula == null ? null : cedula.trim());
        }

        @Override
        public boolean matchesByCedula(AdUser adUser, MaximoPerson maximoPerson) {
            return false;
        }
    }

    private static final class RecordingMaximoRepository implements MaximoRepository {
        private int saveCalls;
        private int updatePersonIdCalls;
        private MaximoPerson lastSaved;
        private final List<String> requestedStatuses = new ArrayList<>();
        private List<MaximoPerson> peopleByStatus = Collections.emptyList();
        private String lastCurrentPersonId;
        private String lastNewPersonId;

        @Override
        public List<MaximoPerson> findPeopleByStatus(String status) {
            requestedStatuses.add(status);
            return peopleByStatus;
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

        public void insertConfiguredPrimaryEmail(String personId, String emailAddress) {
            // no-op for tests
        }

        @Override
        public int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) {
            updatePersonIdCalls++;
            lastCurrentPersonId = currentPersonId;
            lastNewPersonId = newPersonId;
            return 1;
        }
    }

    private static final class RecordingAuditRepository implements AuditRepository {
        private int startCalls;
        private int endCalls;
        private final List<SyncIssue> savedIssues = new ArrayList<>();

        @Override
        public void saveExecutionStart(SyncExecution execution) {
            startCalls++;
        }

        @Override
        public void saveExecutionEnd(SyncExecution execution) {
            endCalls++;
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

    private static final class RecordingMailService implements MailService {
        private final List<String> sentBodies = new ArrayList<>();

        @Override
        public void sendExecutionSummary(SyncExecution execution, String body) {
            sentBodies.add(body);
        }
    }
}
