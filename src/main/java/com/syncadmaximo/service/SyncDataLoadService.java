package com.syncadmaximo.service;

import com.syncadmaximo.config.AppConfig;
import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.model.SyncIssue;
import com.syncadmaximo.model.SyncResult;
import com.syncadmaximo.orchestration.SynchronizationPlan;
import com.syncadmaximo.util.StringSanitizer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Carga y normaliza la información de AD y MAXIMO antes del procesamiento.
 */
public class SyncDataLoadService {

    private final DirectoryService directoryService;
    private final MaximoRepository maximoRepository;
    private final AppConfig config;
    private final AuditRepository auditRepository;

    public SyncDataLoadService(DirectoryService directoryService, MaximoRepository maximoRepository, AppConfig config, AuditRepository auditRepository) {
        this.directoryService = directoryService == null ? new NoOpDirectoryService() : directoryService;
        this.maximoRepository = maximoRepository == null ? new NoOpMaximoRepository() : maximoRepository;
        this.config = config == null ? AppConfig.getInstance() : config;
        this.auditRepository = auditRepository == null ? new NoOpAuditRepository() : auditRepository;
    }

    public LoadedData load(SynchronizationPlan plan, SyncResult result) {
        Map<String, AdUser> adUsers = loadDirectoryUsers(plan, result);
        Map<String, MaximoPerson> maximoPeople = loadMaximoPeople(plan);
        return new LoadedData(adUsers, maximoPeople);
    }

    private Map<String, AdUser> loadDirectoryUsers(SynchronizationPlan plan, SyncResult result) {
        Map<String, AdUser> users = new LinkedHashMap<String, AdUser>();
        List<AdUser> enabled = safeList(directoryService.findEnabledUsers());
        List<AdUser> disabled = safeList(directoryService.findDisabledUsers());

        for (AdUser user : enabled) {
            addDirectoryUser(users, user, plan, result);
        }
        for (AdUser user : disabled) {
            addDirectoryUser(users, user, plan, result);
        }
        plan.setAdLoaded(users.size());
        return users;
    }

    private void addDirectoryUser(Map<String, AdUser> users, AdUser user, SynchronizationPlan plan, SyncResult result) {
        if (user == null) {
            return;
        }
        String key = normalizeUserKey(user.getsAMAccountName());
        if (key == null) {
            SyncIssue issue = new SyncIssue();
            issue.setCode("AD_USER_INVALID");
            issue.setDescription("Se omitió un usuario de AD sin sAMAccountName válido.");
            appendIssue(result, plan, issue);
            plan.incrementSkipped();
            return;
        }
        if (users.containsKey(key)) {
            SyncIssue issue = new SyncIssue();
            issue.setCode("AD_USER_DUPLICATE");
            issue.setDescription("Se encontró un usuario de AD duplicado en la carga.");
            issue.setReferenceId(key);
            appendIssue(result, plan, issue);
            return;
        }
        users.put(key, user);
    }

    private Map<String, MaximoPerson> loadMaximoPeople(SynchronizationPlan plan) {
        Map<String, MaximoPerson> people = new LinkedHashMap<String, MaximoPerson>();
        Set<String> statuses = resolveMaximoStatuses();
        for (String status : statuses) {
            List<MaximoPerson> list = safeList(maximoRepository.findPeopleByStatus(status));
            for (MaximoPerson person : list) {
                if (person == null) {
                    continue;
                }
                String key = normalizeUserKey(person.getPersonId());
                if (key == null) {
                    continue;
                }
                if (!people.containsKey(key)) {
                    people.put(key, person);
                }
            }
        }
        plan.setMaximoLoaded(people.size());
        return people;
    }

    private Set<String> resolveMaximoStatuses() {
        String configured = StringSanitizer.trimToNull(config.getString("sync.maximo.statuses", null));
        LinkedHashSet<String> statuses = new LinkedHashSet<String>();
        if (configured != null) {
            String[] parts = configured.split(",");
            for (String part : parts) {
                String trimmed = StringSanitizer.trimToNull(part);
                if (trimmed != null) {
                    statuses.add(trimmed.toUpperCase(Locale.ROOT));
                }
            }
        }
        if (statuses.isEmpty()) {
            statuses.add("ACTIVO");
            statuses.add("INACTIVO");
        }
        return statuses;
    }

    private void appendIssue(SyncResult result, SynchronizationPlan plan, SyncIssue issue) {
        if (issue == null) {
            return;
        }
        result.addIssue(issue);
        plan.addIssue(issue);
        try {
            auditRepository.saveIssue(result.getRunId(), issue);
        } catch (RuntimeException ignored) {
            // no-op
        }
    }

    private String normalizeUserKey(String value) {
        return StringSanitizer.normalizeUserName(value);
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    public static final class LoadedData {
        private final Map<String, AdUser> adUsers;
        private final Map<String, MaximoPerson> maximoPeople;

        private LoadedData(Map<String, AdUser> adUsers, Map<String, MaximoPerson> maximoPeople) {
            this.adUsers = adUsers;
            this.maximoPeople = maximoPeople;
        }

        public Map<String, AdUser> getAdUsers() {
            return adUsers;
        }

        public Map<String, MaximoPerson> getMaximoPeople() {
            return maximoPeople;
        }
    }

    private static final class NoOpDirectoryService implements DirectoryService {
        @Override
        public List<AdUser> findEnabledUsers() {
            return Collections.emptyList();
        }

        @Override
        public List<AdUser> findDisabledUsers() {
            return Collections.emptyList();
        }

        @Override
        public java.util.Optional<AdUser> findByUserName(String userName) {
            return java.util.Optional.empty();
        }
    }

    private static final class NoOpMaximoRepository implements MaximoRepository {
        @Override
        public List<MaximoPerson> findPeopleByStatus(String status) {
            return Collections.emptyList();
        }

        @Override
        public java.util.Optional<MaximoPerson> findByPersonId(String personId) {
            return java.util.Optional.empty();
        }

        @Override
        public void saveOrUpdate(MaximoPerson person) {
            // no-op
        }

        @Override
        public int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) {
            return 0;
        }
    }

    private static final class NoOpAuditRepository implements AuditRepository {
        @Override
        public void saveExecutionStart(com.syncadmaximo.model.SyncExecution execution) {
            // no-op
        }

        @Override
        public void saveExecutionEnd(com.syncadmaximo.model.SyncExecution execution) {
            // no-op
        }

        @Override
        public void saveIssue(String runId, SyncIssue issue) {
            // no-op
        }

        @Override
        public List<SyncIssue> findIssuesByRunId(String runId) {
            return Collections.emptyList();
        }
    }
}
