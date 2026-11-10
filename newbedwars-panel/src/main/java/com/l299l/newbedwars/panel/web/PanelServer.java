package com.l299l.newbedwars.panel.web;

import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PanelServer implements HttpHandler {
    public static final int THREADS = 4;
    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src 'self'; font-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'";
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "ico", "image/x-icon");

    private final Router router;
    private final SessionManager sessions;
    private final AccountStore accounts;
    private final SessionCookie cookies;
    private final boolean hsts;
    private final Logger logger;
    private HttpServer server;
    private ExecutorService executor;

    public PanelServer(Router router, SessionManager sessions, AccountStore accounts, SessionCookie cookies,
                       boolean hsts, Logger logger) {
        this.router = router;
        this.sessions = sessions;
        this.accounts = accounts;
        this.cookies = cookies;
        this.hsts = hsts;
        this.logger = logger;
    }

    public void start(InetSocketAddress address, SSLContext tls) throws IOException {
        HttpServer created;
        if (tls == null) {
            created = HttpServer.create(address, 64);
        } else {
            HttpsServer https = HttpsServer.create(address, 64);
            https.setHttpsConfigurator(new HttpsConfigurator(tls));
            created = https;
        }
        AtomicInteger counter = new AtomicInteger();
        executor = Executors.newFixedThreadPool(THREADS, r -> {
            Thread t = new Thread(r, "NewBedwars-Panel-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        created.setExecutor(executor);
        created.createContext("/", this);
        created.start();
        server = created;
    }

    public void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        server = null;
        executor = null;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            Response response;
            try {
                response = dispatch(exchange);
            } catch (HttpError e) {
                response = Response.error(e.status, e.code);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Panel request " + exchange.getRequestMethod() + " "
                        + exchange.getRequestURI().getRawPath() + " failed", e);
                response = Response.error(500, "internal");
            }
            write(exchange, response);
        } catch (IOException ignored) {
        } finally {
            exchange.close();
        }
    }

    private Response dispatch(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String path = exchange.getRequestURI().getRawPath();
        if (!path.startsWith("/api/")) return staticFile(method, path);

        Router.Match match = router.match(method, path);
        if (match == null) return Response.error(404, "not_found");
        Request request = new Request(exchange, match.params(), cookies.clientIp(exchange));
        boolean safe = method.equals("GET") || method.equals("HEAD");
        if (!safe && !cookies.sameOrigin(exchange)) throw new HttpError(403, "bad_origin");

        Optional<SessionManager.Session> session = sessions.get(cookies.read(exchange));
        if (session.isPresent()) {
            Optional<Account> account = accounts.find(session.get().user());
            if (account.isPresent()) {
                request.session = session.get();
                request.account = account.get();
                request.role = accounts.role(account.get().role());
            } else {
                sessions.remove(session.get().id());
            }
        }

        Router.Route route = match.route();
        if (route.access() != Router.Access.PUBLIC) {
            if (request.session == null) throw new HttpError(401, "login_required");
            if (!safe && !csrfMatches(request.session.csrf(), request.header("X-CSRF-Token"))) {
                throw new HttpError(403, "csrf");
            }
            if (route.access() == Router.Access.PERMISSION) {
                if (request.account.mustChangePassword()) throw new HttpError(403, "password_change_required");
                Role role = request.role;
                if (role == null || route.permissions().stream().noneMatch(role::has)) throw new HttpError(403, "forbidden");
            }
        }
        return route.handler().handle(request);
    }

    static boolean csrfMatches(String expected, String given) {
        if (expected == null || given == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }

    private Response staticFile(String method, String path) throws IOException {
        if (!method.equals("GET") && !method.equals("HEAD")) throw new HttpError(405, "method_not_allowed");
        String file = path.equals("/") ? "/index.html" : path;
        if (file.contains("..") || file.contains("\\") || file.contains("%") || file.contains("//")) {
            return Response.error(404, "not_found");
        }
        int dot = file.lastIndexOf('.');
        String type = dot < 0 ? null : TYPES.get(file.substring(dot + 1).toLowerCase(Locale.ROOT));
        if (type == null) return Response.error(404, "not_found");
        try (InputStream in = PanelServer.class.getResourceAsStream("/web" + file)) {
            if (in == null) return Response.error(404, "not_found");
            return Response.bytes(type, in.readAllBytes()).header("Cache-Control", "no-cache");
        }
    }

    private void write(HttpExchange exchange, Response response) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Security-Policy", CSP);
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Cross-Origin-Opener-Policy", "same-origin");
        headers.set("Cross-Origin-Resource-Policy", "same-origin");
        if (hsts) headers.set("Strict-Transport-Security", "max-age=31536000");
        if (exchange.getRequestURI().getRawPath().startsWith("/api/")) headers.set("Cache-Control", "no-store");
        response.headers.forEach(headers::set);
        if (response.contentType != null) headers.set("Content-Type", response.contentType);
        boolean head = exchange.getRequestMethod().equalsIgnoreCase("HEAD");
        if (response.file != null) {
            long size = Files.size(response.file);
            exchange.sendResponseHeaders(response.status, head ? -1 : size);
            if (!head) {
                try (OutputStream out = exchange.getResponseBody()) {
                    Files.copy(response.file, out);
                }
            }
            return;
        }
        if (response.status == 204 || head || response.body.length == 0) {
            exchange.sendResponseHeaders(response.status, -1);
            return;
        }
        exchange.sendResponseHeaders(response.status, response.body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response.body);
        }
    }
}
