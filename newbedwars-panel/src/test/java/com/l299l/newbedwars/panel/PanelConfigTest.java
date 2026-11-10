package com.l299l.newbedwars.panel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PanelConfigTest {
    @Test
    void readsValuesAndFallsBackOnBadOnes() {
        assertEquals(PanelConfig.DEFAULTS, PanelConfig.read(key -> null));
        Map<String, Object> values = new HashMap<>();
        values.put("Bind", " 127.0.0.1 ");
        values.put("Port", 70000);
        values.put("Https.Enabled", false);
        values.put("Https.Hostnames", Arrays.asList("panel.example.com", " ", null));
        values.put("Session.IdleMinutes", 5);
        values.put("Login.MaxAttempts", "many");
        values.put("DefaultLanguage", "PL");
        PanelConfig c = PanelConfig.read(values::get);
        assertEquals("127.0.0.1", c.bind());
        assertEquals(8150, c.port());
        assertFalse(c.https());
        assertEquals(List.of("panel.example.com"), c.hostnames());
        assertEquals(5, c.sessionIdleMinutes());
        assertEquals(5, c.loginMaxAttempts());
        assertEquals("pl", c.language());
    }
}
