package com.l299l.newbedwars.parties;

import com.l299l.newbedwars.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyManager {
    private static final int INVITE_EXPIRY_SECONDS = 60;
    private final Plugin plugin;
    private final Messages messages;
    private final Map<UUID, Party> playerToParty = new HashMap<>();
    private final Map<UUID, Party> pendingInvites = new HashMap<>();

    public PartyManager(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public boolean isPlayerInParty(Player p) {
        return playerToParty.containsKey(p.getUniqueId());
    }

    public boolean isPlayerInParty(String name) {
        Player p = Bukkit.getPlayer(name);
        return p != null && isPlayerInParty(p);
    }

    public Party getParty(Player p) {
        return playerToParty.get(p.getUniqueId());
    }

    public Party createParty(Player creator) {
        Party party = new Party(creator.getUniqueId());
        playerToParty.put(creator.getUniqueId(), party);
        return party;
    }

    public Party getPendingInvite(Player player) {
        return pendingInvites.get(player.getUniqueId());
    }

    public boolean hasPendingInvite(Player player) {
        return pendingInvites.containsKey(player.getUniqueId());
    }

    public void sendInvite(Player inviter, Player invitee, Party party) {
        pendingInvites.put(invitee.getUniqueId(), party);
        scheduleInviteExpiry(inviter, invitee, party);
    }

    protected void scheduleInviteExpiry(Player inviter, Player invitee, Party party) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Party stored = pendingInvites.get(invitee.getUniqueId());
                if (stored == party) {
                    pendingInvites.remove(invitee.getUniqueId());
                    if (invitee.isOnline()) {
                        messages.send(invitee, "PartyInviteExpired",
                                Map.of("/player/", inviter.getName()));
                    }
                }
            }
        }.runTaskLater(plugin, 20L * INVITE_EXPIRY_SECONDS);
    }

    public boolean acceptInvite(Player player) {
        Party party = pendingInvites.remove(player.getUniqueId());
        if (party == null || party.size() == 0) return false;
        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party);
        return true;
    }

    public void declineInvite(Player player) {
        pendingInvites.remove(player.getUniqueId());
    }

    public void leaveParty(Player player) {
        Party party = playerToParty.remove(player.getUniqueId());
        if (party == null) return;
        party.removeMember(player.getUniqueId());
        if (party.size() == 0) return;
        if (party.isAdmin(player.getUniqueId())) {
            UUID newAdmin = party.getMembers().get(0);
            party.setAdmin(newAdmin);
            Player newAdminPlayer = Bukkit.getPlayer(newAdmin);
            if (newAdminPlayer != null) {
                messages.send(newAdminPlayer, "PartyYouAreAdmin");
            }
        }
    }

    public void disbandParty(Party party) {
        pendingInvites.values().removeIf(invited -> invited == party);
        for (UUID member : new ArrayList<>(party.getMembers())) {
            playerToParty.remove(member);
        }
        party.getMembers().clear();
    }

    public void kickFromParty(Party party, UUID target) {
        if (!party.getMembers().contains(target)) return;
        party.removeMember(target);
        playerToParty.remove(target);
    }

    public void transferAdmin(Party party, UUID newAdmin) {
        if (!party.getMembers().contains(newAdmin)) return;
        party.setAdmin(newAdmin);
    }

    public List<Player> getOnlineMembers(Party party) {
        List<Player> online = new ArrayList<>();
        for (UUID memberId : party.getMembers()) {
            Player p = Bukkit.getPlayer(memberId);
            if (p != null) {
                online.add(p);
            }
        }
        return online;
    }

    /**
     * Called when a player quits: drops their pending invite, hands the admin role to another
     * online member and disbands the party once nobody else is online. The quitting player stays
     * in the party so they can come back to it.
     */
    public void cleanupOfflinePlayer(Player player) {
        UUID id = player.getUniqueId();
        pendingInvites.remove(id);
        Party party = playerToParty.get(id);
        if (party == null) return;
        UUID nextOnline = null;
        for (UUID member : party.getMembers()) {
            if (!member.equals(id) && Bukkit.getPlayer(member) != null) {
                nextOnline = member;
                break;
            }
        }
        if (nextOnline == null) {
            pendingInvites.values().removeIf(party::equals);
            disbandParty(party);
            return;
        }
        if (party.isAdmin(id)) {
            party.setAdmin(nextOnline);
            Player newAdmin = Bukkit.getPlayer(nextOnline);
            if (newAdmin != null) {
                messages.send(newAdmin, "PartyYouAreAdmin");
            }
        }
    }
}
