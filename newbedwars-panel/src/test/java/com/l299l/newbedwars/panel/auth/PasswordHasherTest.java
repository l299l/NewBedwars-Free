package com.l299l.newbedwars.panel.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher(1000);

    @Test
    void hashesAndVerifies() {
        String stored = hasher.hash("correct horse".toCharArray());
        assertTrue(stored.startsWith("pbkdf2-sha256$1000$"));
        assertTrue(hasher.verify("correct horse".toCharArray(), stored));
        assertFalse(hasher.verify("correct hors".toCharArray(), stored));
        assertNotEquals(stored, hasher.hash("correct horse".toCharArray()));
    }

    @Test
    void malformedHashesNeverMatch() {
        for (String bad : new String[]{null, "", "plain", "md5$1$a$b", "pbkdf2-sha256$x$a$b", "pbkdf2-sha256$0$AA$AA", "pbkdf2-sha256$5$!!$AA"}) {
            assertFalse(hasher.verify("x".toCharArray(), bad), String.valueOf(bad));
        }
    }

    @Test
    void rehashWhenIterationsChange() {
        String stored = hasher.hash("pw".toCharArray());
        assertFalse(hasher.needsRehash(stored));
        assertTrue(new PasswordHasher(2000).needsRehash(stored));
        assertTrue(new PasswordHasher(2000).verify("pw".toCharArray(), stored));
        assertEquals(600_000, PasswordHasher.DEFAULT_ITERATIONS);
    }
}
