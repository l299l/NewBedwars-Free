package com.l299l.newbedwars.text;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokensTest {
    private static final Map<String, String> VALUES = Map.of("players", "7", "maxPlayers", "32", "server_name", "S");

    @Test
    void bothDelimitersWork() {
        assertEquals("7 32 7 S", Tokens.apply("/players/ /maxplayers/ {players} {servername}", VALUES));
    }

    @Test
    void nameIgnoresCaseAndUnderscores() {
        assertEquals("32 32 32", Tokens.apply("{max_players} /maxplayers/ {MaxPlayers}", VALUES));
    }

    @Test
    void unknownTokensAreKeptAndDoNotSwallowKnownOnes() {
        assertEquals("/foo/7 {bar} a/b", Tokens.apply("/foo//players/ {bar} a/b", Map.of("players", "7")));
    }

    @Test
    void replaceKeyHandlesBraces() {
        assertEquals("7 7", Tokens.replaceKey("/players/ {players}", "/players/", "7"));
    }
}
