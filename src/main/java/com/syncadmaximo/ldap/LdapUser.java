package com.syncadmaximo.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa un usuario leído desde Active Directory.
 * Mantiene el valor original de los atributos LDAP y una vista inmutable de sus atributos crudos.
 */
public final class LdapUser {

    private final String distinguishedName;
    private final String samAccountName;
    private final String mail;
    private final String postalCode;
    private final String displayName;
    private final String givenName;
    private final String surname;
    private final boolean enabled;
    private final Map<String, List<String>> attributes;

    public LdapUser(String distinguishedName,
                    String samAccountName,
                    String mail,
                    String postalCode,
                    String displayName,
                    String givenName,
                    String surname,
                    boolean enabled,
                    Map<String, List<String>> attributes) {
        this.distinguishedName = distinguishedName;
        this.samAccountName = samAccountName;
        this.mail = mail;
        this.postalCode = postalCode;
        this.displayName = displayName;
        this.givenName = givenName;
        this.surname = surname;
        this.enabled = enabled;
        this.attributes = wrap(attributes);
    }

    private static Map<String, List<String>> wrap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        if (source != null) {
            for (Map.Entry<String, List<String>> entry : source.entrySet()) {
                List<String> values = entry.getValue() == null
                        ? Collections.<String>emptyList()
                        : new ArrayList<String>(entry.getValue());
                copy.put(entry.getKey(), Collections.unmodifiableList(values));
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public String getSamAccountName() {
        return samAccountName;
    }

    public String getMail() {
        return mail;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getSurname() {
        return surname;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public String getFirstAttribute(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = attributes.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public String toString() {
        return "LdapUser{" +
                "distinguishedName='" + distinguishedName + '\'' +
                ", samAccountName='" + samAccountName + '\'' +
                ", mail='" + mail + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", displayName='" + displayName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
