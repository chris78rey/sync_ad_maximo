package com.syncadmaximo.model;

import java.util.Objects;

public class MaximoPerson {

    private String personId;
    private String status;
    private String firstName;
    private String lastName;
    private String eppCedula;
    private String eppNumRol;
    private String emailAddress;

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEppCedula() {
        return eppCedula;
    }

    public void setEppCedula(String eppCedula) {
        this.eppCedula = eppCedula;
    }

    public String getEppNumRol() {
        return eppNumRol;
    }

    public void setEppNumRol(String eppNumRol) {
        this.eppNumRol = eppNumRol;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean isActive() {
        return "ACTIVO".equalsIgnoreCase(status);
    }

    public String getNormalizedPersonId() {
        return com.syncadmaximo.util.StringSanitizer.normalizeUserName(personId);
    }

    @Override
    public String toString() {
        return "MaximoPerson{" +
                "personId='" + personId + '\'' +
                ", status='" + status + '\'' +
                ", eppCedula='" + eppCedula + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MaximoPerson)) {
            return false;
        }
        MaximoPerson that = (MaximoPerson) o;
        return Objects.equals(getNormalizedPersonId(), that.getNormalizedPersonId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNormalizedPersonId());
    }
}
