package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonObject;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.network.NetworkService;
import com.l299l.newbedwars.panel.network.NodeAuth;
import com.l299l.newbedwars.panel.network.PeerClient;
import com.l299l.newbedwars.panel.network.PeerException;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

public final class NetworkRoutes {
    private static final Duration FORWARD_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration IMPORT_TIMEOUT = Duration.ofMinutes(10);
    private static final Set<String> FORWARDED_TYPES = Set.of("application/json", "application/zip", "application/octet-stream");
    private static final String IMPORT_PATH = "/api/import";

    public interface LocalData {
        JsonObject get() throws Exception;
    }

    private final Router router;
    private final NetworkService network;
    private final LocalData local;

    public NetworkRoutes(Router router, NetworkService network, LocalData local) {
        this.router = router;
        this.network = network;
        this.local = local;
    }

    public void register(Router router) {
        router.allowed("GET", "/api/network", Set.of(Permission.NETWORK_SERVERS), r -> Response.ok(network.overview(local.get())));
        for (String method : new String[]{"GET", "POST", "DELETE"}) {
            router.loggedIn(method, "/api/s/{server}/{*rest}", this::forward);
        }
    }

    private Response forward(Request r) throws Exception {
        Role role = r.role();
        if (r.account().mustChangePassword()) throw new HttpError(403, "password_change_required");
        if (role == null || !role.has(Permission.NETWORK_SERVERS)) throw new HttpError(403, "forbidden");
        String server = r.param("server");
        if (server.equals(network.selfId())) throw new HttpError(400, "local_server");
        PeerClient client = network.client(server);
        if (client == null) throw new HttpError(404, "unknown_server");

        String method = r.method().toUpperCase();
        String innerPath = "/api/" + r.param("rest");
        Router.Match inner = router.match(method, innerPath);
        if (inner == null) throw new HttpError(404, "not_found");
        Router.Route route = inner.route();
        if (route.access() != Router.Access.PERMISSION
                || route.permissions().stream().anyMatch(Permission.LOCAL_ONLY::contains)) {
            throw new HttpError(403, "forbidden");
        }
        if (route.permissions().stream().noneMatch(role::has)) throw new HttpError(403, "forbidden");

        NodeAuth.Identity identity = new NodeAuth.Identity(r.account().name(), NodeAuth.forwardable(role.permissions()),
                r.ip(), network.selfId());
        PeerClient.Payload payload = new PeerClient.Payload.None();
        String bodyHash = NodeAuth.sha256(new byte[0]);
        if (!method.equals("GET")) {
            if (method.equals("POST") && innerPath.equals(IMPORT_PATH)) {
                long length = r.contentLength();
                payload = new PeerClient.Payload.Stream(r.body(), length, r.contentType());
                bodyHash = NodeAuth.UNSIGNED;
            } else {
                byte[] bytes = r.body().readNBytes(Request.MAX_JSON_BYTES + 1);
                if (bytes.length > Request.MAX_JSON_BYTES) throw new HttpError(413, "too_large");
                payload = bytes.length == 0 ? payload : new PeerClient.Payload.Bytes(bytes, r.contentType());
                bodyHash = NodeAuth.sha256(bytes);
            }
        }
        PeerClient.Reply reply;
        try {
            reply = client.send(method, innerPath, r.rawQuery(), identity, bodyHash, payload,
                    bodyHash.equals(NodeAuth.UNSIGNED) ? IMPORT_TIMEOUT : FORWARD_TIMEOUT);
        } catch (PeerException e) {
            throw switch (e.kind()) {
                case UNTRUSTED -> new HttpError(502, "server_untrusted");
                case TIMEOUT -> new HttpError(504, "timeout");
                case UNREACHABLE -> new HttpError(502, "server_unreachable");
            };
        }
        if (reply.status() == 401) {
            try {
                reply.body().close();
            } catch (IOException ignored) {
            }
            throw new HttpError(502, "server_rejected");
        }
        String type = reply.contentType() == null ? "" : reply.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean known = type.isEmpty() || FORWARDED_TYPES.contains(type);
        Response response = Response.stream(reply.status(), type.isEmpty() ? null : known ? reply.contentType() : "application/octet-stream",
                reply.body(), reply.length());
        if (!known) response.header("Content-Disposition", "attachment");
        else if (reply.disposition() != null) response.header("Content-Disposition", reply.disposition());
        return response;
    }
}
