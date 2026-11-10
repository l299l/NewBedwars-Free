package com.l299l.newbedwars.update;

import com.l299l.newbedwars.common.update.Channel;

import java.util.function.Function;

public record UpdateSettings(boolean check, boolean notifyAdmins, boolean autoDownload, Channel channel, int intervalHours) {

    public static final UpdateSettings DEFAULTS = new UpdateSettings(true, true, false, Channel.BETA, 12);

    public static UpdateSettings read(Function<String, Object> get) {
        Object interval = get.apply("Updates.CheckIntervalHours");
        return new UpdateSettings(
                bool(get.apply("Updates.Check"), DEFAULTS.check),
                bool(get.apply("Updates.Notify"), DEFAULTS.notifyAdmins),
                bool(get.apply("Updates.AutoDownload"), DEFAULTS.autoDownload),
                get.apply("Updates.Channel") instanceof String s ? Channel.parse(s, DEFAULTS.channel) : DEFAULTS.channel,
                interval instanceof Number n && n.intValue() >= 1 ? n.intValue() : DEFAULTS.intervalHours);
    }

    private static boolean bool(Object raw, boolean fallback) {
        return raw instanceof Boolean b ? b : fallback;
    }
}
