package com.l299l.newbedwars.panel.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.panel.auth.Permission;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NetworkService {
    public enum Status { ONLINE, OFFLINE, UNTRUSTED, REJECTED, NOT_CONNECTED }

    public record PeerStatus(String id, String url, Status status, String pinSource, long updated) {}

    private static final long FRESH_MILLIS = 1500;
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(4);
    private static final long OVERVIEW_WAIT_MILLIS = 1500;
    private static final int MAX_BODY = 4 * 1024 * 1024;

    private static final class State {
        final PeerClient client;
        volatile Status status = Status.OFFLINE;
        volatile boolean contacted;
        volatile JsonObject data;
        volatile long rev = -1;
        volatile long updated;
        volatile long fetchedAt;
        CompletableFuture<Void> inflight;

        State(PeerClient client) {
            this.client = client;
        }
    }

    private final String selfId;
    private final Map<String, State> peers = new LinkedHashMap<>();
    private final LongSupplier clock;
    private final Logger logger;
    private final ExecutorService executor;
    private final NodeAuth.Identity monitor;

    public NetworkService(String selfId, List<PeerClient> clients, LongSupplier clock, Logger logger) {
        this.selfId = selfId;
        this.clock = clock;
        this.logger = logger;
        for (PeerClient client : clients) peers.put(client.peer().id(), new State(client));
        this.monitor = new NodeAuth.Identity("gateway", Set.of(Permission.DASHBOARD_VIEW), "", selfId);
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "NewBedwars-Panel-Peers-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public String selfId() {
        return selfId;
    }

    public PeerClient client(String id) {
        State state = peers.get(id);
        return state == null ? null : state.client;
    }

    public void stop() {
        executor.shutdownNow();
    }

    public List<PeerStatus> statuses() {
        List<PeerStatus> list = new ArrayList<>();
        for (State s : peers.values()) {
            list.add(new PeerStatus(s.client.peer().id(), s.client.peer().url(), s.contacted ? s.status : null,
                    s.client.pinSource(), s.updated));
        }
        return list;
    }

    private CompletableFuture<Void> refresh(State state) {
        synchronized (state) {
            if (clock.getAsLong() - state.fetchedAt < FRESH_MILLIS) return CompletableFuture.completedFuture(null);
            if (state.inflight != null && !state.inflight.isDone()) return state.inflight;
            state.inflight = CompletableFuture.runAsync(() -> fetch(state), executor);
            return state.inflight;
        }
    }

    private void fetch(State state) {
        try {
            String query = state.rev >= 0 && state.data != null ? "rev=" + state.rev : "";
            PeerClient.Reply reply = state.client.send("GET", "/api/dashboard", query, monitor,
                    NodeAuth.sha256(new byte[0]), new PeerClient.Payload.None(), FETCH_TIMEOUT);
            try (InputStream body = reply.body()) {
                state.contacted = true;
                if (reply.status() == 204 && state.data != null) {
                    state.status = Status.ONLINE;
                    state.updated = clock.getAsLong();
                } else if (reply.status() == 200) {
                    JsonObject parsed = parse(body);
                    if (parsed != null && parsed.has("data") && parsed.get("data").isJsonObject()) {
                        state.data = parsed.getAsJsonObject("data");
                        state.rev = parsed.has("rev") ? parsed.get("rev").getAsLong() : -1;
                        state.status = Status.ONLINE;
                        state.updated = clock.getAsLong();
                    } else {
                        state.status = Status.OFFLINE;
                    }
                } else if (reply.status() == 401 || reply.status() == 403) {
                    state.status = Status.REJECTED;
                } else {
                    state.status = Status.OFFLINE;
                }
            }
        } catch (PeerException e) {
            state.contacted = true;
            state.status = e.kind() == PeerException.Kind.UNTRUSTED ? Status.UNTRUSTED : Status.OFFLINE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            state.contacted = true;
            state.status = Status.OFFLINE;
            logger.log(Level.FINE, "Panel peer " + state.client.peer().id() + " answered badly", e);
        } finally {
            state.fetchedAt = clock.getAsLong();
        }
    }

    private static JsonObject parse(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) return null;
        try {
            JsonElement element = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public JsonObject overview(JsonObject local) {
        List<CompletableFuture<Void>> pending = new ArrayList<>();
        for (State state : peers.values()) pending.add(refresh(state));
        try {
            CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).get(OVERVIEW_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
        JsonArray servers = new JsonArray();
        JsonObject self = new JsonObject();
        self.addProperty("id", selfId);
        self.addProperty("local", true);
        self.addProperty("status", Status.ONLINE.name());
        self.add("data", local);
        servers.add(self);
        for (State state : peers.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", state.client.peer().id());
            o.addProperty("local", false);
            o.addProperty("url", state.client.peer().url());
            o.addProperty("pin", state.client.pinSource());
            Status status = state.status;
            JsonObject data = state.data;
            o.addProperty("status", status.name());
            if (data != null) {
                o.add("data", data);
                o.addProperty("updated", state.updated);
            }
            servers.add(o);
        }
        Set<String> unknown = new LinkedHashSet<>();
        JsonArray network = local != null && local.has("network") && local.get("network").isJsonArray()
                ? local.getAsJsonArray("network") : new JsonArray();
        for (JsonElement e : network) {
            String server = e.getAsJsonObject().get("server").getAsString();
            if (!server.equals(selfId) && !peers.containsKey(server)) unknown.add(server);
        }
        for (String server : unknown) {
            JsonObject o = new JsonObject();
            o.addProperty("id", server);
            o.addProperty("local", false);
            o.addProperty("status", Status.NOT_CONNECTED.name());
            JsonArray arenas = new JsonArray();
            for (JsonElement e : network) if (e.getAsJsonObject().get("server").getAsString().equals(server)) arenas.add(e);
            o.add("networkArenas", arenas);
            servers.add(o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("self", selfId);
        root.add("servers", servers);
        return root;
    }
}
