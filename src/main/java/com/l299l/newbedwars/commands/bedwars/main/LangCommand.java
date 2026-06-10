package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class LangCommand extends SubCommand {
    private final Messages msg;

    public LangCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "lang"; }
    @Override public String getDescription() { return "Change your language."; }
    @Override public String getSyntax() { return "/bw lang <en|pl>"; }
    @Override public String getExample() { return "/bw lang en"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        Language lang = switch (args[1].toLowerCase()) {
            case "en", "english" -> Language.English;
            case "pl", "polish", "pl_pl" -> Language.Polish;
            default -> null;
        };
        if (lang == null) {
            msg.send(player, "LanguageInvalid");
            return;
        }
        NewBedwars.plugin.getPlayerManager().updatePlayerLanguage(player.getName(), lang);
        NewBedwars.plugin.getPlayerManager().save();
        msg.send(player, "LanguageChanged", new HashMap<>() {{
            put("/lang/", lang.name());
        }});
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("en", "pl");
        return null;
    }
}
