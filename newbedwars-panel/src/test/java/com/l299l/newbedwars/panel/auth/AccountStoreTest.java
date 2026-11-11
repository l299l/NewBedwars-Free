package com.l299l.newbedwars.panel.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccountStoreTest {
    @TempDir
    Path dir;

    private byte[] key(int seed) {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (seed + i);
        return key;
    }

    private Account account(String name, String role) {
        return new Account(name, "pbkdf2-sha256$1$AA$AA", role, true, 1234L);
    }

    @Test
    void roundTripIsEncrypted() throws IOException {
        Path file = dir.resolve("accounts.dat");
        AccountStore store = AccountStore.open(file, key(1));
        store.putRole(Role.custom("Builders", Set.of(Permission.DASHBOARD_VIEW, Permission.ARENA_BACKUP)));
        store.put(account("Leon_Admin", "ADMIN"));
        store.put(account("helper", "builders"));

        String raw = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        assertFalse(raw.contains("Leon_Admin"));
        assertFalse(raw.contains("builders"));

        AccountStore again = AccountStore.open(file, key(1));
        Account leon = again.find("leon_admin").orElseThrow();
        assertEquals("Leon_Admin", leon.name());
        assertEquals("admin", leon.role());
        assertTrue(leon.mustChangePassword());
        assertEquals(Set.of(Permission.DASHBOARD_VIEW, Permission.ARENA_BACKUP), again.role("BUILDERS").permissions());
        assertEquals(List.of("admin", "staff", "builders"), again.roles().stream().map(Role::name).toList());
        assertEquals(2, again.accounts().size());
    }

    @Test
    void wrongKeyOrTamperingIsRefused() throws IOException {
        Path file = dir.resolve("accounts.dat");
        AccountStore.open(file, key(1)).put(account("admin1", "admin"));
        IOException wrong = assertThrows(IOException.class, () -> AccountStore.open(file, key(2)));
        assertTrue(wrong.getMessage().contains("key"));

        byte[] data = Files.readAllBytes(file);
        data[data.length - 3] ^= 0x01;
        Files.write(file, data);
        assertThrows(IOException.class, () -> AccountStore.open(file, key(1)));
    }

    @Test
    void rolesAndAdminsAreGuarded() throws IOException {
        AccountStore store = AccountStore.open(dir.resolve("a.dat"), key(3));
        assertThrows(IllegalArgumentException.class, () -> store.put(account("ghost", "nope")));
        assertThrows(IllegalArgumentException.class, () -> Role.custom("admin", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> Role.custom("x y", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> Role.custom("mods", Set.of("server.stop")));
        assertThrows(IllegalArgumentException.class, () -> store.removeRole("staff"));

        store.put(account("boss", "admin"));
        assertTrue(store.isLastAdmin("BOSS"));
        store.put(account("boss2", "admin"));
        assertFalse(store.isLastAdmin("boss"));
        assertFalse(store.isLastAdmin("nobody"));

        store.putRole(Role.custom("mods", Set.of(Permission.DASHBOARD_VIEW)));
        store.put(account("mod1", "mods"));
        assertThrows(IllegalStateException.class, () -> store.removeRole("mods"));
        assertTrue(store.remove("mod1"));
        assertFalse(store.remove("mod1"));
        assertTrue(store.removeRole("MODS"));
        assertNull(store.role("mods"));
        assertTrue(Role.ADMIN_ROLE.permissions().containsAll(Permission.ALL));
        assertEquals(Set.of(Permission.DASHBOARD_VIEW, Permission.STATS_VIEW), Role.STAFF_ROLE.permissions());
    }

    @Test
    void namesAreChecked() {
        assertTrue(Account.isValidName("Leon.K-2_x"));
        assertFalse(Account.isValidName("ab"));
        assertFalse(Account.isValidName("with space"));
        assertFalse(Account.isValidName("x".repeat(33)));
        assertFalse(Account.isValidName(null));
    }

    @Test
    void keyFileIsCreatedOnceOrTakenFromTheEnvironment() throws IOException {
        Path keyFile = dir.resolve("panel.key");
        byte[] first = SecretFile.loadOrCreateKey(keyFile, null);
        assertEquals(32, first.length);
        assertArrayEquals(first, SecretFile.loadOrCreateKey(keyFile, ""));
        byte[] env = key(9);
        assertArrayEquals(env, SecretFile.loadOrCreateKey(dir.resolve("unused.key"), Base64.getEncoder().encodeToString(env)));
        assertFalse(Files.exists(dir.resolve("unused.key")));
        assertThrows(IOException.class, () -> SecretFile.loadOrCreateKey(keyFile, "not base64!"));
        assertThrows(IOException.class, () -> SecretFile.loadOrCreateKey(keyFile, Base64.getEncoder().encodeToString(new byte[8])));
        Files.write(keyFile, new byte[5]);
        assertThrows(IOException.class, () -> SecretFile.loadOrCreateKey(keyFile, null));
    }
}
