package com.rrjaggery.auth.config;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class BootstrapAdminLock {
    private static final int LOCK_NAMESPACE = 0x52524A;
    private static final int LOCK_ID = 1;
    private static final ReentrantLock NON_POSTGRES_TEST_LOCK = new ReentrantLock();

    private final JdbcTemplate jdbcTemplate;

    public BootstrapAdminLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void acquire() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                try (PreparedStatement statement =
                             connection.prepareStatement("SELECT pg_advisory_xact_lock(?, ?)")) {
                    statement.setInt(1, LOCK_NAMESPACE);
                    statement.setInt(2, LOCK_ID);
                    statement.execute();
                }
                return null;
            }

            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new IllegalStateException("First-Admin bootstrap requires an active transaction");
            }
            NON_POSTGRES_TEST_LOCK.lock();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    NON_POSTGRES_TEST_LOCK.unlock();
                }
            });
            return null;
        });
    }
}
