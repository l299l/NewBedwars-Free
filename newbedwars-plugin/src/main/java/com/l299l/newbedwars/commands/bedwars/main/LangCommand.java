package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

public class LangCommand extends BwCommand {

    private static final long COOLDOWN_MILLIS = 2000;
    private static final Map<UUID, Long> LAST_CHANGE = new HashMap<>();

    static synchronized boolean onCooldown(UUID id, long now) {
        LAST_CHANGE.values().removeIf(t -> now - t >= COOLDOWN_MILLIS);
        Long last = LAST_CHANGE.get(id);
        if (last != null && now - last < COOLDOWN_MILLIS) return true;
        LAST_CHANGE.put(id, now);
        return false;
    }

    @Override public String getName() { return "lang"; }
    @Override public String getDescription() { return "Change your language."; }
    @Override public String getSyntax() { return "/bw lang <en|pl|de|es|fr|ru>"; }
    @Override public String getExample() { return "/bw lang en"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            usage(player);
            return;
        }
        Language lang = switch (args[1].toLowerCase()) {
            case "en", "english"                -> Language.English;
            case "pl", "polish", "pl_pl"        -> Language.Polish;
            case "de", "german", "deutsch"      -> Language.German;
            case "es", "spanish", "español"     -> Language.Spanish;
            case "fr", "french", "français"     -> Language.French;
            case "ru", "russian", "русский"     -> Language.Russian;
            default -> null;
        };
        if (lang == null) {
            msg.send(player, "LanguageInvalid");
            return;
        }
        PlayerManager players = NewBedwars.plugin.getPlayerManager();
        PlayerProfile current = players.getPlayers().get(player.getName());
        if (current == null || current.language() != lang) {
            if (onCooldown(player.getUniqueId(), System.currentTimeMillis())) {
                msg.send(player, "LanguageCooldown");
                return;
            }
            players.updatePlayerLanguage(player.getName(), lang);
            players.save();
        }
        msg.send(player, "LanguageChanged", Map.of("/lang/", lang.name()));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("en", "pl", "de", "es", "fr", "ru");
        return null;
    }
}
