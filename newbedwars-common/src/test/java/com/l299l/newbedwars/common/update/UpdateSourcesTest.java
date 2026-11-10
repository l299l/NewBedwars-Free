package com.l299l.newbedwars.common.update;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateSourcesTest {

    static final String MODRINTH = """
            [
              {"id": "proxy27", "version_number": "2.7-beta-proxy", "version_type": "beta", "loaders": ["velocity"],
               "game_versions": ["1.17", "26.3"],
               "files": [{"url": "https://cdn.modrinth.com/p.jar", "filename": "NewBedwars-Proxy-2.7-beta.jar", "primary": true,
                          "hashes": {"sha512": "aa", "sha1": "bb"}}]},
              {"id": "plugin27", "version_number": "2.7-beta", "version_type": "beta", "loaders": ["paper", "spigot"],
               "game_versions": ["26.3", "1.18", "1.17.1", "1.17", "26.1-snapshot-1"],
               "files": [{"url": "https://cdn.modrinth.com/s.jar", "filename": "NewBedwars-2.7-beta-sources.jar", "primary": false,
                          "hashes": {"sha512": "00"}},
                         {"url": "https://cdn.modrinth.com/n.jar", "filename": "NewBedwars-2.7-beta.jar", "primary": true,
                          "hashes": {"sha512": "abc123", "sha1": "ff"}}]},
              {"id": "plugin30a", "version_number": "3.0-alpha", "version_type": "alpha", "loaders": ["paper"],
               "game_versions": ["1.21"],
               "files": [{"url": "https://cdn.modrinth.com/a.jar", "filename": "NewBedwars-3.0-alpha.jar", "primary": true,
                          "hashes": {"sha512": "dd"}}]},
              {"id": "plugin26", "version_number": "2.6-beta", "version_type": "beta", "loaders": ["paper"],
               "game_versions": ["1.17"],
               "files": [{"url": "https://cdn.modrinth.com/o.jar", "filename": "NewBedwars-2.6-beta.jar", "primary": true,
                          "hashes": {"sha512": "ee"}}]},
              {"id": "weird", "version_number": "nightly", "version_type": "beta", "loaders": ["paper"], "files": []}
            ]""";

    static final String GITHUB = """
            [
              {"tag_name": "v2.8-beta", "draft": true, "prerelease": false, "html_url": "https://github.com/x/draft",
               "assets": [{"name": "NewBedwars-2.8-beta.jar", "browser_download_url": "https://github.com/d.jar", "digest": "sha256:11"}]},
              {"tag_name": "v2.7-beta", "draft": false, "prerelease": false, "html_url": "https://github.com/x/2.7",
               "assets": [{"name": "NewBedwars-2.7-beta.jar", "browser_download_url": "https://github.com/n.jar", "digest": "sha256:cafe"},
                          {"name": "NewBedwars-Proxy-2.7-beta.jar", "browser_download_url": "https://github.com/p.jar"}]},
              {"tag_name": "v2.7", "draft": false, "prerelease": true, "html_url": "https://github.com/x/pre",
               "assets": [{"name": "NewBedwars-2.7.jar", "browser_download_url": "https://github.com/r.jar"}]}
            ]""";

    @Test
    void modrinthPicksThePluginJarWithHashAndLowestGameVersion() throws IOException {
        List<ReleaseInfo> releases = ModrinthSource.parse(MODRINTH, Product.PLUGIN);
        assertEquals(3, releases.size());
        ReleaseInfo r = releases.get(0);
        assertEquals(PluginVersion.parse("2.7-beta"), r.version());
        assertEquals(Channel.BETA, r.channel());
        assertEquals("https://cdn.modrinth.com/n.jar", r.downloadUrl());
        assertEquals("NewBedwars-2.7-beta.jar", r.fileName());
        assertEquals("SHA-512", r.hashAlgorithm());
        assertEquals("abc123", r.hash());
        assertEquals(PluginVersion.parse("1.17"), r.minGameVersion());
        assertEquals("https://modrinth.com/plugin/new-bedwars/version/plugin27", r.pageUrl());
        assertEquals(Channel.ALPHA, releases.get(1).channel());
    }

    @Test
    void modrinthProxyOnlyTakesTheProxyJar() throws IOException {
        List<ReleaseInfo> releases = ModrinthSource.parse(MODRINTH, Product.PROXY);
        assertEquals(1, releases.size());
        assertEquals("NewBedwars-Proxy-2.7-beta.jar", releases.get(0).fileName());
        assertEquals(PluginVersion.parse("2.7-beta"), releases.get(0).version());
    }

    @Test
    void gitHubSkipsDraftsAndReadsTheDigest() throws IOException {
        List<ReleaseInfo> releases = GitHubSource.parse(GITHUB, Product.PLUGIN);
        assertEquals(2, releases.size());
        ReleaseInfo r = releases.get(0);
        assertEquals(PluginVersion.parse("2.7-beta"), r.version());
        assertEquals("SHA-256", r.hashAlgorithm());
        assertEquals("cafe", r.hash());
        assertNull(r.minGameVersion());
        assertEquals(Channel.BETA, releases.get(1).channel());
        assertFalse(releases.get(1).hasChecksum());

        List<ReleaseInfo> proxy = GitHubSource.parse(GITHUB, Product.PROXY);
        assertEquals(1, proxy.size());
        assertFalse(proxy.get(0).hasChecksum());
    }

    @Test
    void jarNamesArePickedPerProduct() {
        assertTrue(Product.PLUGIN.isJar("NewBedwars-2.7-beta.jar"));
        assertFalse(Product.PLUGIN.isJar("NewBedwars-Proxy-2.7-beta.jar"));
        assertFalse(Product.PLUGIN.isJar("NewBedwars-Panel-2.7-beta.jar"));
        assertFalse(Product.PLUGIN.isJar("NewBedwars-2.7-beta-sources.jar"));
        assertTrue(Product.PROXY.isJar("NewBedwars-Proxy-2.7-beta.jar"));
        assertFalse(Product.PROXY.isJar("NewBedwars-Panel-2.7-beta.jar"));
    }

    @Test
    void unreadableAnswersThrow() {
        assertThrows(IOException.class, () -> ModrinthSource.parse("<html>", Product.PLUGIN));
        assertThrows(IOException.class, () -> GitHubSource.parse("{\"message\":\"rate limit\"}", Product.PLUGIN));
    }

    @Test
    void checkerFiltersByChannelAndComparesWithCurrent() {
        UpdateChecker checker = new UpdateChecker(new FakeFetcher(MODRINTH, GITHUB));
        UpdateChecker.Result beta = checker.check(Product.PLUGIN, PluginVersion.parse("2.6-beta"), Channel.BETA);
        assertTrue(beta.newer());
        assertEquals(PluginVersion.parse("2.7-beta"), beta.latest().version());

        UpdateChecker.Result alpha = checker.check(Product.PLUGIN, PluginVersion.parse("2.6-beta"), Channel.ALPHA);
        assertEquals(PluginVersion.parse("3.0-alpha"), alpha.latest().version());

        UpdateChecker.Result release = checker.check(Product.PLUGIN, PluginVersion.parse("2.6-beta"), Channel.RELEASE);
        assertNull(release.latest());
        assertFalse(release.newer());

        UpdateChecker.Result upToDate = checker.check(Product.PLUGIN, PluginVersion.parse("2.7-beta"), Channel.BETA);
        assertFalse(upToDate.newer());
        assertFalse(upToDate.failed());

        assertFalse(checker.check(Product.PLUGIN, null, Channel.BETA).newer());
    }

    @Test
    void checkerFallsBackToGitHubAndReportsBothErrors() {
        FakeFetcher fetcher = new FakeFetcher(null, GITHUB);
        UpdateChecker.Result fallback = new UpdateChecker(fetcher).check(Product.PLUGIN, PluginVersion.parse("2.6-beta"), Channel.BETA);
        assertEquals("GitHub", fallback.latest().source());
        assertTrue(fallback.newer());
        assertTrue(fetcher.urls.get(0).startsWith("https://api.modrinth.com/v2/project/new-bedwars/version?loaders="));
        assertTrue(fetcher.urls.get(0).contains("paper"));

        UpdateChecker.Result failed = new UpdateChecker(new FakeFetcher(null, null))
                .check(Product.PLUGIN, PluginVersion.parse("2.6-beta"), Channel.BETA);
        assertTrue(failed.failed());
        assertTrue(failed.error().contains("Modrinth") && failed.error().contains("GitHub"));
    }

    static final class FakeFetcher implements Fetcher {
        final String modrinth;
        final String github;
        final List<String> urls = new ArrayList<>();

        FakeFetcher(String modrinth, String github) {
            this.modrinth = modrinth;
            this.github = github;
        }

        @Override
        public String get(String url) throws IOException {
            urls.add(url);
            String body = url.contains("modrinth") ? modrinth : github;
            if (body == null) throw new IOException("HTTP 503");
            return body;
        }

        @Override
        public InputStream open(String url) throws IOException {
            throw new IOException("not used");
        }
    }
}
