package com.syncadmaximo.ldap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.syncadmaximo.model.AdUser;

/**
 * Convierte respuestas LDAP/AD en objetos de dominio livianos.
 */
public class LdapUserMapper {

    public static final String ATTR_DISTINGUISHED_NAME = "distinguishedName";
    public static final String ATTR_SAM_ACCOUNT_NAME = "sAMAccountName";
    public static final String ATTR_MAIL = "mail";
    public static final String ATTR_POSTAL_CODE = "postalCode";
    public static final String ATTR_DISPLAY_NAME = "displayName";
    public static final String ATTR_GIVEN_NAME = "givenName";
    public static final String ATTR_SURNAME = "sn";
    public static final String ATTR_USER_ACCOUNT_CONTROL = "userAccountControl";

    public LdapUser map(SearchResult searchResult) throws NamingException {
        if (searchResult == null) {
            return null;
        }
        return map(searchResult.getAttributes(), resolveDistinguishedName(searchResult, searchResult.getAttributes()));
    }

    public LdapUser map(Attributes attributes, String distinguishedName) throws NamingException {
        if (attributes == null) {
            return new LdapUser(distinguishedName, null, null, null, null, null, null, false, new LinkedHashMap<String, List<String>>());
        }

        Map<String, List<String>> rawAttributes = toMap(attributes);
        String samAccountName = first(attributes, ATTR_SAM_ACCOUNT_NAME);
        String mail = first(attributes, ATTR_MAIL);
        String postalCode = first(attributes, ATTR_POSTAL_CODE);
        String displayName = first(attributes, ATTR_DISPLAY_NAME);
        String givenName = first(attributes, ATTR_GIVEN_NAME);
        String surname = first(attributes, ATTR_SURNAME);
        boolean enabled = isEnabled(first(attributes, ATTR_USER_ACCOUNT_CONTROL));

        String dn = distinguishedName != null ? distinguishedName : first(attributes, ATTR_DISTINGUISHED_NAME);
        if (dn == null) {
            dn = rawAttributes.containsKey(ATTR_DISTINGUISHED_NAME) ? first(attributes, ATTR_DISTINGUISHED_NAME) : null;
        }

        return new LdapUser(dn, samAccountName, mail, postalCode, displayName, givenName, surname, enabled, rawAttributes);
    }

    public Map<String, List<String>> toMap(Attributes attributes) throws NamingException {
        Map<String, List<String>> values = new LinkedHashMap<String, List<String>>();
        if (attributes == null) {
            return values;
        }

        NamingEnumeration<? extends Attribute> all = attributes.getAll();
        try {
            while (all.hasMore()) {
                Attribute attribute = all.next();
                List<String> collected = new ArrayList<String>();
                NamingEnumeration<?> enumeration = attribute.getAll();
                try {
                    while (enumeration.hasMore()) {
                        Object value = enumeration.next();
                        if (value != null) {
                            collected.add(String.valueOf(value));
                        }
                    }
                } finally {
                    if (enumeration != null) {
                        enumeration.close();
                    }
                }
                values.put(attribute.getID(), collected);
            }
        } finally {
            if (all != null) {
                all.close();
            }
        }
        return values;
    }

    public String first(Attributes attributes, String attributeName) throws NamingException {
        if (attributes == null || attributeName == null) {
            return null;
        }
        Attribute attribute = attributes.get(attributeName);
        if (attribute == null) {
            return null;
        }
        Object value = attribute.get();
        return value == null ? null : String.valueOf(value);
    }

    private String resolveDistinguishedName(SearchResult searchResult, Attributes attributes) throws NamingException {
        if (attributes != null) {
            String dn = first(attributes, ATTR_DISTINGUISHED_NAME);
            if (dn != null && dn.length() > 0) {
                return dn;
            }
        }
        String nameInNamespace = null;
        try {
            nameInNamespace = searchResult.getNameInNamespace();
        } catch (UnsupportedOperationException ex) {
            // Ignorado: algunos providers LDAP no exponen este dato.
        }
        if (nameInNamespace != null && nameInNamespace.length() > 0) {
            return nameInNamespace;
        }
        return searchResult.getName();
    }

    public AdUser toAdUser(LdapUser user) {
        if (user == null) {
            return null;
        }
        AdUser adUser = new AdUser();
        adUser.setsAMAccountName(user.getSamAccountName());
        adUser.setPostalCode(user.getPostalCode());
        adUser.setMail(user.getMail());
        adUser.setGivenName(user.getGivenName());
        adUser.setSn(user.getSurname());
        adUser.setDisplayName(user.getDisplayName());
        adUser.setEnabled(user.isEnabled());
        return adUser;
    }

    public AdUser mapToAdUser(SearchResult searchResult) throws NamingException {
        return toAdUser(map(searchResult));
    }

    private boolean isEnabled(String userAccountControlValue) {
        if (userAccountControlValue == null || userAccountControlValue.trim().isEmpty()) {
            return true;
        }
        try {
            int flags = Integer.parseInt(userAccountControlValue.trim());
            return (flags & 0x2) == 0;
        } catch (NumberFormatException ex) {
            return true;
        }
    }
}
