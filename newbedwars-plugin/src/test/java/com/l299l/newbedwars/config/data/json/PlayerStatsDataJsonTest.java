package com.l299l.newbedwars.config.data.json;

import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStatsDataJsonTest {

    private static LinkedTreeMap<String, Object> row(String name, int wins) {
        LinkedTreeMap<String, Object> m = new LinkedTreeMap<>();
        if (name != null) m.put("name", name);
        m.put("wins", (double) wins);
        m.put("gamesPlayed", (double) wins);
        return m;
    }

    @Test
    void legacyNameKeysMoveToTheProfileUuid() {
        PlayerManager pm = new PlayerManager();
        UUID leo = UUID.randomUUID();
        pm.addPlayer(new PlayerProfile(leo, "Leo", Language.English, PlayerProfile.defaultFastBuy()));
        Map<String, LinkedTreeMap<String, Object>> raw = new LinkedHashMap<>();
        raw.put("Leo", row(null, 4));
        raw.put("Ghost", row(null, 2));
        assertEquals(2, PlayerStatsDataJson.read(raw, pm));
        assertEquals(4, pm.getStats(leo).wins());
        assertEquals(2, pm.getStats(PlayerManager.offlineId("Ghost")).wins());
        assertEquals(2, pm.getStats("Ghost").wins());
    }

    @Test
    void uuidKeysAreReadWithTheirNameAndNotCountedAsLegacy() {
        PlayerManager pm = new PlayerManager();
        UUID id = UUID.randomUUID();
        Map<String, LinkedTreeMap<String, Object>> raw = new LinkedHashMap<>();
        raw.put(id.toString(), row("Alex", 7));
        assertEquals(0, PlayerStatsDataJson.read(raw, pm));
        assertEquals(7, pm.getStats("Alex").wins());
    }

    @Test
    void savedFileIsKeyedByUuidAndKeepsTheName() {
        PlayerManager pm = new PlayerManager();
        UUID id = UUID.randomUUID();
        pm.addStats(id, "Alex", new PlayerStats(1, 2, 3, 4, 5, 6, 7));
        Map<String, Map<String, Object>> file = PlayerStatsDataJson.toFile(pm);
        assertEquals("Alex", file.get(id.toString()).get("name"));
        assertEquals(5, file.get(id.toString()).get("finalKills"));
        PlayerManager reloaded = new PlayerManager();
        Map<String, LinkedTreeMap<String, Object>> raw = new LinkedHashMap<>();
        LinkedTreeMap<String, Object> m = new LinkedTreeMap<>();
        file.get(id.toString()).forEach((k, v) -> m.put(k, v instanceof Integer i ? (double) i : v));
        raw.put(id.toString(), m);
        PlayerStatsDataJson.read(raw, reloaded);
        assertEquals(new PlayerStats(1, 2, 3, 4, 5, 6, 7), reloaded.getStats(id));
    }
}
