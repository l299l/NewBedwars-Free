package com.l299l.newbedwars.common.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JdbcConnectorTest {
    @Test
    void defaultsPreferSslAndDoNotAllowKeyRetrieval() {
        String url = JdbcConnector.url("db", 3306, "nbw", null, false);
        assertTrue(url.startsWith("jdbc:mysql://db:3306/nbw?"));
        assertTrue(url.contains("sslMode=PREFERRED"));
        assertFalse(url.contains("allowPublicKeyRetrieval"));
        assertFalse(url.contains("useSSL=false"));
    }

    @Test
    void configuredValuesAreUsed() {
        String url = JdbcConnector.url("db", 3307, "nbw", "verify_ca", true);
        assertTrue(url.contains("sslMode=VERIFY_CA"));
        assertTrue(url.contains("allowPublicKeyRetrieval=true"));
    }

    @Test
    void unknownModeFallsBackToDefault() {
        assertEquals("PREFERRED", JdbcConnector.normalizeSslMode("nonsense"));
        assertEquals("DISABLED", JdbcConnector.normalizeSslMode(" disabled "));
    }
}
