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
    private static final int MAX_PENDING_INVITES = 10;
    private final Plugin plugin;
    private final Messages messages;
    private final Map<UUID, Party> playerToParty = new HashMap<>();
    private final Map<UUID, Party> pendingInvites = new HashMap<>();
    private final Map<String, Party> networkParties = new HashMap<>();
    private final Map<UUID, String> networkNames = new HashMap<>();

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
        changed(party, creator.getUniqueId(), com.l299l.newbedwars.api.events.PartyChangeEvent.Change.CREATED);
        return party;
    }

    public Party getPendingInvite(Player player) {
        return pendingInvites.get(player.getUniqueId());
    }

    public boolean hasPendingInvite(Player player) {
        return pendingInvites.containsKey(player.getUniqueId());
    }

    public boolean canSendInvite(Party party) {
        return pendingInvites.values().stream().filter(p -> p == party).count() < MAX_PENDING_INVITES;
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
                                Map.of("/player/", inviter.getName()),
                                com.l299l.newbedwars.placeholder.MessageContext.about(inviter));
                    }
                }
            }
        }.runTaskLater(plugin, 20L * INVITE_EXPIRY_SECONDS);
    }

    public boolean acceptInvite(Player player) {
        Party party = pendingInvites.remove(player.getUniqueId());
        if (party == null || party.size() == 0) return false;
        if (playerToParty.containsKey(player.getUniqueId())) return false;
        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party);
        changed(party, player.getUniqueId(), com.l299l.newbedwars.api.events.PartyChangeEvent.Change.JOINED);
        return true;
    }

    public void declineInvite(Player player) {
        pendingInvites.remove(player.getUniqueId());
    }

    public void leaveParty(Player player) {
        Party party = playerToParty.remove(player.getUniqueId());
        if (party == null) return;
        party.removeMember(player.getUniqueId());
        changed(party, player.getUniqueId(), com.l299l.newbedwars.api.events.PartyChangeEvent.Change.LEFT);
        if (party.size() == 0) {
            changed(party, null, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.DISBANDED);
            return;
        }
        if (party.isAdmin(player.getUniqueId())) {
            UUID newAdmin = party.getMembers().get(0);
            for (UUID member : party.getMembers()) {
                if (Bukkit.getPlayer(member) != null) {
                    newAdmin = member;
                    break;
                }
            }
            party.setAdmin(newAdmin);
            Player newAdminPlayer = Bukkit.getPlayer(newAdmin);
            if (newAdminPlayer != null) {
                messages.send(newAdminPlayer, "PartyYouAreAdmin");
            }
            changed(party, newAdmin, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.LEADER_CHANGED);
        }
    }

    private void changed(Party party, UUID player, com.l299l.newbedwars.api.events.PartyChangeEvent.Change change) {
        com.l299l.newbedwars.apiimpl.ApiEvents.partyChange(this, party, player, change);
    }

    public void disbandParty(Party party) {
        pendingInvites.values().removeIf(invited -> invited == party);
        boolean hadMembers = !party.getMembers().isEmpty();
        for (UUID member : new ArrayList<>(party.getMembers())) {
            playerToParty.remove(member);
        }
        party.getMembers().clear();
        if (hadMembers) changed(party, null, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.DISBANDED);
    }

    public void kickFromParty(Party party, UUID target) {
        if (!party.getMembers().contains(target)) return;
        party.removeMember(target);
        playerToParty.remove(target);
        changed(party, target, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.KICKED);
    }

    public void transferAdmin(Party party, UUID newAdmin) {
        if (!party.getMembers().contains(newAdmin) || party.isAdmin(newAdmin)) return;
        party.setAdmin(newAdmin);
        changed(party, newAdmin, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.LEADER_CHANGED);
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

    public void cleanupOfflinePlayer(Player player) {
        UUID id = player.getUniqueId();
        pendingInvites.remove(id);
        if (com.l299l.newbedwars.network.NetworkManager.get() != null) {
            return;
        }
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
            changed(party, nextOnline, com.l299l.newbedwars.api.events.PartyChangeEvent.Change.LEADER_CHANGED);
        }
    }

    public String nameOf(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        return networkNames.getOrDefault(id, "?");
    }

    public void applyNetworkSync(com.l299l.newbedwars.common.protocol.payload.PartySync sync) {
        if (sync == null) return;
        if (sync.removed != null) {
            for (UUID id : sync.removed) {
                Party old = playerToParty.remove(id);
                if (old != null) old.removeMember(id);
            }
        }
        if (sync.partyId == null) return;
        Party party = networkParties.get(sync.partyId);
        if (sync.disbanded || sync.admin == null || sync.members == null || sync.members.isEmpty()) {
            if (party != null) {
                networkParties.remove(sync.partyId);
                for (UUID member : new ArrayList<>(party.getMembers())) playerToParty.remove(member);
                party.getMembers().clear();
            }
            return;
        }
        if (party == null) {
            party = new Party(sync.admin);
            networkParties.put(sync.partyId, party);
        }
        for (UUID member : new ArrayList<>(party.getMembers())) {
            if (playerToParty.get(member) == party) playerToParty.remove(member);
        }
        party.getMembers().clear();
        for (com.l299l.newbedwars.common.protocol.payload.PartySync.Member m : sync.members) {
            if (m == null || m.uuid == null) continue;
            party.addMember(m.uuid);
            playerToParty.put(m.uuid, party);
            if (m.name != null) networkNames.put(m.uuid, m.name);
        }
        party.setAdmin(sync.admin);
    }
}
