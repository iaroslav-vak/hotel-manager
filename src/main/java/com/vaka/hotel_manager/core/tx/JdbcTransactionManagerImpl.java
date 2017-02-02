package com.vaka.hotel_manager.core.tx;

import com.vaka.hotel_manager.core.context.ApplicationContext;
import com.vaka.hotel_manager.repository.exception.ConstraintViolationException;
import com.vaka.hotel_manager.repository.util.SQLFunction;
import com.vaka.hotel_manager.util.exception.RepositoryException;
import com.vaka.hotel_manager.util.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * Created by Iaroslav on 12/30/2016.
 */
public class JdbcTransactionManagerImpl implements TransactionManager, ConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcTransactionManagerImpl.class);
    private static final ThreadLocal<Connection> CONNECTION = new ThreadLocal<>();
    private static final ThreadLocal<TransactionStatus> STATUS = new ThreadLocal<TransactionStatus>() {
        @Override
        protected TransactionStatus initialValue() {
            return TransactionStatus.NOT_ACTIVE;
        }
    };
    private final int isolationDefault;
    private DataSource dataSource;

    public JdbcTransactionManagerImpl(int isolationDefault) {
        this.isolationDefault = isolationDefault;
    }

    @Override
    public void begin(int isolationLevel) throws TransactionException {
        if (STATUS.get() != TransactionStatus.NOT_ACTIVE) {
            if (isRollBackOnly()) {
                //no-op if transaction is in rollbackOnly state that signs about inner transactions
                return;
            } else throw new TransactionException("Transaction already started");
        }
        try {
            Connection connection = getDataSource().getConnection();
            connection.setTransactionIsolation(isolationLevel);
            connection.setAutoCommit(false);
            CONNECTION.set(connection);
            STATUS.set(TransactionStatus.ACTIVE);
        } catch (SQLException e) {
            throw new TransactionException(e);
        }
    }

    @Override
    public void commit() throws TransactionException {
        if (CONNECTION.get() == null)
            throw new TransactionException("There are no transaction to commit");
        if (isRollBackOnly())
            //no-op if transaction is in rollbackOnly state
            return;
        if (STATUS.get() != TransactionStatus.ACTIVE)
            throw new TransactionException(String.format("Transaction is in illegal state: %s", STATUS.get()));
        try (Connection connection = CONNECTION.get()) {
            connection.commit();
        } catch (SQLException e) {
            throw new TransactionException(e);
        } finally {
            STATUS.set(TransactionStatus.NOT_ACTIVE);
            CONNECTION.remove();
        }
    }

    @Override
    public void rollback() throws TransactionException {
        if (CONNECTION.get() == null)
            throw new TransactionException("There are no transaction to rollback");
        if (STATUS.get() == TransactionStatus.ACTIVE || STATUS.get() == TransactionStatus.ACTIVE_ROLLBACK_ONLY) {
            try (Connection connection = CONNECTION.get()) {
                connection.rollback();
            } catch (SQLException e) {
                throw new TransactionException(e);
            } finally {
                STATUS.set(TransactionStatus.NOT_ACTIVE);
                CONNECTION.remove();
            }
        } else
            throw new TransactionException(String.format("Transaction is in illegal state: %s", STATUS.get()));
    }

    @Override
    public void setRollBackOnly(boolean rollbackOnly) {
        if (rollbackOnly == isRollBackOnly())
            return;
        if (rollbackOnly) {
            if (STATUS.get() == TransactionStatus.ACTIVE) {
                STATUS.set(TransactionStatus.ACTIVE_ROLLBACK_ONLY);
            } else throw new TransactionException(String.format(
                    "You can't set status to ACTIVE_ROLLBACK_ONLY if current isn't ACTIVE or ACTIVE_ROLLBACK_ONLY, current - %s", STATUS.get()));
        } else {
            if (STATUS.get() == TransactionStatus.ACTIVE_ROLLBACK_ONLY) {
                STATUS.set(TransactionStatus.ACTIVE);
            } else throw new TransactionException(String.format(
                    "You can't set status to ACTIVE if current isn't ACTIVE_ROLLBACK_ONLY or ACTIVE, current - %s", STATUS.get()));
        }
    }

    @Override
    public boolean isRollBackOnly() {
        return STATUS.get() == TransactionStatus.ACTIVE_ROLLBACK_ONLY;
    }

    @Override
    public TransactionStatus getStatus() {
        return STATUS.get();
    }

    @Override
    public Integer getIsolationDefault() {
        return isolationDefault;
    }

    @Override
    public <T> T withConnection(SQLFunction<Connection, T> withCon) {
        try {
            if (STATUS.get() == TransactionStatus.NOT_ACTIVE){
                return withOutTransaction(withCon);
            } else return withCon.apply(CONNECTION.get());
        } catch (SQLException e) {
            LOG.info(e.getMessage(), e);
            if (e instanceof SQLIntegrityConstraintViolationException){
                throw new ConstraintViolationException(e);
            }
            throw new RepositoryException("Internal server error");
        }
    }

    private <T> T withOutTransaction(SQLFunction<Connection, T> withCon) throws SQLException {
        try (Connection connection = getDataSource().getConnection()){
            return withCon.apply(connection);
        }
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
}
