package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.common.update.ReleaseInfo;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.update.UpdateNotifier;
import com.l299l.newbedwars.update.UpdateService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class UpdateCommand extends BwCommand {
    public static final String PERMISSION = "newbedwars.bw.update";

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public String getDescription() {
        return "Check for a new NewBedwars version and download it.";
    }

    @Override
    public String getSyntax() {
        return "/bw update [check|download]";
    }

    @Override
    public String getExample() {
        return "/bw update download";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission(PERMISSION) && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        if (!Properties.Updates.check()) {
            msg.send(player, "UpdateChecksDisabled");
            return;
        }
        String action = args.length >= 2 ? args[1].toLowerCase() : "check";
        if (!action.equals("check") && !action.equals("download")) {
            usage(player);
            return;
        }
        boolean download = action.equals("download");
        UpdateService service = NewBedwars.plugin.getUpdateService();
        msg.send(player, "UpdateChecking");
        service.checkNow().whenComplete((result, error) -> sync(() -> {
            if (!player.isOnline()) return;
            if (error != null || result.failed()) {
                msg.send(player, "UpdateCheckFailed", Map.of("/error/", error != null ? reason(error) : result.error()));
                return;
            }
            if (!result.newer()) {
                msg.send(player, "UpdateNone", Map.of("/current/", NewBedwars.plugin.getDescription().getVersion()));
                return;
            }
            ReleaseInfo release = result.latest();
            if (!download || service.isDownloaded(release)) {
                UpdateNotifier.tell(player, release, service);
                if (!download) msg.send(player, "UpdateDownloadHint");
                return;
            }
            startDownload(player, service, release);
        }));
    }

    private void startDownload(Player player, UpdateService service, ReleaseInfo release) {
        Map<String, String> r = Map.of("/latest/", release.version().toString());
        msg.send(player, "UpdateDownloading", r);
        service.downloadNow(release).whenComplete((path, error) -> sync(() -> {
            if (!player.isOnline()) return;
            if (error == null) {
                msg.send(player, "UpdateDownloaded", r);
            } else if (unwrap(error) instanceof UpdateService.IncompatibleException) {
                msg.send(player, "UpdateIncompatible", Map.of("/latest/", release.version().toString(),
                        "/mc/", String.valueOf(release.minGameVersion())));
            } else {
                msg.send(player, "UpdateDownloadFailed", Map.of("/error/", reason(error)));
            }
        }));
    }

    private static void sync(Runnable task) {
        Bukkit.getScheduler().runTask(NewBedwars.plugin, task);
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    private static String reason(Throwable error) {
        Throwable cause = unwrap(error);
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return args.length == 2 ? List.of("check", "download") : null;
    }
}
