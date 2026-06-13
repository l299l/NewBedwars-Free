package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class JoinCommand extends SubCommand {
    private final Messages msg;

    public JoinCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Command to join arena";
    }

    @Override
    public String getSyntax() {
        return "/bw join (<arena>)";
    }

    @Override
    public String getExample() {
        return "/bw join arena1";
    }

    @Override
    public void perform(Player p, String[] args, IArena old) {
        if (Properties.RequireJoinPermission && !p.hasPermission("newbedwars.bw.join") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return;
        }

        PartyManager pm = NewBedwars.plugin.getPartyManager();
        Party party = pm.getParty(p);

        // Regular party members cannot initiate joins — only the admin can
        if (party != null && !party.isAdmin(p.getUniqueId())) {
            Player admin = org.bukkit.Bukkit.getPlayer(party.getAdmin());
            String adminName = admin != null ? admin.getName() : "?";
            msg.send(p, "PartyOnlyAdminCanJoin", new HashMap<>() {{ put("/player/", adminName); }});
            return;
        }

        // Detect pending-rejoin slots (player still in players map but not physically in the arena world)
        IArena pendingArena = null;
        for (IArena existing : Arena.arenaByName.values()) {
            if (existing.isPlayerInArena(p)) {
                if (Arena.arenaByWorld.get(p.getWorld()) == existing) {
                    msg.send(p, "ArenaJoinError");
                    return;
                }
                pendingArena = existing;
                break;
            }
        }

        if (args.length > 1) {
            IArena arena = Arena.arenaByName.get(args[1]);
            if (arena == null) {
                msg.send(p, "ArenaNotExists");
                return;
            }
            if (!arena.isEnabled()) {
                msg.send(p, "ArenaNotEnabled", new HashMap<>() {{
                    put("/arenaname/", arena.getArenaName());
                }});
                return;
            }
            if (pendingArena != null) {
                if (pendingArena != arena) {
                    msg.send(p, "ArenaJoinError");
                    return;
                }
                if (arena.rejoin(p)) {
                    msg.send(p, "Rejoined");
                } else {
                    msg.send(p, "RejoinFailed");
                }
                return;
            }
            GameStatus status = arena.status();
            if (status == GameStatus.waiting || status == GameStatus.starting) {
                if (party != null && arena.getGamerules().AllowParties) {
                    // Party join — check size vs maxInTeam
                    List<Player> members = pm.getOnlineMembers(party);
                    if (members.size() > arena.getMaxInTeam()) {
                        msg.send(p, "PartyTooBig", new HashMap<>() {{
                            put("/maxteam/", String.valueOf(arena.getMaxInTeam()));
                        }});
                        return;
                    }
                    if (!arena.joinParty(members)) {
                        msg.send(p, "ArenaIsFullError");
                    }
                } else if (party != null && !arena.getGamerules().AllowParties) {
                    msg.send(p, "PartyNotAllowedInArena");
                } else {
                    if (!arena.join(p)) {
                        msg.send(p, "ArenaIsFullError");
                    }
                }
            } else if (status == GameStatus.playing) {
                msg.send(p, "ArenaRunningError");
            } else if (status == GameStatus.restarting) {
                msg.send(p, "ArenaRestartingError");
            } else {
                msg.send(p, "ArenaJoinError");
            }
        } else {
            p.openInventory(new ArenaSelectGUI(p).getInventory());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return Arena.arenaByName.keySet().stream().toList();
        }
        return null;
    }
}
