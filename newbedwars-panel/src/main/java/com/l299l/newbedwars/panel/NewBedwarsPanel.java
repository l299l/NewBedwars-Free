package com.l299l.newbedwars.panel;

import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.LoginLimiter;
import com.l299l.newbedwars.panel.auth.PasswordHasher;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.SecretFile;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.command.PanelCommand;
import com.l299l.newbedwars.panel.command.UserCommands;
import com.l299l.newbedwars.panel.dashboard.DashboardService;
import com.l299l.newbedwars.panel.gamelog.GameLog;
import com.l299l.newbedwars.panel.gamelog.GameLogService;
import com.l299l.newbedwars.panel.routes.AdminRoutes;
import com.l299l.newbedwars.panel.routes.GameRoutes;
import com.l299l.newbedwars.panel.routes.ShopRoutes;
import com.l299l.newbedwars.panel.routes.StatsRoutes;
import com.l299l.newbedwars.panel.routes.ArenaRoutes;
import com.l299l.newbedwars.panel.routes.AuthRoutes;
import com.l299l.newbedwars.panel.dashboard.DashboardState;
import com.l299l.newbedwars.panel.tls.TlsSetup;
import com.l299l.newbedwars.panel.web.PanelServer;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;
import com.l299l.newbedwars.panel.web.SessionCookie;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class NewBedwarsPanel extends JavaPlugin {
    private static final List<String> LANGUAGES = List.of("en", "pl");

    public record Status(String url, boolean https, String fingerprint, int sessions) {}

    private PanelConfig config;
    private AccountStore accounts;
    private AuditLog audit;
    private SessionManager sessions;
    private LoginLimiter limiter;
    private DashboardService dashboard;
    private PanelServer server;
    private BukkitTask cleanup;
    private PasswordHasher hasher;
    private GameLogService gameLog;
    private PanelMessages messages = new PanelMessages(PanelMessages.FALLBACK);
    private UserCommands userCommands;
    private String url;
    private boolean https;
    private String fingerprint;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadMessages(PanelConfig.read(getConfig()::get).language());
        PluginCommand command = getCommand("nbwpanel");
        if (command != null) {
            PanelCommand executor = new PanelCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        if (!httpServerAvailable()) {
            getLogger().severe("This Java runtime has no built-in web server (module jdk.httpserver). "
                    + "Run the server with a full JDK/JRE to use the panel.");
            return;
        }
        start();
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    private static boolean httpServerAvailable() {
        try {
            Class.forName("com.sun.net.httpserver.HttpServer");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    void start() {
        reloadConfig();
        config = PanelConfig.read(getConfig()::get);
        Path folder = getDataFolder().toPath();
        byte[] key;
        try {
            key = SecretFile.loadOrCreateKey(folder.resolve("panel.key"), System.getenv("NBW_PANEL_KEY"));
            accounts = AccountStore.open(folder.resolve("accounts.dat"), key);
        } catch (IOException | RuntimeException e) {
            getLogger().severe("The panel did not start: " + e.getMessage());
            return;
        }
        hasher = new PasswordHasher();
        audit = new AuditLog(folder.resolve("audit.log"), getLogger(), System::currentTimeMillis);
        sessions = new SessionManager(TimeUnit.MINUTES.toMillis(config.sessionIdleMinutes()),
                TimeUnit.HOURS.toMillis(config.sessionMaxHours()), System::currentTimeMillis);
        userCommands = new UserCommands(accounts, sessions, hasher, audit, System::currentTimeMillis);
        limiter = new LoginLimiter(config.loginMaxAttempts(), TimeUnit.MINUTES.toMillis(config.loginLockMinutes()),
                System::currentTimeMillis);
        dashboard = new DashboardService(this, NewBedwarsAPI.get());
        if (gameLog == null) {
            gameLog = new GameLogService(new GameLog(System::currentTimeMillis),
                    new com.l299l.newbedwars.panel.gamelog.CommandFilter(config.ignoredCommands()), world -> NewBedwarsAPI.get().getArena(world),
                    com.l299l.newbedwars.panel.gamelog.GameLogService::mayRun);
            getServer().getPluginManager().registerEvents(gameLog, this);
        }

        TlsSetup.Result tls = null;
        if (config.https()) {
            try {
                tls = config.keystore().isEmpty()
                        ? TlsSetup.selfSigned(folder.resolve("selfsigned.p12"), key,
                        TlsSetup.defaultNames(config.bind(), config.hostnames()), new Date())
                        : TlsSetup.ownKeystore(folder.resolve(config.keystore()), config.keystorePassword().toCharArray());
            } catch (IOException | GeneralSecurityException e) {
                getLogger().severe("The panel did not start: the HTTPS certificate could not be loaded (" + e.getMessage() + ")");
                return;
            }
        } else {
            getLogger().warning("Https.Enabled is false: the panel runs over plain HTTP. Passwords and sessions can be read "
                    + "by anyone on the network unless a reverse proxy adds HTTPS in front of it.");
        }

        SessionCookie cookies = new SessionCookie(tls != null, config.trustProxyHeaders());
        Router router = new Router();
        registerRoutes(router, cookies);
        boolean hsts = tls != null && !tls.selfSigned();
        server = new PanelServer(router, sessions, accounts, cookies,
                hsts, getLogger());
        try {
            server.start(new InetSocketAddress(config.bind(), config.port()), tls == null ? null : tls.context());
        } catch (BindException e) {
            getLogger().severe("The panel did not start: port " + config.port() + " on " + config.bind() + " is already in use.");
            server = null;
            return;
        } catch (IOException | RuntimeException | LinkageError e) {
            getLogger().log(Level.SEVERE, "The panel did not start", e);
            server = null;
            return;
        }
        dashboard.start();
        cleanup = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            sessions.cleanup();
            limiter.cleanup();
        }, 1200L, 1200L);

        String host = config.bind().equals("0.0.0.0") || config.bind().equals("::") ? "localhost" : config.bind();
        https = tls != null;
        url = (https ? "https" : "http") + "://" + host + ":" + server.port() + "/";
        fingerprint = tls != null && tls.selfSigned() ? tls.fingerprint() : null;
        getLogger().info("Panel running at " + url);
        if (accounts.isEmpty()) {
            getLogger().info("No panel accounts yet. Create the first admin with: nbwpanel user create <name> admin");
        }
        if (tls != null && tls.selfSigned()) {
            getLogger().info((tls.created() ? "Created a self-signed certificate" : "Self-signed certificate")
                    + ", SHA-256 fingerprint: " + tls.fingerprint());
            getLogger().info("Your browser will warn about it once; compare the fingerprint, or set Https.Keystore to use your own certificate.");
        }
    }

    void shutdown() {
        if (cleanup != null) cleanup.cancel();
        cleanup = null;
        if (dashboard != null) dashboard.stop();
        if (server != null) server.stop();
        server = null;
        url = null;
        fingerprint = null;
    }

    private void loadMessages(String defaultLanguage) {
        PanelMessages loaded = new PanelMessages(defaultLanguage);
        for (String language : LANGUAGES) {
            String resource = "lang/" + language + ".yml";
            try (InputStream in = getResource(resource)) {
                if (in != null) {
                    try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        loaded.put(language, flat(YamlConfiguration.loadConfiguration(reader)));
                    }
                }
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Could not read " + resource, e);
            }
            File file = new File(getDataFolder(), resource);
            if (!file.exists()) saveResource(resource, false);
            loaded.put(language, flat(YamlConfiguration.loadConfiguration(file)));
        }
        messages = loaded;
    }

    private static Map<String, String> flat(YamlConfiguration yaml) {
        Map<String, String> map = new HashMap<>();
        for (String key : yaml.getKeys(false)) {
            if (yaml.isString(key)) map.put(key, yaml.getString(key));
        }
        return map;
    }

    public Status status() {
        return new Status(url, https, fingerprint, sessions == null ? 0 : sessions.count());
    }

    public PanelMessages messages() {
        return messages;
    }

    public UserCommands userCommands() {
        return userCommands;
    }

    private void registerRoutes(Router router, SessionCookie cookies) {
        new AuthRoutes(accounts, sessions, limiter, hasher, cookies, audit, config.language()).register(router);
        new ArenaRoutes(NewBedwarsAPI.get().getMaintenance(), audit, config.uploadMaxMegabytes() * 1024L * 1024L).register(router);
        new AdminRoutes(accounts, userCommands, audit).register(router);
        new StatsRoutes(NewBedwarsAPI.get()).register(router);
        new GameRoutes(NewBedwarsAPI.get().getGameControl(), gameLog, audit).register(router);
        new ShopRoutes(NewBedwarsAPI.get().getShop(), audit).register(router);
        router.allowed("GET", "/api/dashboard", Set.of(Permission.DASHBOARD_VIEW), request -> {
            DashboardState.Snapshot snapshot = dashboard.snapshot();
            String known = request.query("rev");
            if (snapshot == null) return Response.error(503, "not_ready");
            if (known != null && known.equals(String.valueOf(snapshot.revision()))) return Response.noContent();
            JsonObject body = new JsonObject();
            body.addProperty("rev", snapshot.revision());
            body.add("data", snapshot.data());
            return Response.ok(body);
        });
    }

    public AccountStore accounts() {
        return accounts;
    }

    public AuditLog audit() {
        return audit;
    }

    public SessionManager sessions() {
        return sessions;
    }

    public LoginLimiter limiter() {
        return limiter;
    }

    public PanelConfig panelConfig() {
        return config;
    }
}
