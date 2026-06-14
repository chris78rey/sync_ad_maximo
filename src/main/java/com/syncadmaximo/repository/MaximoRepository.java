package com.syncadmaximo.repository;

import com.syncadmaximo.model.MaximoPerson;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MaximoRepository extends com.syncadmaximo.service.MaximoRepository {
    Optional<String> findActivePersonIdByCedula(String cedula) throws SQLException;

    Optional<String> findCedulaByPersonId(String personId) throws SQLException;

    Optional<String> findPrimaryEmailByPersonId(String personId) throws SQLException;

    boolean isEmailAssignedToDifferentPerson(String emailAddress, String personId) throws SQLException;

    int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) throws SQLException;

    int updatePrimaryEmail(String personId, String emailAddress) throws SQLException;

    int insertPrimaryEmail(long emailId, long rowstamp, String personId, String emailAddress, String type, boolean primary) throws SQLException;

    long nextSequenceValue(String sequenceName) throws SQLException;

    void callStoredProcedure(String procedureName, List<?> parameters) throws SQLException;

    @Override
    List<MaximoPerson> findPeopleByStatus(String status);

    @Override
    Optional<MaximoPerson> findByPersonId(String personId);

    @Override
    void saveOrUpdate(MaximoPerson person);
}
