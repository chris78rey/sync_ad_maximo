package com.syncadmaximo.model;

import java.util.Objects;

public class AdUser {

    private String sAMAccountName;
    private String postalCode;
    private String mail;
    private String givenName;
    private String sn;
    private String displayName;
    private boolean enabled;

    public AdUser() {
    }

    public AdUser(String sAMAccountName, String postalCode, String mail) {
        this.sAMAccountName = sAMAccountName;
        this.postalCode = postalCode;
        this.mail = mail;
    }

    public String getsAMAccountName() {
        return sAMAccountName;
    }

    public void setsAMAccountName(String sAMAccountName) {
        this.sAMAccountName = sAMAccountName;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNormalizedUserName() {
        return com.syncadmaximo.util.StringSanitizer.normalizeUserName(sAMAccountName);
    }

    public String getNormalizedEmail() {
        return com.syncadmaximo.util.StringSanitizer.normalizeEmail(mail);
    }

    @Override
    public String toString() {
        return "AdUser{" +
                "sAMAccountName='" + sAMAccountName + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", mail='" + mail + '\'' +
                ", enabled=" + enabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdUser)) {
            return false;
        }
        AdUser adUser = (AdUser) o;
        return Objects.equals(getNormalizedUserName(), adUser.getNormalizedUserName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNormalizedUserName());
    }
}
