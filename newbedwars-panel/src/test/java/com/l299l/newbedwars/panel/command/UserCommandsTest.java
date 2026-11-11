package com.l299l.newbedwars.panel.command;

import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.PasswordHasher;
import com.l299l.newbedwars.panel.auth.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UserCommandsTest {
    @TempDir
    Path dir;

    private AccountStore store;
    private SessionManager sessions;
    private AuditLog audit;
    private UserCommands users;
    private final PasswordHasher hasher = new PasswordHasher(1);

    @BeforeEach
    void setUp() throws Exception {
        store = AccountStore.open(dir.resolve("accounts.dat"), new byte[32]);
        sessions = new SessionManager(60_000, 600_000, () -> 1000L);
        audit = new AuditLog(dir.resolve("audit.log"), Logger.getAnonymousLogger(), () -> 5L);
        users = new UserCommands(store, sessions, hasher, audit, () -> 42L);
    }

    @Test
    void createStoresAOneTimePassword() {
        UserCommands.Result r = users.create("console", "Boss", "ADMIN");
        assertEquals(UserCommands.Code.CREATED, r.code());
        assertEquals("admin", r.role());
        assertEquals(UserCommands.PASSWORD_LENGTH, r.password().length());
        Account boss = store.find("boss").orElseThrow();
        assertTrue(boss.mustChangePassword());
        assertEquals(42L, boss.created());
        assertTrue(hasher.verify(r.password().toCharArray(), boss.passwordHash()));
        assertEquals("user.create", audit.recent(1).get(0).action());
        assertEquals("created", audit.recent(1).get(0).result());
        assertFalse(audit.recent(1).get(0).toString().contains(r.password()));
    }

    @Test
    void createRejectsBadInput() {
        assertEquals(UserCommands.Code.INVALID_NAME, users.create("console", "a", "admin").code());
        assertEquals(UserCommands.Code.INVALID_NAME, users.create("console", "bad name!", "admin").code());
        assertEquals(UserCommands.Code.UNKNOWN_ROLE, users.create("console", "helper", "nope").code());
        users.create("console", "helper", "staff");
        assertEquals(UserCommands.Code.EXISTS, users.create("console", "HELPER", "admin").code());
        assertEquals(1, store.accounts().size());
    }

    @Test
    void resetChangesThePasswordAndLogsOut() {
        String first = users.create("console", "helper", "staff").password();
        SessionManager.Session session = sessions.create("helper");
        UserCommands.Result r = users.reset("console", "HELPER");
        assertEquals(UserCommands.Code.RESET, r.code());
        assertEquals("helper", r.name());
        assertNotEquals(first, r.password());
        Account helper = store.find("helper").orElseThrow();
        assertTrue(hasher.verify(r.password().toCharArray(), helper.passwordHash()));
        assertFalse(hasher.verify(first.toCharArray(), helper.passwordHash()));
        assertTrue(sessions.get(session.id()).isEmpty());
        assertEquals(UserCommands.Code.NOT_FOUND, users.reset("console", "ghost").code());
    }

    @Test
    void deleteKeepsTheLastAdmin() {
        users.create("console", "boss", "admin");
        users.create("console", "helper", "staff");
        SessionManager.Session session = sessions.create("helper");
        assertEquals(UserCommands.Code.LAST_ADMIN, users.delete("console", "boss").code());
        assertEquals(UserCommands.Code.DELETED, users.delete("console", "helper").code());
        assertTrue(sessions.get(session.id()).isEmpty());
        assertEquals(UserCommands.Code.NOT_FOUND, users.delete("console", "helper").code());
        users.create("console", "second", "admin");
        assertEquals(UserCommands.Code.DELETED, users.delete("console", "boss").code());
        assertEquals(1, users.list().size());
    }

    @Test
    void passwordsUseTheAlphabet() {
        for (int i = 0; i < 50; i++) assertTrue(UserCommands.randomPassword().matches("[A-HJ-NP-Za-km-z2-9]{16}"));
    }
}
