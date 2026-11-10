package com.l299l.newbedwars.common.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ModrinthSource implements UpdateSource {
    public static final String PROJECT_PAGE = "https://modrinth.com/plugin/new-bedwars";
    private static final String API = "https://api.modrinth.com/v2/project/new-bedwars/version";

    private final Fetcher fetcher;

    public ModrinthSource(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String name() {
        return "Modrinth";
    }

    @Override
    public List<ReleaseInfo> releases(Product product) throws IOException {
        String loaders = URLEncoder.encode("[\"" + product.loader() + "\"]", StandardCharsets.UTF_8);
        return parse(fetcher.get(API + "?loaders=" + loaders), product);
    }

    static List<ReleaseInfo> parse(String json, Product product) throws IOException {
        JsonArray versions = Json.array(json, "Modrinth");
        List<ReleaseInfo> out = new ArrayList<>();
        for (JsonElement element : versions) {
            if (!element.isJsonObject()) continue;
            JsonObject version = element.getAsJsonObject();
            if (!Json.contains(version.get("loaders"), product.loader())) continue;
            PluginVersion number = PluginVersion.parse(Json.string(version, "version_number"));
            if (number == null) continue;
            JsonObject file = jarFile(version.get("files"), product);
            if (file == null) continue;
            Channel channel = Channel.max(Channel.parse(Json.string(version, "version_type"), Channel.RELEASE), number.channel());
            JsonObject hashes = file.has("hashes") && file.get("hashes").isJsonObject() ? file.getAsJsonObject("hashes") : new JsonObject();
            String sha512 = Json.string(hashes, "sha512");
            out.add(new ReleaseInfo(number, channel, PROJECT_PAGE + "/version/" + Json.string(version, "id"),
                    Json.string(file, "url"), Json.string(file, "filename"),
                    sha512 == null ? null : "SHA-512", sha512, lowestGameVersion(version.get("game_versions")), "Modrinth"));
        }
        return out;
    }

    private static JsonObject jarFile(JsonElement files, Product product) {
        if (files == null || !files.isJsonArray()) return null;
        JsonObject first = null;
        for (JsonElement element : files.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject file = element.getAsJsonObject();
            if (!product.isJar(Json.string(file, "filename")) || Json.string(file, "url") == null) continue;
            if (Json.bool(file, "primary")) return file;
            if (first == null) first = file;
        }
        return first;
    }

    private static PluginVersion lowestGameVersion(JsonElement gameVersions) {
        if (gameVersions == null || !gameVersions.isJsonArray()) return null;
        PluginVersion lowest = null;
        for (JsonElement element : gameVersions.getAsJsonArray()) {
            if (!element.isJsonPrimitive()) continue;
            String raw = element.getAsString();
            if (!raw.matches("\\d+(\\.\\d+)+")) continue;
            PluginVersion v = PluginVersion.parse(raw);
            if (v != null && (lowest == null || v.compareTo(lowest) < 0)) lowest = v;
        }
        return lowest;
    }
}
