package com.l299l.newbedwars.commands.bedwars.admin.setup.normal;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.List;

public class SetResourcePackCommand extends SubCommand {
    private static final int SHA1_HEX_LENGTH = 40;
    private final Messages msg;

    public SetResourcePackCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "setResourcePack";
    }

    @Override
    public String getDescription() {
        return "Set a per-arena resource pack URL (and optional SHA-1 hash). Use 'clear' to remove.";
    }

    @Override
    public String getSyntax() {
        return "/bw setResourcePack <url|clear> [sha1hash]";
    }

    @Override
    public String getExample() {
        return "/bw setResourcePack https://example.com/pack.zip";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        String urlArg = args[1];
        if (urlArg.equalsIgnoreCase("clear")) {
            arena.setResourcePackUrl(null);
            arena.setResourcePackHash(null);
            player.sendMessage(msg.getMsg(player, "ResourcePackCleared"));
            return;
        }
        if (args.length >= 3) {
            String hash = args[2];
            if (!isValidSha1Hex(hash)) {
                player.sendMessage(msg.getMsg(player, "ResourcePackHashInvalid"));
                return;
            }
            arena.setResourcePackUrl(urlArg);
            arena.setResourcePackHash(hash);
        } else {
            arena.setResourcePackUrl(urlArg);
            arena.setResourcePackHash(null);
        }
        player.sendMessage(msg.getMsg(player, "ResourcePackSet"));
    }

    private boolean isValidSha1Hex(String hash) {
        if (hash == null || hash.length() != SHA1_HEX_LENGTH) return false;
        return hash.matches("[0-9a-fA-F]+");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("clear");
        return List.of();
    }
}
