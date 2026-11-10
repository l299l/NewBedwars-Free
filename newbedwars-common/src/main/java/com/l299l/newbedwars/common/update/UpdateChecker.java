package com.l299l.newbedwars.common.update;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class UpdateChecker {
    private final List<UpdateSource> sources;

    public UpdateChecker(Fetcher fetcher) {
        this(List.of(new ModrinthSource(fetcher), new GitHubSource(fetcher)));
    }

    public UpdateChecker(List<UpdateSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public static String userAgent(String product, String version) {
        return product + "/" + version + " (github.com/l299l/NewBedwars-Free)";
    }

    public Result check(Product product, PluginVersion current, Channel channel) {
        List<String> errors = new ArrayList<>();
        for (UpdateSource source : sources) {
            try {
                ReleaseInfo newest = source.releases(product).stream()
                        .filter(r -> channel.allows(r.channel()))
                        .max(Comparator.comparing(ReleaseInfo::version))
                        .orElse(null);
                return new Result(newest, newest != null && newest.version().isNewerThan(current), null);
            } catch (Exception e) {
                errors.add(source.name() + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
        return new Result(null, false, String.join("; ", errors));
    }

    public record Result(ReleaseInfo latest, boolean newer, String error) {
        public boolean failed() {
            return error != null;
        }
    }
}
