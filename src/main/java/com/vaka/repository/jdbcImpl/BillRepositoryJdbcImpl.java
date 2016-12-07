package com.vaka.repository.jdbcImpl;

import com.vaka.context.ApplicationContext;
import com.vaka.domain.Bill;
import com.vaka.repository.BillRepository;
import com.vaka.util.repository.CrudRepositoryUtil;
import com.vaka.util.DomainExtractor;
import com.vaka.util.repository.NamedPreparedStatement;
import com.vaka.util.repository.StatementExtractor;
import com.vaka.util.exception.RepositoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Iaroslav on 12/3/2016.
 */
public class BillRepositoryJdbcImpl implements BillRepository {
    private DataSource dataSource;
    private Map<String, String> queryByClassAndMethodName;

    @Override
    public Bill create(Bill entity) {
        String strQuery = getQueryByClassAndMethodName().get("bill.create");
        try (Connection connection = getDataSource().getConnection();//TODO move to JdbcUtil
             NamedPreparedStatement statement = createAndExecuteCreateStatement(connection, strQuery, entity, Statement.RETURN_GENERATED_KEYS);
             ResultSet resultSet = statement.getGenerationKeys()) {
            if (resultSet.next()) {
                entity.setId(resultSet.getInt(1));
                return entity;
            } else throw new SQLException("ID wasn't returned");
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }
    private NamedPreparedStatement createAndExecuteCreateStatement(Connection connection, String strQuery, Bill entity, int statementCode) throws SQLException {
        NamedPreparedStatement statement = new NamedPreparedStatement(connection, strQuery, statementCode).init();
        StatementExtractor.extract(entity, statement);
        statement.execute();
        return statement;
    }
    @Override
    public Optional<Bill> getByReservationId(Integer id) {
        try {
            Connection connection = getDataSource().getConnection();
            String strQuery = getQueryByClassAndMethodName().get("bill.getByReservationId");
            NamedPreparedStatement statement = new NamedPreparedStatement(connection, strQuery).init();
            statement.setStatement("reservationId", id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next())
                return Optional.of(DomainExtractor.extractBill(resultSet));
            else return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public Optional<Bill> getById(Integer id) {
        String strQuery = getQueryByClassAndMethodName().get("bill.getById");
        try (Connection connection = getDataSource().getConnection();
             NamedPreparedStatement statement = CrudRepositoryUtil.createGetByIdStatement(connection, strQuery, id);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next())
                return Optional.of(DomainExtractor.extractBill(resultSet));
            else return Optional.empty();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public boolean delete(Integer id) {
        String strQuery = getQueryByClassAndMethodName().get("bill.delete");
        return CrudRepositoryUtil.delete(strQuery, id);
    }

    @Override
    public boolean update(Integer id, Bill entity) {
        entity.setId(id);
        String strQuery = getQueryByClassAndMethodName().get("bill.update");
        try (Connection connection = getDataSource().getConnection();
             NamedPreparedStatement statement = createUpdateStatement(connection, strQuery, entity)) {
            return statement.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    private NamedPreparedStatement createUpdateStatement(Connection connection, String strQuery, Bill entity) throws SQLException {
        NamedPreparedStatement statement = new NamedPreparedStatement(connection, strQuery).init();
        StatementExtractor.extract(entity, statement);
        return statement;
    }

    public DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    dataSource = ApplicationContext.getInstance().getBean(DataSource.class);
                }
            }
        }
        return dataSource;
    }
    public Map<String, String> getQueryByClassAndMethodName() {
        if (queryByClassAndMethodName == null) {
            synchronized (this) {
                if (queryByClassAndMethodName == null) {
                    queryByClassAndMethodName = ApplicationContext.getInstance().getQueryByClassAndMethodName();
                }
            }
        }
        return queryByClassAndMethodName;
    }
}
