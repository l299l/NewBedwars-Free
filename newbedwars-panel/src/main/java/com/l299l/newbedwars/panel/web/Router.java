package com.l299l.newbedwars.panel.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Router {
    public enum Access { PUBLIC, LOGGED_IN, PERMISSION }

    public interface Handler {
        Response handle(Request request) throws Exception;
    }

    public record Route(String method, String pattern, String[] segments, Access access, Set<String> permissions, Handler handler) {}

    public record Match(Route route, Map<String, String> params) {}

    private final List<Route> routes = new ArrayList<>();

    public void add(String method, String pattern, Access access, Set<String> anyOf, Handler handler) {
        routes.add(new Route(method, pattern, split(pattern), access, anyOf, handler));
    }

    public void open(String method, String pattern, Handler handler) {
        add(method, pattern, Access.PUBLIC, Set.of(), handler);
    }

    public void loggedIn(String method, String pattern, Handler handler) {
        add(method, pattern, Access.LOGGED_IN, Set.of(), handler);
    }

    public void allowed(String method, String pattern, Set<String> anyOf, Handler handler) {
        add(method, pattern, Access.PERMISSION, anyOf, handler);
    }

    public List<Route> routes() {
        return List.copyOf(routes);
    }

    public Match match(String method, String path) {
        String[] parts = split(path);
        boolean pathKnown = false;
        for (Route route : routes) {
            Map<String, String> params = matchPath(route.segments(), parts);
            if (params == null) continue;
            pathKnown = true;
            if (route.method().equals(method)) return new Match(route, params);
        }
        if (pathKnown) throw new HttpError(405, "method_not_allowed");
        return null;
    }

    private static Map<String, String> matchPath(String[] pattern, String[] parts) {
        if (pattern.length != parts.length) return null;
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < pattern.length; i++) {
            String p = pattern[i];
            if (p.startsWith("{") && p.endsWith("}")) {
                if (parts[i].isEmpty()) return null;
                params.put(p.substring(1, p.length() - 1), decode(parts[i]));
            } else if (!p.equals(parts[i])) {
                return null;
            }
        }
        return params;
    }

    private static String decode(String segment) {
        try {
            return URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new HttpError(400, "bad_path");
        }
    }

    private static String[] split(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed.isEmpty() ? new String[0] : trimmed.split("/", -1);
    }
}
