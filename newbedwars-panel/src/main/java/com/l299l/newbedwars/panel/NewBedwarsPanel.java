package com.l299l.newbedwars.panel;

import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.LoginLimiter;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.SecretFile;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.dashboard.DashboardService;
import com.l299l.newbedwars.panel.dashboard.DashboardState;
import com.l299l.newbedwars.panel.tls.TlsSetup;
import com.l299l.newbedwars.panel.web.PanelServer;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;
import com.l299l.newbedwars.panel.web.SessionCookie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class NewBedwarsPanel extends JavaPlugin {
    private PanelConfig config;
    private AccountStore accounts;
    private AuditLog audit;
    private SessionManager sessions;
    private LoginLimiter limiter;
    private DashboardService dashboard;
    private PanelServer server;
    private BukkitTask cleanup;

    @Override
    public void onEnable() {
        saveDefaultConfig();
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
        audit = new AuditLog(folder.resolve("audit.log"), getLogger(), System::currentTimeMillis);
        sessions = new SessionManager(TimeUnit.MINUTES.toMillis(config.sessionIdleMinutes()),
                TimeUnit.HOURS.toMillis(config.sessionMaxHours()), System::currentTimeMillis);
        limiter = new LoginLimiter(config.loginMaxAttempts(), TimeUnit.MINUTES.toMillis(config.loginLockMinutes()),
                System::currentTimeMillis);
        dashboard = new DashboardService(this, NewBedwarsAPI.get());

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

        Router router = new Router();
        registerRoutes(router);
        boolean hsts = tls != null && !tls.selfSigned();
        server = new PanelServer(router, sessions, accounts, new SessionCookie(tls != null, config.trustProxyHeaders()),
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
        getLogger().info("Panel running at " + (tls != null ? "https" : "http") + "://" + host + ":" + server.port() + "/");
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
    }

    private void registerRoutes(Router router) {
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
