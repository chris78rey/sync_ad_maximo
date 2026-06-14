package com.syncadmaximo.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.syncadmaximo.ldap.LdapUser;
import com.syncadmaximo.model.AdUser;

/**
 * Validador principal de usuarios provenientes de Active Directory.
 * Aplica normalización, validaciones básicas y detección de duplicados/conflictos.
 */
public class SyncValidator {

    private static final String FIELD_CEDULA = "cedula";
    private static final String FIELD_USUARIO = "usuario";
    private static final String FIELD_CORREO = "correo";

    private final CedulaNormalizer cedulaNormalizer;
    private final UserNormalizer userNormalizer;
    private final EmailNormalizer emailNormalizer;

    public SyncValidator() {
        this(new CedulaNormalizer(), new UserNormalizer(), new EmailNormalizer());
    }

    public SyncValidator(CedulaNormalizer cedulaNormalizer,
                         UserNormalizer userNormalizer,
                         EmailNormalizer emailNormalizer) {
        this.cedulaNormalizer = cedulaNormalizer == null ? new CedulaNormalizer() : cedulaNormalizer;
        this.userNormalizer = userNormalizer == null ? new UserNormalizer() : userNormalizer;
        this.emailNormalizer = emailNormalizer == null ? new EmailNormalizer() : emailNormalizer;
    }

    public ValidationReport validate(LdapUser user) {
        ValidationReport report = new ValidationReport(user);
        if (user == null) {
            report.blockIdentity(FIELD_CEDULA, "USER_NULL", "El usuario LDAP es nulo.", null, null);
            return report;
        }

        validateCedula(user, report);
        validateUsuario(user, report);
        validateCorreo(user, report);
        return report;
    }

    public ValidationReport validate(AdUser user) {
        return validate(toLdapUser(user));
    }

    public List<ValidationReport> validate(Collection<LdapUser> users) {
        List<ValidationReport> reports = new ArrayList<ValidationReport>();
        if (users == null || users.isEmpty()) {
            return reports;
        }

        for (LdapUser user : users) {
            reports.add(validate(user));
        }

        detectBatchDuplicatesAndConflicts(reports);
        return reports;
    }

    public List<ValidationReport> validateAdUsers(Collection<AdUser> users) {
        List<ValidationReport> reports = new ArrayList<ValidationReport>();
        if (users == null || users.isEmpty()) {
            return reports;
        }
        for (AdUser user : users) {
            reports.add(validate(user));
        }
        return reports;
    }

    private void validateCedula(LdapUser user, ValidationReport report) {
        String rawCedula = user.getPostalCode();
        String digitsOnly = digitsOnly(rawCedula);
        String normalizedCedula = cedulaNormalizer.normalize(rawCedula);

        report.setNormalizedCedula(normalizedCedula);
        report.setCedulaStrategyApplied(cedulaNormalizer.getNineDigitStrategy().name());

        if (rawCedula == null || rawCedula.trim().isEmpty()) {
            report.blockIdentity(FIELD_CEDULA,
                    "CEDULA_VACIA",
                    "La cédula está vacía y no puede procesarse.",
                    rawCedula,
                    normalizedCedula);
            return;
        }

        if (normalizedCedula == null || normalizedCedula.isEmpty()) {
            report.blockIdentity(FIELD_CEDULA,
                    "CEDULA_SIN_DIGITOS",
                    "La cédula no contiene dígitos válidos.",
                    rawCedula,
                    normalizedCedula);
            return;
        }

        if (digitsOnly.length() == 9) {
            if (cedulaNormalizer.getNineDigitStrategy() == CedulaNineDigitStrategy.AGREGAR_CERO) {
                report.addInfo(FIELD_CEDULA,
                        "CEDULA_9_DIGITOS_NORMALIZADA",
                        "La cédula tenía 9 dígitos y se normalizó agregando un cero inicial.",
                        rawCedula,
                        normalizedCedula);
            } else {
                report.blockIdentity(FIELD_CEDULA,
                        "CEDULA_9_DIGITOS_REQUIERE_REVISION",
                        "La cédula tiene 9 dígitos y requiere revisión manual según la estrategia configurada.",
                        rawCedula,
                        normalizedCedula);
            }
            return;
        }

        if (normalizedCedula.length() < 10 || normalizedCedula.length() > 10) {
            report.blockIdentity(FIELD_CEDULA,
                    "CEDULA_LONGITUD_INVALIDA",
                    "La cédula debe tener 10 dígitos después de la normalización.",
                    rawCedula,
                    normalizedCedula);
        }
    }

    private void validateUsuario(LdapUser user, ValidationReport report) {
        String rawUser = user.getSamAccountName();
        String normalizedUser = userNormalizer.normalize(rawUser);
        report.setNormalizedUser(normalizedUser);

        if (rawUser == null || rawUser.trim().isEmpty() || normalizedUser == null || normalizedUser.isEmpty()) {
            report.blockIdentity(FIELD_USUARIO,
                    "USUARIO_VACIO",
                    "El usuario sAMAccountName está vacío y no puede procesarse.",
                    rawUser,
                    normalizedUser);
        }
    }

    private void validateCorreo(LdapUser user, ValidationReport report) {
        String rawEmail = user.getMail();
        String normalizedEmail = emailNormalizer.normalize(rawEmail);
        report.setNormalizedEmail(normalizedEmail);

        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            report.markEmailUnavailable(FIELD_CORREO,
                    "CORREO_VACIO",
                    "No se sincronizará correo porque AD.mail está vacío.",
                    rawEmail,
                    normalizedEmail);
            return;
        }

        if (!emailNormalizer.isBasicFormatValid(rawEmail)) {
            report.blockEmail(FIELD_CORREO,
                    "CORREO_INVALIDO",
                    "El correo no cumple un formato básico válido y no debe sincronizarse.",
                    rawEmail,
                    normalizedEmail);
        }
    }

    private void detectBatchDuplicatesAndConflicts(List<ValidationReport> reports) {
        Map<String, List<ValidationReport>> cedulaIndex = new LinkedHashMap<String, List<ValidationReport>>();
        Map<String, List<ValidationReport>> userIndex = new LinkedHashMap<String, List<ValidationReport>>();
        Map<String, List<ValidationReport>> emailIndex = new LinkedHashMap<String, List<ValidationReport>>();

        for (ValidationReport report : reports) {
            index(cedulaIndex, report.getNormalizedCedula(), report);
            index(userIndex, report.getNormalizedUser(), report);
            index(emailIndex, report.getNormalizedEmail(), report);
        }

        for (Map.Entry<String, List<ValidationReport>> entry : cedulaIndex.entrySet()) {
            List<ValidationReport> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            if (sameNormalizedUser(group)) {
                markDuplicateGroup(group, FIELD_CEDULA, "CEDULA_DUPLICADA",
                        "La misma cédula aparece repetida en más de un registro.");
            } else {
                markConflictGroup(group, FIELD_CEDULA, "CEDULA_EN_CONFLICTO",
                        "La cédula está asociada a múltiples identidades distintas.");
            }
        }

        for (Map.Entry<String, List<ValidationReport>> entry : userIndex.entrySet()) {
            List<ValidationReport> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            if (sameNormalizedCedula(group)) {
                markDuplicateGroup(group, FIELD_USUARIO, "USUARIO_DUPLICADO",
                        "El mismo sAMAccountName aparece repetido en más de un registro.");
            } else {
                markConflictGroup(group, FIELD_USUARIO, "USUARIO_EN_CONFLICTO",
                        "El sAMAccountName está asociado a múltiples cédulas distintas.");
            }
        }

        for (Map.Entry<String, List<ValidationReport>> entry : emailIndex.entrySet()) {
            List<ValidationReport> group = entry.getValue();
            if (group.size() < 2) {
                continue;
            }
            if (sameNormalizedCedula(group) && sameNormalizedUser(group)) {
                markDuplicateGroup(group, FIELD_CORREO, "CORREO_DUPLICADO",
                        "El mismo correo aparece repetido en más de un registro.");
            } else {
                markEmailConflictGroup(group);
            }
        }
    }

    private void markDuplicateGroup(List<ValidationReport> group, String field, String code, String message) {
        for (ValidationReport report : group) {
            report.markDuplicate(field, code, message, originalValue(report, field), normalizedValue(report, field));
        }
    }

    private void markConflictGroup(List<ValidationReport> group, String field, String code, String message) {
        for (ValidationReport report : group) {
            report.markConflict(field, code, message, originalValue(report, field), normalizedValue(report, field));
        }
    }

    private void markEmailConflictGroup(List<ValidationReport> group) {
        for (ValidationReport report : group) {
            report.blockEmail(FIELD_CORREO,
                    "CORREO_EN_CONFLICTO",
                    "El correo está asociado a múltiples identidades y no debe sincronizarse.",
                    originalValue(report, FIELD_CORREO),
                    normalizedValue(report, FIELD_CORREO));
        }
    }

    private boolean sameNormalizedCedula(List<ValidationReport> group) {
        if (group == null || group.size() < 2) {
            return true;
        }
        String first = group.get(0).getNormalizedCedula();
        for (int i = 1; i < group.size(); i++) {
            if (!equalsIgnoreCase(first, group.get(i).getNormalizedCedula())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameNormalizedUser(List<ValidationReport> group) {
        if (group == null || group.size() < 2) {
            return true;
        }
        String first = group.get(0).getNormalizedUser();
        for (int i = 1; i < group.size(); i++) {
            if (!equalsIgnoreCase(first, group.get(i).getNormalizedUser())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameNormalizedIdentity(List<ValidationReport> group) {
        return sameNormalizedCedula(group) && sameNormalizedUser(group);
    }

    private void index(Map<String, List<ValidationReport>> index, String key, ValidationReport report) {
        if (key == null || key.trim().isEmpty() || report == null) {
            return;
        }
        List<ValidationReport> group = index.get(key);
        if (group == null) {
            group = new ArrayList<ValidationReport>();
            index.put(key, group);
        }
        group.add(report);
    }

    private LdapUser toLdapUser(AdUser user) {
        if (user == null) {
            return null;
        }
        return new LdapUser(
                null,
                user.getsAMAccountName(),
                user.getMail(),
                user.getPostalCode(),
                user.getDisplayName(),
                user.getGivenName(),
                user.getSn(),
                user.isEnabled(),
                java.util.Collections.<String, java.util.List<String>>emptyMap());
    }

    private String originalValue(ValidationReport report, String field) {
        if (report == null || report.getSourceUser() == null) {
            return null;
        }
        if (FIELD_CEDULA.equals(field)) {
            return report.getSourceUser().getPostalCode();
        }
        if (FIELD_USUARIO.equals(field)) {
            return report.getSourceUser().getSamAccountName();
        }
        if (FIELD_CORREO.equals(field)) {
            return report.getSourceUser().getMail();
        }
        return null;
    }

    private String normalizedValue(ValidationReport report, String field) {
        if (report == null) {
            return null;
        }
        if (FIELD_CEDULA.equals(field)) {
            return report.getNormalizedCedula();
        }
        if (FIELD_USUARIO.equals(field)) {
            return report.getNormalizedUser();
        }
        if (FIELD_CORREO.equals(field)) {
            return report.getNormalizedEmail();
        }
        return null;
    }

    private String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
