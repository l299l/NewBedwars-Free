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
import java.util.function.Consumer;

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
    boolean updatesCheck = true;
    boolean updatesNotify = true;
    String updatesChannel = "BETA";

    static ProxyConfig load(Path dataDir) throws IOException {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProxyConfig.class);
        return load(dataDir, message -> logger.warn(message));
    }

    static ProxyConfig load(Path dataDir, Consumer<String> warn) throws IOException {
        Path file = dataDir.resolve("config.yml");
        copyDefault(file, "config.yml");
        for (String lang : List.of("en", "pl")) copyDefault(dataDir.resolve("lang").resolve(lang + ".yml"), "lang/" + lang + ".yml");
        Map<String, Object> m;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml().load(r);
            if (loaded instanceof Map<?, ?> map) {
                m = cast(map);
            } else {
                warn.accept("config.yml is empty or not a YAML map; the default settings are used");
                m = Map.of();
            }
        }
        ProxyConfig c = new ProxyConfig();
        c.gameServers = strings(m.get("game-servers"), "game-servers", warn);
        c.lobbyServers = strings(m.get("lobby-servers"), "lobby-servers", warn);
        c.ticketTimeoutSeconds = integer(m.get("ticket-timeout-seconds"), 10, "ticket-timeout-seconds", warn);
        c.stateStaleSeconds = integer(m.get("state-stale-seconds"), 15, "state-stale-seconds", warn);
        c.partyInviteSeconds = integer(m.get("party-invite-seconds"), 60, "party-invite-seconds", warn);
        Object mysql = m.get("mysql");
        if (mysql instanceof Map<?, ?> raw) {
            Map<String, Object> db = cast(raw);
            c.mysqlEnabled = bool(db.get("enabled"), false, "mysql.enabled", warn);
            c.mysqlHost = text(db.get("host"), "localhost");
            c.mysqlPort = integer(db.get("port"), 3306, "mysql.port", warn);
            c.mysqlDatabase = text(db.get("database"), "newbedwars");
            c.mysqlUser = text(db.get("user"), "root");
            c.mysqlPassword = db.get("password") == null ? "" : String.valueOf(db.get("password"));
            c.mysqlSslMode = JdbcConnector.normalizeSslMode(db.get("ssl-mode") == null ? null : String.valueOf(db.get("ssl-mode")));
            c.mysqlAllowPublicKeyRetrieval = bool(db.get("allow-public-key-retrieval"), false, "mysql.allow-public-key-retrieval", warn);
            c.mysqlPollSeconds = integer(db.get("poll-seconds"), 5, "mysql.poll-seconds", warn);
        } else if (mysql != null) {
            warn.accept("config.yml: 'mysql' must be a section; MySQL stays disabled");
        }
        Object updatesSection = m.get("updates");
        if (updatesSection instanceof Map<?, ?> raw) {
            Map<String, Object> updates = cast(raw);
            c.updatesCheck = bool(updates.get("check"), true, "updates.check", warn);
            c.updatesNotify = bool(updates.get("notify"), true, "updates.notify", warn);
            if (updates.get("channel") != null) c.updatesChannel = String.valueOf(updates.get("channel"));
        } else if (updatesSection != null) {
            warn.accept("config.yml: 'updates' must be a section; the default update settings are used");
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

    private static List<String> strings(Object o, String key, Consumer<String> warn) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object x : list) if (x != null) out.add(String.valueOf(x));
        } else if (o != null) {
            warn.accept("config.yml: '" + key + "' must be a list; it is ignored");
        }
        return out;
    }

    private static String text(Object o, String def) {
        return o == null ? def : String.valueOf(o);
    }

    static boolean bool(Object o, boolean def, String key, Consumer<String> warn) {
        if (o == null) return def;
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) {
            String t = s.trim();
            if (t.equalsIgnoreCase("true")) return true;
            if (t.equalsIgnoreCase("false")) return false;
        }
        warn.accept("config.yml: '" + key + "' must be true or false (found '" + o + "'); " + def + " is used");
        return def;
    }

    static int integer(Object o, int def, String key, Consumer<String> warn) {
        if (o == null) return def;
        Integer value = null;
        if (o instanceof Number n) {
            value = n.intValue();
        } else if (o instanceof String s) {
            try {
                value = Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (value == null || value <= 0) {
            warn.accept("config.yml: '" + key + "' must be a positive whole number (found '" + o + "'); " + def + " is used");
            return def;
        }
        return value;
    }
}
