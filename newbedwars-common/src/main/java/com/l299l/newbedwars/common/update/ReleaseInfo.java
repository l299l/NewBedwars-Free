package com.l299l.newbedwars.common.update;

public record ReleaseInfo(PluginVersion version, Channel channel, String pageUrl, String downloadUrl, String fileName,
                          String hashAlgorithm, String hash, PluginVersion minGameVersion, String source) {

    public boolean hasChecksum() {
        return hashAlgorithm != null && hash != null && !hash.isEmpty();
    }

    public boolean supportsGameVersion(PluginVersion server) {
        return minGameVersion == null || server == null || server.compareTo(minGameVersion) >= 0;
    }
}
