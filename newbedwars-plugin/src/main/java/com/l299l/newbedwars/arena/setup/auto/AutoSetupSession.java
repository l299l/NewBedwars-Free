package com.l299l.newbedwars.arena.setup.auto;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.ArenaInfo;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Messages;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoSetupSession {
    private static final Map<UUID, AutoSetupSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, MapScanner> SCANS = new ConcurrentHashMap<>();

    public record Button(String labelKey, String command, boolean suggest) {}

    private final UUID admin;
    private final IArena arena;
    private final AreaTracker areas = new AreaTracker();
    private final Set<String> skipped = new HashSet<>();
    private SetupStep current;

    private AutoSetupSession(UUID admin, IArena arena) {
        this.admin = admin;
        this.arena = arena;
    }

    public static AutoSetupSession start(Player player, IArena arena) {
        AutoSetupSession session = new AutoSetupSession(player.getUniqueId(), arena);
        SESSIONS.put(player.getUniqueId(), session);
        return session;
    }

    public static AutoSetupSession of(Player player, IArena arena) {
        AutoSetupSession session = SESSIONS.get(player.getUniqueId());
        if (session == null) return null;
        if (session.arena != arena || !arena.getPlayersOnSetup().contains(player.getUniqueId())) {
            SESSIONS.remove(player.getUniqueId());
            return null;
        }
        return session;
    }

    public static boolean isScanning(Player player) {
        return SCANS.containsKey(player.getUniqueId());
    }

    public static void scanStarted(Player player, MapScanner scanner) {
        SCANS.put(player.getUniqueId(), scanner);
    }

    public static void scanEnded(UUID id) {
        SCANS.remove(id);
    }

    public static boolean cancel(Player player) {
        MapScanner scanner = SCANS.remove(player.getUniqueId());
        if (scanner != null) scanner.cancel();
        return SESSIONS.remove(player.getUniqueId()) != null || scanner != null;
    }

    public AreaTracker areas() {
        return areas;
    }

    public SetupStep current() {
        return current;
    }

    public SetupStep firstOpen() {
        for (SetupStep step : SetupStep.missing(arena)) {
            if (!skipped.contains(step.key())) return step;
        }
        return null;
    }

    public void prompt(Player player) {
        areas.refresh(arena);
        current = firstOpen();
        show(player);
    }

    public void afterCommand(Player player) {
        areas.refresh(arena);
        SetupStep now = firstOpen();
        if (Objects.equals(now, current)) return;
        current = now;
        show(player);
    }

    public void skip(Player player) {
        if (current != null) skipped.add(current.key());
        prompt(player);
    }

    private void show(Player player) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (current == null) {
            finish(player, msg);
            return;
        }
        int left = (int) SetupStep.missing(arena).stream().filter(s -> !skipped.contains(s.key())).count();
        String text = msg.getMsg(player, "AutoSetup-Step-" + current.kind().name())
                .replace("/team/", teamLabel(current.team()))
                .replace("/left/", String.valueOf(left));
        List<BaseComponent> line = new ArrayList<>(List.of(TextComponent.fromLegacyText(text)));
        for (Button button : buttons(current)) {
            line.add(new TextComponent(" "));
            line.addAll(button(msg.getMsg(player, button.labelKey()), button));
        }
        line.add(new TextComponent(" "));
        line.addAll(button(msg.getMsg(player, "AutoSetup-Button-Skip"), new Button("AutoSetup-Button-Skip", "/bw startAutomaticSetup skip", false)));
        player.spigot().sendMessage(line.toArray(new BaseComponent[0]));
    }

    private void finish(Player player, Messages msg) {
        player.sendMessage(msg.getMsg(player, arena.canBeEnabled() ? "AutoSetup-Finished-Ready" : "AutoSetup-Finished-NotReady")
                .replace("/skipped/", String.valueOf(skipped.size())));
        List<BaseComponent> line = new ArrayList<>(List.of(TextComponent.fromLegacyText(msg.getMsg(player, "AutoSetup-SaveReminder"))));
        line.add(new TextComponent(" "));
        line.addAll(button(msg.getMsg(player, "AutoSetup-Button-Save"), new Button("AutoSetup-Button-Save", "/bw save", false)));
        player.spigot().sendMessage(line.toArray(new BaseComponent[0]));
        SESSIONS.remove(admin);
    }

    private String teamLabel(String teamName) {
        if (teamName == null) return "";
        Team team = arena.getTeams().get(teamName);
        return (team == null ? "" : team.getColor().toString()) + teamName + "§r";
    }

    public static List<Button> buttons(SetupStep step) {
        String team = step.team();
        return switch (step.kind()) {
            case TEAMS -> List.of(new Button("AutoSetup-Button-CreateTeam", "/bw createTeam ", true));
            case TEAM_BED -> List.of(here("/bw setTeamBed " + team));
            case TEAM_SPAWN -> List.of(here("/bw setTeamSpawn " + team));
            case TEAM_SHOP -> List.of(here("/bw setTeamShop " + team + " VILLAGER"));
            case TEAM_UPGRADES -> List.of(here("/bw setTeamUpgrades " + team + " VILLAGER"));
            case TEAM_GENERATOR -> List.of(here("/bw setGenerator BASIC " + team));
            case TEAM_BASE -> List.of(new Button("AutoSetup-Button-Corner1", "/bw setTeamBasePos1 " + team, false),
                    new Button("AutoSetup-Button-Corner2", "/bw setTeamBasePos2 " + team, false));
            case TEAM_PROTECTION -> List.of(new Button("AutoSetup-Button-Corner1", "/bw setTeamBuildProtAreaPos1 " + team, false),
                    new Button("AutoSetup-Button-Corner2", "/bw setTeamBuildProtAreaPos2 " + team, false));
            case ARENA_GENERATOR -> List.of(new Button("AutoSetup-Button-Diamond", "/bw setGenerator DIAMOND", false),
                    new Button("AutoSetup-Button-Emerald", "/bw setGenerator EMERALD", false));
            case WAITING_POS1 -> List.of(here("/bw setWaitingPos1"));
            case WAITING_POS2 -> List.of(here("/bw setWaitingPos2"));
            case WAITING_SPAWN -> List.of(here("/bw setWaitingSpawn"));
            case QUICK_VOID -> List.of(here("/bw setQuickVoidY"));
        };
    }

    private static Button here(String command) {
        return new Button("AutoSetup-Button-Here", command, false);
    }

    private static List<BaseComponent> button(String label, Button button) {
        BaseComponent[] parts = TextComponent.fromLegacyText("§a[" + label + "§a]");
        List<BaseComponent> out = new ArrayList<>();
        for (BaseComponent part : parts) {
            part.setClickEvent(new ClickEvent(button.suggest() ? ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND, button.command()));
            part.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(button.command())));
            out.add(part);
        }
        return out;
    }

    public static String summaryLine(Player player, Team team) {
        StringBuilder out = new StringBuilder(team.getColor().toString()).append(team.getName()).append("§7:");
        for (ArenaInfo.TeamSetting setting : ArenaInfo.teamSettings(team)) {
            out.append(" §7").append(setting.label(player)).append(' ').append(ArenaInfo.tick(setting.set()));
        }
        return out.toString();
    }
}
