package com.vaka.hotel_manager.repository.jdbcImpl;

import com.vaka.hotel_manager.core.context.ApplicationContext;
import com.vaka.hotel_manager.core.tx.ConnectionManager;
import com.vaka.hotel_manager.domain.User;
import com.vaka.hotel_manager.repository.UserRepository;
import com.vaka.hotel_manager.repository.util.DomainToStatementExtractor;
import com.vaka.hotel_manager.repository.util.JdbcCrudHelper;
import com.vaka.hotel_manager.repository.util.NamedPreparedStatement;
import com.vaka.hotel_manager.repository.util.StatementToDomainExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Iaroslav on 12/3/2016.
 */
public class UserRepositoryJdbcImpl implements UserRepository {
    private static final Logger LOG = LoggerFactory.getLogger(UserRepositoryJdbcImpl.class);
    private Map<String, String> queryByClassAndMethodName;
    private ConnectionManager connectionManager;
    private JdbcCrudHelper crudHelper;

    @Override
    public Optional<User> getByVkId(String vkId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> getByEmail(String email) {
        String strQuery = getQueryByClassAndMethodName().get("user.getByEmail");
        LOG.info(String.format("SQL query: %s", strQuery));
        return getConnectionManager().withConnection(connection -> {
            try (NamedPreparedStatement statement = createGetByEmailStatement(connection, strQuery, email);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return Optional.of(StatementToDomainExtractor.extractUser(resultSet));
                else return Optional.empty();
            }
        });
    }

    private NamedPreparedStatement createGetByEmailStatement(Connection connection, String strQuery, String email) throws SQLException {
        NamedPreparedStatement statement = NamedPreparedStatement.create(connection, strQuery);
        statement.setStatement("email", email);
        return statement;
    }

    @Override
    public User create(User entity) {
        String strQuery = getQueryByClassAndMethodName().get("user.create");
        LOG.info(String.format("SQL query: %s", strQuery));
        return getCrudHelper().create(
                DomainToStatementExtractor::extract,
                strQuery, entity);
    }

    @Override
    public Optional<User> getById(Integer id) {
        String strQuery = getQueryByClassAndMethodName().get("user.getById");
        LOG.info(String.format("SQL query: %s", strQuery));
        return getCrudHelper().getById(StatementToDomainExtractor::extractUser, strQuery, id);
    }


    @Override
    public boolean delete(Integer id) {
        String strQuery = getQueryByClassAndMethodName().get("user.delete");
        LOG.info(String.format("SQL query: %s", strQuery));
        return getCrudHelper().delete(strQuery, id);
    }


    @Override
    public boolean update(Integer id, User entity) {
        String strQuery = getQueryByClassAndMethodName().get("user.update");
        LOG.info(String.format("SQL query: %s", strQuery));
        return getCrudHelper().update(DomainToStatementExtractor::extract, strQuery, entity, id);
    }


    public ConnectionManager getConnectionManager() {
        if (connectionManager == null) {
            synchronized (this) {
                if (connectionManager == null) {
                    connectionManager = ApplicationContext.getInstance().getBean(ConnectionManager.class);
                }
            }
        }
        return connectionManager;
    }

    public JdbcCrudHelper getCrudHelper() {
        if (crudHelper == null) {
            synchronized (this) {
                if (crudHelper == null) {
                    crudHelper = ApplicationContext.getInstance().getBean(JdbcCrudHelper.class);
                }
            }
        }
        return crudHelper;
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
