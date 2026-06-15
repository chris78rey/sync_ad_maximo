package com.syncadmaximo.service;

import com.syncadmaximo.model.MaximoPerson;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MaximoRepository {

    List<MaximoPerson> findPeopleByStatus(String status);

    Optional<MaximoPerson> findByPersonId(String personId);

    void saveOrUpdate(MaximoPerson person);

    int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) throws SQLException;
}
