package com.l299l.newbedwars.panel.web;

import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.network.NodeAuth;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PanelServer implements HttpHandler {
    public static final int THREADS = 8;
    public static final int GATEWAY_THREADS = 16;
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
    private final NodeAuth.Verifier nodeAuth;
    private final Set<String> warnedTargets = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private HttpServer server;
    private ExecutorService executor;
    private long readMillis = RequestGuard.READ_MILLIS;
    private long idleMillis = RequestGuard.IDLE_MILLIS;
    private RequestGuard guard;

    public PanelServer(Router router, SessionManager sessions, AccountStore accounts, SessionCookie cookies,
                       boolean hsts, Logger logger) {
        this(router, sessions, accounts, cookies, hsts, logger, null);
    }

    public PanelServer(Router router, SessionManager sessions, AccountStore accounts, SessionCookie cookies,
                       boolean hsts, Logger logger, NodeAuth.Verifier nodeAuth) {
        this.nodeAuth = nodeAuth;
        this.router = router;
        this.sessions = sessions;
        this.accounts = accounts;
        this.cookies = cookies;
        this.hsts = hsts;
        this.logger = logger;
    }

    public void start(InetSocketAddress address, SSLContext tls) throws IOException {
        start(address, tls, THREADS);
    }

    public void start(InetSocketAddress address, SSLContext tls, int threads) throws IOException {
        HttpServer created;
        if (tls == null) {
            created = HttpServer.create(address, 64);
        } else {
            HttpsServer https = HttpsServer.create(address, 64);
            https.setHttpsConfigurator(new HttpsConfigurator(tls));
            created = https;
        }
        AtomicInteger counter = new AtomicInteger();
        executor = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "NewBedwars-Panel-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        guard = new RequestGuard(readMillis, idleMillis);
        guard.start();
        created.setExecutor(guard.wrap(executor));
        created.createContext("/", this);
        created.start();
        server = created;
    }

    public void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        if (guard != null) guard.stop();
        server = null;
        executor = null;
        guard = null;
    }

    void limits(long readMillis, long idleMillis) {
        this.readMillis = readMillis;
        this.idleMillis = idleMillis;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void handle(HttpExchange exchange) {
        RequestGuard.Slot slot = guard == null ? RequestGuard.unguarded() : guard.current();
        try {
            Response response;
            try {
                response = dispatch(exchange, slot);
            } catch (HttpError e) {
                response = Response.error(e.status, e.code);
            } catch (Exception | StackOverflowError e) {
                if (slot.aborted()) return;
                logger.log(Level.WARNING, "Panel request " + exchange.getRequestMethod() + " "
                        + exchange.getRequestURI().getRawPath() + " failed", e);
                response = Response.error(500, "internal");
            }
            slot.finishing();
            if (slot.aborted()) return;
            write(exchange, response);
        } catch (IOException ignored) {
        } catch (Error e) {
            logger.log(Level.SEVERE, "Panel request " + exchange.getRequestMethod() + " "
                    + exchange.getRequestURI().getRawPath() + " failed", e);
            throw e;
        } finally {
            exchange.close();
        }
    }

    private Response dispatch(HttpExchange exchange, RequestGuard.Slot slot) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        String path = exchange.getRequestURI().getRawPath();
        boolean streaming = isUpload(method, path);
        byte[] body = null;
        if (streaming) {
            slot.streaming();
        } else {
            body = exchange.getRequestBody().readNBytes(Request.MAX_JSON_BYTES + 1);
            if (body.length > Request.MAX_JSON_BYTES) throw new HttpError(413, "too_large");
            if (!slot.done()) throw new IOException("request read timed out");
        }
        if (!path.startsWith("/api/")) return staticFile(method, path);

        Router.Match match = router.match(method, path);
        if (match == null) return Response.error(404, "not_found");
        Request request = new Request(exchange, match.params(), cookies.clientIp(exchange));
        if (streaming) request.stream = RequestGuard.track(exchange.getRequestBody(), slot);
        else request.buffered = body;
        boolean safe = method.equals("GET") || method.equals("HEAD");
        if (!safe && !cookies.sameOrigin(exchange)) throw new HttpError(403, "bad_origin");

        Router.Route route = match.route();
        if (request.header(NodeAuth.SIGNATURE) != null) {
            authenticateNode(request, method, path, route);
            return route.handler().handle(request);
        }

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

    private void authenticateNode(Request request, String method, String path, Router.Route route) throws IOException {
        if (nodeAuth == null) throw new HttpError(403, "not_a_node");
        NodeAuth.Verdict verdict = nodeAuth.verify(method, path, request.rawQuery(), request::header);
        if (!verdict.ok()) {
            if (verdict.failure() == NodeAuth.Failure.TARGET && warnedTargets.size() < 16 && warnedTargets.add(verdict.body())) {
                logger.warning("The gateway panel calls this server \"" + verdict.body() + "\" but its Network.Id is \""
                        + nodeAuth.selfId() + "\". Use the same name in Network.Peers on the gateway and in Network.Id here.");
            }
            throw new HttpError(401, "node_auth");
        }
        boolean unsigned = NodeAuth.UNSIGNED.equals(verdict.body());
        if (unsigned) {
            if (!(method.equals("POST") && path.equals("/api/import"))) throw new HttpError(401, "node_auth");
        } else {
            byte[] bytes = request.body().readNBytes(Request.MAX_JSON_BYTES + 1);
            if (bytes.length > Request.MAX_JSON_BYTES) throw new HttpError(413, "too_large");
            if (!MessageDigest.isEqual(NodeAuth.sha256(bytes).getBytes(StandardCharsets.UTF_8), verdict.body().getBytes(StandardCharsets.UTF_8))) {
                throw new HttpError(401, "node_auth");
            }
            request.buffered = bytes;
        }
        if (route.access() != Router.Access.PERMISSION
                || route.permissions().stream().anyMatch(Permission.LOCAL_ONLY::contains)) {
            throw new HttpError(403, "node_forbidden");
        }
        NodeAuth.Identity identity = verdict.identity();
        Role role = new Role("node", identity.permissions(), false);
        if (route.permissions().stream().noneMatch(role::has)) throw new HttpError(403, "forbidden");
        if (!identity.ip().isEmpty()) request.ip = identity.ip();
        request.account = new Account(identity.account(), "", "node", false, 0);
        request.role = role;
    }

    static boolean isUpload(String method, String path) {
        if (!method.equals("POST")) return false;
        if (path.equals("/api/import")) return true;
        if (!path.startsWith("/api/s/") || !path.endsWith("/import")) return false;
        String server = path.substring("/api/s/".length(), path.length() - "/import".length());
        return !server.isEmpty() && server.indexOf('/') < 0;
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
        if (response.stream != null) {
            try (java.io.InputStream in = response.stream) {
                if (head) {
                    exchange.sendResponseHeaders(response.status, -1);
                    return;
                }
                exchange.sendResponseHeaders(response.status, response.streamLength >= 0 ? response.streamLength : 0);
                try (OutputStream out = exchange.getResponseBody()) {
                    in.transferTo(out);
                }
            }
            return;
        }
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
