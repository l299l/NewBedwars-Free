package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.common.sql.JdbcConnector;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseTest {

    private final List<String> logged = new CopyOnWriteArrayList<>();

    private Logger logger() {
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord r) {
                logged.add(r.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return log;
    }

    private void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long end = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < end) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }

    private boolean logContains(String part) {
        return logged.stream().anyMatch(m -> m.contains(part));
    }

    @Test
    void closeDoesNotWaitForAPendingRetryAndRunsItOnce() throws Exception {
        JdbcConnector connector = mock(JdbcConnector.class);
        when(connector.get()).thenThrow(new SQLException("down"));
        Database db = new Database(logger(), "s", connector, 30_000L);
        db.submitRetrying("Saving the stats of Leo", c -> {
        });
        await(() -> logContains("tried again"));
        long start = System.nanoTime();
        db.close();
        assertTrue((System.nanoTime() - start) / 1_000_000L < 5000L);
        verify(connector, times(2)).get();
        assertTrue(logContains("Saving the stats of Leo failed and was dropped"));
        verify(connector).close();
    }

    @Test
    void pendingRetrySucceedingAtCloseIsWritten() throws Exception {
        JdbcConnector connector = mock(JdbcConnector.class);
        Connection connection = mock(Connection.class);
        when(connector.get()).thenThrow(new SQLException("down")).thenReturn(connection);
        Database db = new Database(logger(), "s", connector, 30_000L);
        int[] ran = {0};
        db.submitRetrying("Saving", c -> ran[0]++);
        await(() -> logContains("tried again"));
        db.close();
        assertEquals(1, ran[0]);
    }

    @Test
    void everyDroppedSaveIsLoggedEvenWhenErrorsAreRateLimited() throws Exception {
        JdbcConnector connector = mock(JdbcConnector.class);
        when(connector.get()).thenThrow(new SQLException("down"));
        Database db = new Database(logger(), "s", connector, 30_000L);
        db.submit("First save", c -> {
        });
        db.submit("Second save", c -> {
        });
        await(() -> logContains("Second save failed and was dropped"));
        assertTrue(logContains("First save failed and was dropped"));
        db.close();
    }

    @Test
    void failureAfterTheStatementWasSentIsNotRetried() throws Exception {
        JdbcConnector connector = mock(JdbcConnector.class);
        when(connector.get()).thenReturn(mock(Connection.class));
        Database db = new Database(logger(), "s", connector, 30_000L);
        int[] ran = {0};
        db.submitRetrying("Saving the stats of Leo", c -> {
            ran[0]++;
            throw new SQLException("Communications link failure");
        });
        await(() -> logContains("not tried again"));
        db.close();
        assertEquals(1, ran[0]);
    }

    @Test
    void rolledBackFailureIsRetried() throws Exception {
        JdbcConnector connector = mock(JdbcConnector.class);
        when(connector.get()).thenReturn(mock(Connection.class));
        Database db = new Database(logger(), "s", connector, 30_000L);
        int[] ran = {0};
        db.submitRetrying("Saving", c -> {
            ran[0]++;
            throw new java.sql.SQLTransactionRollbackException("deadlock", "40001");
        });
        await(() -> logContains("tried again"));
        db.close();
        assertEquals(2, ran[0]);
    }

    @Test
    void closeDoesNotBlockOnAConnectorStuckInAConnectAttempt() throws Exception {
        JdbcConnector connector = mock(JdbcConnector.class);
        java.util.concurrent.CountDownLatch inGet = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch closed = new java.util.concurrent.CountDownLatch(1);
        when(connector.get()).thenAnswer(i -> {
            inGet.countDown();
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
            return mock(Connection.class);
        });
        doAnswer(i -> {
            release.await();
            closed.countDown();
            return null;
        }).when(connector).close();
        Database db = new Database(logger(), "s", connector, 30_000L, 200L);
        db.submit("Saving the stats of Leo", c -> {
        });
        assertTrue(inGet.await(3, java.util.concurrent.TimeUnit.SECONDS));
        long start = System.nanoTime();
        db.close();
        assertTrue((System.nanoTime() - start) / 1_000_000L < 2000L);
        assertTrue(logContains("The database did not answer in time"));
        assertEquals(1, closed.getCount());
        release.countDown();
        assertTrue(closed.await(3, java.util.concurrent.TimeUnit.SECONDS));
        verify(connector).close();
    }
}
