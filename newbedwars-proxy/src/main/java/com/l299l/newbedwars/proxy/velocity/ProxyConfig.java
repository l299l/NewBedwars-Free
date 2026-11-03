package com.l299l.newbedwars.proxy.velocity;

import com.l299l.newbedwars.common.sql.JdbcConnector;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ProxyConfig {
    List<String> gameServers = new ArrayList<>();
    List<String> lobbyServers = new ArrayList<>();
    int ticketTimeoutSeconds = 10;
    int stateStaleSeconds = 15;
    int partyInviteSeconds = 60;
    boolean mysqlEnabled;
    String mysqlHost = "localhost";
    int mysqlPort = 3306;
    String mysqlDatabase = "newbedwars";
    String mysqlUser = "root";
    String mysqlPassword = "";
    String mysqlSslMode = "PREFERRED";
    boolean mysqlAllowPublicKeyRetrieval;
    int mysqlPollSeconds = 5;

    static ProxyConfig load(Path dataDir) throws IOException {
        Path file = dataDir.resolve("config.yml");
        copyDefault(file, "config.yml");
        for (String lang : List.of("en", "pl")) copyDefault(dataDir.resolve("lang").resolve(lang + ".yml"), "lang/" + lang + ".yml");
        Map<String, Object> m;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml().load(r);
            m = loaded instanceof Map<?, ?> map ? cast(map) : Map.of();
        }
        ProxyConfig c = new ProxyConfig();
        c.gameServers = strings(m.get("game-servers"));
        c.lobbyServers = strings(m.get("lobby-servers"));
        c.ticketTimeoutSeconds = integer(m.get("ticket-timeout-seconds"), 10);
        c.stateStaleSeconds = integer(m.get("state-stale-seconds"), 15);
        c.partyInviteSeconds = integer(m.get("party-invite-seconds"), 60);
        if (m.get("mysql") instanceof Map<?, ?> raw) {
            Map<String, Object> db = cast(raw);
            c.mysqlEnabled = Boolean.TRUE.equals(db.get("enabled"));
            c.mysqlHost = String.valueOf(db.getOrDefault("host", "localhost"));
            c.mysqlPort = integer(db.get("port"), 3306);
            c.mysqlDatabase = String.valueOf(db.getOrDefault("database", "newbedwars"));
            c.mysqlUser = String.valueOf(db.getOrDefault("user", "root"));
            c.mysqlPassword = db.get("password") == null ? "" : String.valueOf(db.get("password"));
            c.mysqlSslMode = JdbcConnector.normalizeSslMode(db.get("ssl-mode") == null ? null : String.valueOf(db.get("ssl-mode")));
            c.mysqlAllowPublicKeyRetrieval = Boolean.TRUE.equals(db.get("allow-public-key-retrieval"));
            c.mysqlPollSeconds = integer(db.get("poll-seconds"), 5);
        }
        return c;
    }

    static Map<String, String> loadLanguage(Path file, String resource) throws IOException {
        Map<String, String> out = new java.util.HashMap<>();
        try (InputStream in = ProxyConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) flat(yaml().load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)), out);
        }
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                flat(yaml().load(r), out);
            }
        }
        return out;
    }

    private static Yaml yaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    private static void flat(Object loaded, Map<String, String> out) {
        if (!(loaded instanceof Map<?, ?> map)) return;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getValue() != null) out.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
        }
    }

    private static void copyDefault(Path target, String resource) throws IOException {
        if (Files.exists(target)) return;
        Files.createDirectories(target.getParent());
        try (InputStream in = ProxyConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Missing resource " + resource);
            Files.copy(in, target);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> list) for (Object x : list) if (x != null) out.add(String.valueOf(x));
        return out;
    }

    private static int integer(Object o, int def) {
        return o instanceof Number n && n.intValue() > 0 ? n.intValue() : def;
    }
}
