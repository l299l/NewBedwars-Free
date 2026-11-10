package com.l299l.newbedwars.panel.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Response {
    final int status;
    final String contentType;
    final byte[] body;
    final Path file;
    final Map<String, String> headers = new LinkedHashMap<>();

    private Response(int status, String contentType, byte[] body, Path file) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        this.file = file;
    }

    public static Response json(int status, JsonElement json) {
        return new Response(status, "application/json; charset=utf-8", json.toString().getBytes(StandardCharsets.UTF_8), null);
    }

    public static Response ok(JsonElement json) {
        return json(200, json);
    }

    public static Response ok() {
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        return ok(o);
    }

    public static Response error(int status, String code) {
        JsonObject o = new JsonObject();
        o.addProperty("error", code);
        return json(status, o);
    }

    public static Response error(int status, String code, String detail) {
        JsonObject o = new JsonObject();
        o.addProperty("error", code);
        if (detail != null) o.addProperty("detail", detail);
        return json(status, o);
    }

    public static Response noContent() {
        return new Response(204, null, new byte[0], null);
    }

    public static Response bytes(String contentType, byte[] body) {
        return new Response(200, contentType, body, null);
    }

    public static Response download(Path file, String downloadName) {
        Response r = new Response(200, "application/zip", null, file);
        r.headers.put("Content-Disposition", "attachment; filename=\"" + downloadName.replace("\"", "") + "\"");
        return r;
    }

    public Response header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public int status() {
        return status;
    }

    public String bodyText() {
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }
}
