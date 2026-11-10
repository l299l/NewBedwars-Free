package com.l299l.newbedwars.panel.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Request {
    public static final int MAX_JSON_BYTES = 64 * 1024;

    final HttpExchange exchange;
    final Map<String, String> params;
    final String ip;
    SessionManager.Session session;
    Account account;
    Role role;
    private Map<String, String> query;
    private JsonObject json;

    Request(HttpExchange exchange, Map<String, String> params, String ip) {
        this.exchange = exchange;
        this.params = params;
        this.ip = ip;
    }

    public String param(String name) {
        return params.get(name);
    }

    public String ip() {
        return ip;
    }

    public SessionManager.Session session() {
        return session;
    }

    public Account account() {
        return account;
    }

    public Role role() {
        return role;
    }

    public String user() {
        return account == null ? null : account.name();
    }

    public String header(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    public String query(String name) {
        if (query == null) {
            query = new HashMap<>();
            String raw = exchange.getRequestURI().getRawQuery();
            if (raw != null) {
                for (String pair : raw.split("&")) {
                    int eq = pair.indexOf('=');
                    String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
                    String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    query.putIfAbsent(key, value);
                }
            }
        }
        return query.get(name);
    }

    public InputStream body() {
        return exchange.getRequestBody();
    }

    public JsonObject json() throws IOException {
        if (json != null) return json;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        try (InputStream in = exchange.getRequestBody()) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                if (out.size() + n > MAX_JSON_BYTES) throw new HttpError(413, "too_large");
                out.write(buffer, 0, n);
            }
        }
        String text = out.toString(StandardCharsets.UTF_8);
        if (text.isBlank()) {
            json = new JsonObject();
            return json;
        }
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) throw new HttpError(400, "bad_json");
            json = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            if (e instanceof HttpError h) throw h;
            throw new HttpError(400, "bad_json");
        }
        return json;
    }

    public String string(String key) throws IOException {
        JsonElement e = json().get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive()) return null;
        return e.getAsString();
    }

    public String required(String key) throws IOException {
        String value = string(key);
        if (value == null || value.isEmpty()) throw new HttpError(400, "missing_" + key);
        return value;
    }
}
