package com.syncadmaximo.repository.oracle;

import com.syncadmaximo.model.MaximoPerson;
import com.syncadmaximo.repository.MaximoRepository;
import com.syncadmaximo.sql.OracleSql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OracleMaximoRepository implements MaximoRepository {
    private final OracleConnectionFactory connectionFactory;

    public OracleMaximoRepository(OracleConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<String> findActivePersonIdByCedula(String cedula) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return findActivePersonIdByCedula(connection, cedula);
        }
    }

    public Optional<String> findActivePersonIdByCedula(Connection connection, String cedula) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.selectPersonByCedula(connectionFactory.getSchema()))) {
            OracleSql.bindNullableString(statement, 1, cedula);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("PERSONID")) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<String> findCedulaByPersonId(String personId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return findCedulaByPersonId(connection, personId);
        }
    }

    public Optional<String> findCedulaByPersonId(Connection connection, String personId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.selectPersonByPersonId(connectionFactory.getSchema()))) {
            OracleSql.bindNullableString(statement, 1, personId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("EPP_CEDULA")) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<String> findPrimaryEmailByPersonId(String personId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return findPrimaryEmailByPersonId(connection, personId);
        }
    }

    public Optional<String> findPrimaryEmailByPersonId(Connection connection, String personId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.selectPrimaryEmailByPersonId(connectionFactory.getSchema()))) {
            OracleSql.bindNullableString(statement, 1, personId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("EMAILADDRESS")) : Optional.empty();
            }
        }
    }

    @Override
    public boolean isEmailAssignedToDifferentPerson(String emailAddress, String personId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return isEmailAssignedToDifferentPerson(connection, emailAddress, personId);
        }
    }

    public boolean isEmailAssignedToDifferentPerson(Connection connection, String emailAddress, String personId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.emailBelongsToOtherPerson(connectionFactory.getSchema()))) {
            OracleSql.bindNullableString(statement, 1, emailAddress);
            OracleSql.bindNullableString(statement, 2, personId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    @Override
    public int updatePersonIdByCedula(String cedula, String currentPersonId, String newPersonId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return updatePersonIdByCedula(connection, cedula, currentPersonId, newPersonId);
        }
    }

    public int updatePersonIdByCedula(Connection connection, String cedula, String currentPersonId, String newPersonId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.updatePersonIdByCedula(connectionFactory.getSchema()))) {
            OracleSql.bindNullableString(statement, 1, newPersonId);
            OracleSql.bindNullableString(statement, 2, cedula);
            OracleSql.bindNullableString(statement, 3, currentPersonId);
            return statement.executeUpdate();
        }
    }

    @Override
    public int updatePrimaryEmail(String personId, String emailAddress) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return updatePrimaryEmail(connection, personId, emailAddress);
        }
    }

    public int updatePrimaryEmail(Connection connection, String personId, String emailAddress) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.updatePrimaryEmail(connectionFactory.getSchema()))) {
            OracleSql.bindNullableString(statement, 1, emailAddress);
            OracleSql.bindNullableString(statement, 2, personId);
            return statement.executeUpdate();
        }
    }

    @Override
    public int insertPrimaryEmail(long emailId, long rowstamp, String personId, String emailAddress, String type, boolean primary) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return insertPrimaryEmail(connection, emailId, rowstamp, personId, emailAddress, type, primary);
        }
    }

    public int insertPrimaryEmail(Connection connection, long emailId, long rowstamp, String personId, String emailAddress, String type, boolean primary) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.insertPrimaryEmail(connectionFactory.getSchema()))) {
            statement.setLong(1, emailId);
            OracleSql.bindNullableString(statement, 2, personId);
            OracleSql.bindNullableString(statement, 3, emailAddress);
            OracleSql.bindNullableString(statement, 4, type);
            statement.setInt(5, primary ? 1 : 0);
            statement.setLong(6, rowstamp);
            return statement.executeUpdate();
        }
    }

    @Override
    public long nextSequenceValue(String sequenceName) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            return nextSequenceValue(connection, sequenceName);
        }
    }

    public long nextSequenceValue(Connection connection, String sequenceName) throws SQLException {
        if (sequenceName == null || sequenceName.trim().isEmpty()) {
            throw new IllegalArgumentException("sequenceName es obligatorio");
        }
        try (PreparedStatement statement = connection.prepareStatement(OracleSql.nextSequenceValueSql(sequenceName));
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("No se pudo obtener NEXTVAL para la secuencia " + sequenceName);
            }
            return resultSet.getLong(1);
        }
    }

    @Override
    public void callStoredProcedure(String procedureName, List<?> parameters) throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            callStoredProcedure(connection, procedureName, parameters);
        }
    }

    public void callStoredProcedure(Connection connection, String procedureName, List<?> parameters) throws SQLException {
        if (procedureName == null || procedureName.trim().isEmpty()) {
            throw new IllegalArgumentException("procedureName es obligatorio");
        }
        List<?> args = parameters == null ? List.of() : parameters;
        StringBuilder sql = new StringBuilder("{ call ").append(procedureName.trim()).append("(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
        }
        sql.append(") }");

        try (CallableStatement statement = connection.prepareCall(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                statement.setObject(i + 1, args.get(i));
            }
            statement.execute();
        }
    }

    @Override
    public List<MaximoPerson> findPeopleByStatus(String status) {
        try (Connection connection = connectionFactory.openConnection()) {
            return findPeopleByStatus(connection, status);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar MAXIMO.PERSON", ex);
        }
    }

    @Override
    public Optional<MaximoPerson> findByPersonId(String personId) {
        try (Connection connection = connectionFactory.openConnection()) {
            return findByPersonId(connection, personId);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar MAXIMO.PERSON por personId", ex);
        }
    }

    @Override
    public void saveOrUpdate(MaximoPerson person) {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                saveOrUpdate(connection, person);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo guardar MAXIMO.PERSON", ex);
        }
    }

    public List<MaximoPerson> findPeopleByStatus(Connection connection, String status) throws SQLException {
        String sql = "SELECT p.PERSONID, p.STATUS, p.FIRSTNAME, p.LASTNAME, p.EPP_CEDULA, p.EPP_NUM_ROL, e.EMAILADDRESS "
                + "FROM " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.PERSON_TABLE) + " p "
                + "LEFT JOIN " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.EMAIL_TABLE) + " e "
                + "ON e.PERSONID = p.PERSONID AND e.ISPRIMARY = 1 "
                + "WHERE p.STATUS = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            OracleSql.bindNullableString(statement, 1, status);
            try (ResultSet rs = statement.executeQuery()) {
                List<MaximoPerson> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapPerson(rs));
                }
                return result;
            }
        }
    }

    public Optional<MaximoPerson> findByPersonId(Connection connection, String personId) throws SQLException {
        String sql = "SELECT p.PERSONID, p.STATUS, p.FIRSTNAME, p.LASTNAME, p.EPP_CEDULA, p.EPP_NUM_ROL, e.EMAILADDRESS "
                + "FROM " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.PERSON_TABLE) + " p "
                + "LEFT JOIN " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.EMAIL_TABLE) + " e "
                + "ON e.PERSONID = p.PERSONID AND e.ISPRIMARY = 1 "
                + "WHERE p.PERSONID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            OracleSql.bindNullableString(statement, 1, personId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapPerson(rs)) : Optional.empty();
            }
        }
    }

    public void saveOrUpdate(Connection connection, MaximoPerson person) throws SQLException {
        if (person == null || person.getPersonId() == null || person.getPersonId().trim().isEmpty()) {
            throw new IllegalArgumentException("person y personId son obligatorios");
        }
        boolean exists = findByPersonId(connection, person.getPersonId()).isPresent();
        if (exists) {
            updatePerson(connection, person);
        } else {
            insertPerson(connection, person);
        }

        if (person.getEmailAddress() != null && !person.getEmailAddress().trim().isEmpty()) {
            if (isEmailAssignedToDifferentPerson(connection, person.getEmailAddress(), person.getPersonId())) {
                throw new SQLException("El correo ya pertenece a otra persona: " + person.getEmailAddress());
            }
            if (findPrimaryEmailByPersonId(connection, person.getPersonId()).isPresent()) {
                updatePrimaryEmail(connection, person.getPersonId(), person.getEmailAddress());
            } else {
                insertConfiguredPrimaryEmail(connection, person.getPersonId(), person.getEmailAddress());
            }
        }
    }

    public void insertPerson(Connection connection, MaximoPerson person) throws SQLException {
        String sql = "INSERT INTO " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.PERSON_TABLE)
                + " (PERSONID, STATUS, FIRSTNAME, LASTNAME, EPP_CEDULA, EPP_NUM_ROL) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            OracleSql.bindNullableString(statement, 1, person.getPersonId());
            OracleSql.bindNullableString(statement, 2, person.getStatus());
            OracleSql.bindNullableString(statement, 3, person.getFirstName());
            OracleSql.bindNullableString(statement, 4, person.getLastName());
            OracleSql.bindNullableString(statement, 5, person.getEppCedula());
            OracleSql.bindNullableString(statement, 6, person.getEppNumRol());
            statement.executeUpdate();
        }
    }

    public void updatePerson(Connection connection, MaximoPerson person) throws SQLException {
        String sql = "UPDATE " + OracleSql.qualifiedTable(connectionFactory.getSchema(), OracleSql.PERSON_TABLE)
                + " SET STATUS = ?, FIRSTNAME = ?, LASTNAME = ?, EPP_CEDULA = ?, EPP_NUM_ROL = ? WHERE PERSONID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            OracleSql.bindNullableString(statement, 1, person.getStatus());
            OracleSql.bindNullableString(statement, 2, person.getFirstName());
            OracleSql.bindNullableString(statement, 3, person.getLastName());
            OracleSql.bindNullableString(statement, 4, person.getEppCedula());
            OracleSql.bindNullableString(statement, 5, person.getEppNumRol());
            OracleSql.bindNullableString(statement, 6, person.getPersonId());
            statement.executeUpdate();
        }
    }

    public void insertConfiguredPrimaryEmail(Connection connection, String personId, String emailAddress) throws SQLException {
        String emailIdSequence = connectionFactory.getEmailIdSequenceName();
        String rowstampSequence = connectionFactory.getRowstampSequenceName();
        if (emailIdSequence == null || rowstampSequence == null) {
            throw new SQLException("Las secuencias para EMAILID y ROWSTAMP deben configurarse para insertar correos");
        }
        long emailId = nextSequenceValue(connection, emailIdSequence);
        long rowstamp = nextSequenceValue(connection, rowstampSequence);
        insertPrimaryEmail(connection, emailId, rowstamp, personId, emailAddress, "TRABAJO", true);
    }

    public MaximoPerson mapPerson(ResultSet rs) throws SQLException {
        MaximoPerson person = new MaximoPerson();
        person.setPersonId(rs.getString("PERSONID"));
        person.setStatus(rs.getString("STATUS"));
        person.setFirstName(rs.getString("FIRSTNAME"));
        person.setLastName(rs.getString("LASTNAME"));
        person.setEppCedula(rs.getString("EPP_CEDULA"));
        person.setEppNumRol(rs.getString("EPP_NUM_ROL"));
        person.setEmailAddress(rs.getString("EMAILADDRESS"));
        return person;
    }
}
