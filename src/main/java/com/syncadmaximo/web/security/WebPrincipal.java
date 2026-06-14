package com.syncadmaximo.web.security;

import java.security.Principal;
import java.util.Locale;
import java.util.Objects;

public final class WebPrincipal implements Principal {

    private final String userName;
    private final String displayName;
    private final String role;

    public WebPrincipal(String userName, String displayName, String role) {
        this.userName = normalize(userName);
        this.displayName = displayName == null || displayName.trim().isEmpty() ? this.userName : displayName.trim();
        this.role = role == null || role.trim().isEmpty() ? null : role.trim();
    }

    @Override
    public String getName() {
        return userName;
    }

    public String getUserName() {
        return userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String expectedRole) {
        if (expectedRole == null || role == null) {
            return false;
        }
        return role.equalsIgnoreCase(expectedRole.trim());
    }

    public boolean isUser(String expectedUser) {
        if (expectedUser == null) {
            return false;
        }
        return userName.equalsIgnoreCase(expectedUser.trim());
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "WebPrincipal{" +
                "userName='" + userName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebPrincipal)) {
            return false;
        }
        WebPrincipal that = (WebPrincipal) o;
        return Objects.equals(userName, that.userName)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, displayName, role);
    }
}
