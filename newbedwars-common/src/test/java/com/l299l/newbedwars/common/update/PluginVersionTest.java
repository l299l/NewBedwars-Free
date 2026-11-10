package com.l299l.newbedwars.common.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginVersionTest {

    private static PluginVersion v(String s) {
        return PluginVersion.parse(s);
    }

    @Test
    void parsesTheFormsWeRelease() {
        assertEquals(v("2.6-beta"), v("v2.6-beta"));
        assertEquals(v("2.6-beta"), v("2.6-beta-proxy"));
        assertEquals(v("2.7"), v("2.7.0"));
        assertEquals(Channel.BETA, v("2.6-beta").channel());
        assertEquals(Channel.RELEASE, v("2.7").channel());
        assertEquals(Channel.ALPHA, v("3.0-alpha.2").channel());
        assertEquals(Channel.BETA, v("3.0-rc1").channel());
    }

    @Test
    void rejectsUnknownForms() {
        assertNull(v(null));
        assertNull(v(""));
        assertNull(v("latest"));
        assertNull(v("2.x"));
        assertNull(v("2.6-SNAPSHOT"));
        assertNull(v("2.6-beta-proxy-extra"));
    }

    @Test
    void ordersNumbersThenStage() {
        assertTrue(v("2.7-beta").isNewerThan(v("2.6-beta")));
        assertTrue(v("2.10-beta").isNewerThan(v("2.9-beta")));
        assertTrue(v("2.6").isNewerThan(v("2.6-beta")));
        assertTrue(v("2.6-rc").isNewerThan(v("2.6-beta.3")));
        assertTrue(v("2.6-beta.2").isNewerThan(v("2.6-beta")));
        assertTrue(v("2.6-beta").isNewerThan(v("2.6-alpha")));
        assertTrue(v("2.6.1-beta").isNewerThan(v("2.6-beta")));
        assertFalse(v("2.6-beta").isNewerThan(v("2.6-beta")));
        assertFalse(v("2.5").isNewerThan(v("2.6-beta")));
        assertFalse(v("2.7").isNewerThan(null));
    }

    @Test
    void gameVersionsCompare() {
        assertTrue(v("26.3").compareTo(v("1.21.11")) > 0);
        assertTrue(v("1.17.1").compareTo(v("1.17")) > 0);
        assertEquals(0, v("1.17").compareTo(v("1.17.0")));
        assertEquals(v("1.17").hashCode(), v("1.17.0").hashCode());
    }

    @Test
    void channelAllowsItselfAndMoreStable() {
        assertTrue(Channel.BETA.allows(Channel.RELEASE));
        assertTrue(Channel.BETA.allows(Channel.BETA));
        assertFalse(Channel.BETA.allows(Channel.ALPHA));
        assertFalse(Channel.RELEASE.allows(Channel.BETA));
        assertTrue(Channel.ALPHA.allows(Channel.ALPHA));
        assertEquals(Channel.BETA, Channel.parse(" beta ", Channel.RELEASE));
        assertEquals(Channel.RELEASE, Channel.parse("nightly", Channel.RELEASE));
    }
}
