package com.syncadmaximo.ldap;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Cliente LDAP/Active Directory basado en JNDI.
 * No depende de Jakarta y puede ser usado desde cualquier capa superior.
 */
public class LdapClient {

    public static final String DEFAULT_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String DEFAULT_OBJECT_CLASS_FILTER = "(objectClass=user)";
    public static final String DEFAULT_ENABLED_FILTER = "(&(objectClass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))";
    public static final String DEFAULT_DISABLED_FILTER = "(&(objectClass=user)(userAccountControl:1.2.840.113556.1.4.803:=2))";

    private final String providerUrl;
    private final String bindDn;
    private final String bindPassword;
    private final String baseDn;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String contextFactory;
    private final String objectClassFilter;
    private final String enabledFilter;
    private final String disabledFilter;
    private final LdapUserMapper mapper;

    public LdapClient(String providerUrl,
                      String bindDn,
                      String bindPassword,
                      String baseDn) {
        this(providerUrl, bindDn, bindPassword, baseDn, 5000, 5000, DEFAULT_CONTEXT_FACTORY,
                DEFAULT_OBJECT_CLASS_FILTER, DEFAULT_ENABLED_FILTER, DEFAULT_DISABLED_FILTER, new LdapUserMapper());
    }

    public LdapClient(String providerUrl,
                      String bindDn,
                      String bindPassword,
                      String baseDn,
                      int connectTimeoutMillis,
                      int readTimeoutMillis,
                      String contextFactory,
                      String objectClassFilter,
                      String enabledFilter,
                      String disabledFilter,
                      LdapUserMapper mapper) {
        this.providerUrl = providerUrl;
        this.bindDn = bindDn;
        this.bindPassword = bindPassword;
        this.baseDn = baseDn;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.contextFactory = contextFactory == null ? DEFAULT_CONTEXT_FACTORY : contextFactory;
        this.objectClassFilter = objectClassFilter == null ? DEFAULT_OBJECT_CLASS_FILTER : objectClassFilter;
        this.enabledFilter = enabledFilter == null ? DEFAULT_ENABLED_FILTER : enabledFilter;
        this.disabledFilter = disabledFilter == null ? DEFAULT_DISABLED_FILTER : disabledFilter;
        this.mapper = mapper == null ? new LdapUserMapper() : mapper;
    }

    public List<LdapUser> findAllUsers() {
        return findUsers(objectClassFilter);
    }

    public List<LdapUser> findEnabledUsers() {
        return findUsers(enabledFilter);
    }

    public List<LdapUser> findDisabledUsers() {
        return findUsers(disabledFilter);
    }

    public Optional<LdapUser> findBySamAccountName(String samAccountName) {
        if (samAccountName == null || samAccountName.trim().isEmpty()) {
            return Optional.empty();
        }
        String escaped = escapeLdapFilterValue(samAccountName.trim());
        String filter = "(&(objectClass=user)(sAMAccountName=" + escaped + "))";
        List<LdapUser> users = findUsers(filter);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public List<LdapUser> findUsers(String filter) {
        DirContext context = null;
        try {
            context = createContext();
            return search(context, filter, buildAttributes());
        } catch (NamingException ex) {
            throw new LdapClientException("Error consultando LDAP: " + ex.getMessage(), ex);
        } finally {
            closeQuietly(context);
        }
    }

    protected List<LdapUser> search(DirContext context, String filter, String[] attributes) throws NamingException {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(attributes);

        NamingEnumeration<SearchResult> results = context.search(baseDn, filter, controls);
        List<LdapUser> users = new ArrayList<LdapUser>();
        try {
            while (results.hasMore()) {
                SearchResult result = results.next();
                LdapUser user = mapper.map(result);
                if (user != null) {
                    users.add(user);
                }
            }
        } finally {
            if (results != null) {
                results.close();
            }
        }
        return users;
    }

    protected DirContext createContext() throws NamingException {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(connectTimeoutMillis));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(readTimeoutMillis));

        if (bindDn != null && bindDn.trim().length() > 0) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            if (bindPassword != null) {
                env.put(Context.SECURITY_CREDENTIALS, bindPassword);
            }
        }

        return new InitialDirContext(env);
    }

    protected String[] buildAttributes() {
        return new String[] {
                LdapUserMapper.ATTR_DISTINGUISHED_NAME,
                LdapUserMapper.ATTR_SAM_ACCOUNT_NAME,
                LdapUserMapper.ATTR_MAIL,
                LdapUserMapper.ATTR_POSTAL_CODE,
                LdapUserMapper.ATTR_DISPLAY_NAME,
                LdapUserMapper.ATTR_GIVEN_NAME,
                LdapUserMapper.ATTR_SURNAME,
                LdapUserMapper.ATTR_USER_ACCOUNT_CONTROL,
                "memberOf"
        };
    }

    protected void closeQuietly(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception ex) {
                // Ignorado a propósito.
            }
        }
    }

    protected String escapeLdapFilterValue(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\5c");
                    break;
                case '*':
                    escaped.append("\\2a");
                    break;
                case '(': 
                    escaped.append("\\28");
                    break;
                case ')':
                    escaped.append("\\29");
                    break;
                case '\u0000':
                    escaped.append("\\00");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }

    public static class LdapClientException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public LdapClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
