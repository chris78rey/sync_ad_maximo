package com.syncadmaximo.service;

import com.syncadmaximo.model.AdUser;
import com.syncadmaximo.model.MaximoPerson;

import java.util.Optional;

public interface ValidationService {

    boolean isValidCedula(String cedula);

    boolean isValidEmail(String email);

    boolean isValidUserName(String userName);

    Optional<String> normalizeCedula(String cedula);

    boolean matchesByCedula(AdUser adUser, MaximoPerson maximoPerson);
}
