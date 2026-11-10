package com.l299l.newbedwars.common.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GitHubSource implements UpdateSource {
    private static final String API = "https://api.github.com/repos/l299l/NewBedwars-Free/releases?per_page=10";

    private final Fetcher fetcher;

    public GitHubSource(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String name() {
        return "GitHub";
    }

    @Override
    public List<ReleaseInfo> releases(Product product) throws IOException {
        return parse(fetcher.get(API), product);
    }

    static List<ReleaseInfo> parse(String json, Product product) throws IOException {
        JsonArray releases = Json.array(json, "GitHub");
        List<ReleaseInfo> out = new ArrayList<>();
        for (JsonElement element : releases) {
            if (!element.isJsonObject()) continue;
            JsonObject release = element.getAsJsonObject();
            if (Json.bool(release, "draft")) continue;
            PluginVersion number = PluginVersion.parse(Json.string(release, "tag_name"));
            if (number == null) continue;
            JsonObject asset = jarAsset(release.get("assets"), product);
            if (asset == null) continue;
            boolean prerelease = Json.bool(release, "prerelease");
            Channel channel = Channel.max(number.channel(), prerelease ? Channel.BETA : Channel.RELEASE);
            String digest = Json.string(asset, "digest");
            String algorithm = null;
            String hash = null;
            if (digest != null && digest.toLowerCase(Locale.ROOT).startsWith("sha256:")) {
                algorithm = "SHA-256";
                hash = digest.substring("sha256:".length());
            }
            out.add(new ReleaseInfo(number, channel, Json.string(release, "html_url"),
                    Json.string(asset, "browser_download_url"), Json.string(asset, "name"),
                    algorithm, hash, null, "GitHub"));
        }
        return out;
    }

    private static JsonObject jarAsset(JsonElement assets, Product product) {
        if (assets == null || !assets.isJsonArray()) return null;
        for (JsonElement element : assets.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject asset = element.getAsJsonObject();
            if (product.isJar(Json.string(asset, "name")) && Json.string(asset, "browser_download_url") != null) return asset;
        }
        return null;
    }
}
