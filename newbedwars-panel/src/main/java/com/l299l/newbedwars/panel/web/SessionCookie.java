package com.l299l.newbedwars.panel.web;

import com.sun.net.httpserver.HttpExchange;

import java.net.URI;
import java.util.List;
import java.util.Locale;

public final class SessionCookie {
    public static final String SECURE_NAME = "__Host-nbw";
    public static final String PLAIN_NAME = "nbw";

    private final boolean https;
    private final boolean trustProxyHeaders;

    public SessionCookie(boolean https, boolean trustProxyHeaders) {
        this.https = https;
        this.trustProxyHeaders = trustProxyHeaders;
    }

    boolean secure(HttpExchange exchange) {
        if (https) return true;
        if (!trustProxyHeaders) return false;
        String proto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        return proto != null && proto.trim().equalsIgnoreCase("https");
    }

    private String name(HttpExchange exchange) {
        return secure(exchange) ? SECURE_NAME : PLAIN_NAME;
    }

    String read(HttpExchange exchange) {
        String wanted = name(exchange);
        List<String> headers = exchange.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String header : headers) {
            for (String part : header.split(";")) {
                int eq = part.indexOf('=');
                if (eq > 0 && part.substring(0, eq).trim().equals(wanted)) return part.substring(eq + 1).trim();
            }
        }
        return null;
    }

    public Response attach(Response response, Request request, String sessionId) {
        return response.header("Set-Cookie", cookie(request.exchange, sessionId, -1));
    }

    public Response clear(Response response, Request request) {
        return response.header("Set-Cookie", cookie(request.exchange, "", 0));
    }

    private String cookie(HttpExchange exchange, String value, int maxAge) {
        StringBuilder c = new StringBuilder(name(exchange)).append('=').append(value).append("; Path=/; HttpOnly; SameSite=Strict");
        if (secure(exchange)) c.append("; Secure");
        if (maxAge >= 0) c.append("; Max-Age=").append(maxAge);
        return c.toString();
    }

    String clientIp(HttpExchange exchange) {
        String direct = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!trustProxyHeaders) return direct;
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return direct;
        String[] hops = forwarded.split(",");
        String last = hops[hops.length - 1].trim();
        return last.isEmpty() ? direct : last;
    }

    boolean sameOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null) return true;
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (trustProxyHeaders) {
            String forwardedHost = exchange.getRequestHeaders().getFirst("X-Forwarded-Host");
            if (forwardedHost != null && !forwardedHost.isBlank()) host = forwardedHost.split(",")[0].trim();
        }
        if (host == null) return false;
        try {
            URI uri = URI.create(origin);
            String scheme = secure(exchange) ? "https" : "http";
            if (!scheme.equalsIgnoreCase(uri.getScheme())) return false;
            String originHost = uri.getHost() + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
            return normalize(originHost, scheme).equals(normalize(host, scheme));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalize(String hostPort, String scheme) {
        String h = hostPort.toLowerCase(Locale.ROOT);
        String defaultPort = scheme.equals("https") ? ":443" : ":80";
        return h.endsWith(defaultPort) ? h.substring(0, h.length() - defaultPort.length()) : h;
    }
}
