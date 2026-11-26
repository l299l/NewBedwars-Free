package com.l299l.newbedwars.update;

import com.l299l.newbedwars.common.update.PluginVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateServiceVersionTest {

    @Test
    void serverVersionIgnoresBuildSuffixes() {
        assertEquals(0, UpdateService.serverVersion("26.3.build.237-stable").compareTo(PluginVersion.parse("26.3")));
        assertEquals(0, UpdateService.serverVersion("26.1.2.build.5-stable").compareTo(PluginVersion.parse("26.1.2")));
        assertEquals(0, UpdateService.serverVersion("1.17.1-R0.1-SNAPSHOT").compareTo(PluginVersion.parse("1.17.1")));
    }

    @Test
    void serverVersionIsNewerThanOldReleases() {
        assertTrue(UpdateService.serverVersion("26.3.build.237-stable").compareTo(PluginVersion.parse("1.21.4")) > 0);
        assertTrue(UpdateService.serverVersion("1.17.1-R0.1-SNAPSHOT").compareTo(PluginVersion.parse("1.20")) < 0);
    }

    @Test
    void serverVersionIsNullWithoutNumbers() {
        assertNull(UpdateService.serverVersion("build-1"));
        assertNull(UpdateService.serverVersion(null));
    }
}
