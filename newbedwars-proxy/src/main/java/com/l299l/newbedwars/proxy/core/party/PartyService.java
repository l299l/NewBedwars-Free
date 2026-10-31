package com.l299l.newbedwars.proxy.core.party;

import com.l299l.newbedwars.proxy.core.ProxyPlatform;

import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.PartySync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Parties in network mode: the same commands and messages as the plugin's {@code /party}, but on the proxy so a
 * party survives server switches. Every change is synced to the backends the members are on ({@code PARTY_SYNC}).
 */
public final class PartyService {
    public static final String FROM = "proxy";

    private final ProxyPlatform platform;
    private final long inviteMillis;
    private final Map<UUID, ProxyParty> byPlayer = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    /** Last known names, so offline members still have one. */
    private final Map<UUID, String> names = new HashMap<>();
    private int nextId;

    private record Invite(ProxyParty party, UUID inviter, long expiresAt) {}

    public PartyService(ProxyPlatform platform, long inviteMillis) {
        this.platform = platform;
        this.inviteMillis = inviteMillis;
    }

    public ProxyParty partyOf(UUID player) {
        return byPlayer.get(player);
    }

    // ----- commands -----

    public void invite(UUID p, String targetName, long now) {
        String own = platform.nameOf(p);
        if (targetName.equalsIgnoreCase(own)) {
            msg(p, "PartyCantInviteSelf");
            return;
        }
        UUID target = platform.findOnline(targetName);
        if (target == null) {
            msg(p, "PartyPlayerNotFound", "/player/", targetName);
            return;
        }
        String targetReal = platform.nameOf(target);
        if (byPlayer.containsKey(target)) {
            msg(p, "PartyPlayerAlreadyInParty", "/player/", targetReal);
            return;
        }
        ProxyParty party = byPlayer.get(p);
        if (party == null) {
            party = new ProxyParty("p" + (++nextId) + "-" + Long.toHexString(now), p);
            byPlayer.put(p, party);
            sync(party, List.of());
        } else if (!party.isAdmin(p)) {
            msg(p, "PartyNotAdmin");
            return;
        }
        invites.put(target, new Invite(party, p, now + inviteMillis));
        msg(p, "PartyInviteSent", "/player/", targetReal);
        msg(target, "PartyInviteReceived", "/player/", own);
        platform.inviteButtons(target);
    }

    public void accept(UUID p) {
        Invite inv = invites.remove(p);
        if (inv == null || inv.party.size() == 0) {
            msg(p, "PartyNoPendingInvite");
            return;
        }
        if (byPlayer.containsKey(p)) {
            msg(p, "PartyAlreadyInParty");
            return;
        }
        ProxyParty party = inv.party;
        party.add(p);
        byPlayer.put(p, party);
        msg(p, "PartyAccepted", "/player/", name(party.admin()));
        for (UUID m : party.members()) {
            if (!m.equals(p)) msg(m, "PartyMemberJoined", "/player/", name(p));
        }
        sync(party, List.of());
    }

    public void deny(UUID p) {
        Invite inv = invites.remove(p);
        if (inv == null) {
            msg(p, "PartyNoPendingInvite");
            return;
        }
        msg(p, "PartyDenied", "/player/", name(inv.party.admin()));
        msg(inv.party.admin(), "PartyMemberDenied", "/player/", name(p));
    }

    public void leave(UUID p) {
        ProxyParty party = byPlayer.get(p);
        if (party == null) {
            msg(p, "PartyNoParty");
            return;
        }
        removeMember(party, p);
        msg(p, "PartyLeft");
        for (UUID m : party.members()) msg(m, "PartyMemberLeft", "/player/", name(p));
        if (party.size() == 1) {
            UUID last = party.members().get(0);
            disband(party, List.of(p));
            msg(last, "PartyDisbanded");
            return;
        }
        if (party.size() == 0) {
            disband(party, List.of(p));
            return;
        }
        if (party.isAdmin(p)) {
            UUID next = firstOnline(party, null);
            party.setAdmin(next != null ? next : party.members().get(0));
            msg(party.admin(), "PartyYouAreAdmin");
        }
        sync(party, List.of(p));
    }

    public void list(UUID p) {
        ProxyParty party = byPlayer.get(p);
        if (party == null) {
            msg(p, "PartyNoParty");
            return;
        }
        msg(p, "PartyList", "/size/", String.valueOf(party.size()));
        for (UUID m : party.members()) {
            Map<String, String> r = new HashMap<>();
            r.put("/player/", name(m));
            r.put("/admin/", party.isAdmin(m) ? "%PartyListAdminTag%" : "");
            platform.message(p, "PartyListMember", r);
        }
    }

    public void kick(UUID p, String targetName) {
        ProxyParty party = adminParty(p);
        if (party == null) return;
        if (targetName.equalsIgnoreCase(platform.nameOf(p))) {
            msg(p, "PartyCantKickSelf");
            return;
        }
        UUID target = memberByName(party, targetName);
        if (target == null) {
            msg(p, "PartyNotFound", "/player/", targetName);
            return;
        }
        String targetReal = name(target);
        removeMember(party, target);
        msg(target, "PartyYouWereKicked");
        for (UUID m : party.members()) msg(m, "PartyKicked", "/player/", targetReal);
        if (party.size() <= 1) {
            disband(party, List.of(target));
            msg(p, "PartyDisbanded");
            return;
        }
        sync(party, List.of(target));
    }

    public void transfer(UUID p, String targetName) {
        ProxyParty party = adminParty(p);
        if (party == null) return;
        UUID target = memberByName(party, targetName);
        if (target == null) {
            msg(p, "PartyNotFound", "/player/", targetName);
            return;
        }
        party.setAdmin(target);
        for (UUID m : party.members()) {
            if (m.equals(target)) msg(m, "PartyYouAreAdmin");
            else msg(m, "PartyAdminTransferred", "/player/", name(target));
        }
        sync(party, List.of());
    }

    /** Names for tab completion: the other members (for kick/admin). */
    public List<String> memberNames(UUID p) {
        ProxyParty party = byPlayer.get(p);
        List<String> out = new ArrayList<>();
        if (party == null) return out;
        for (UUID m : party.members()) {
            if (!m.equals(p) && platform.isOnline(m)) out.add(name(m));
        }
        return out;
    }

    // ----- connection events -----

    /**
     * A player left the proxy: their invite is dropped, the admin role moves to an online member, and the
     * party is disbanded once nobody is online (same as on a single server).
     */
    public void onDisconnect(UUID p) {
        invites.remove(p);
        ProxyParty party = byPlayer.get(p);
        if (party == null) return;
        UUID next = firstOnline(party, p);
        if (next == null) {
            disband(party, List.of());
            return;
        }
        if (party.isAdmin(p)) {
            party.setAdmin(next);
            msg(next, "PartyYouAreAdmin");
            sync(party, List.of());
        }
    }

    /** A player reached a backend: tell it about their party. */
    public void onServerSwitch(UUID p) {
        ProxyParty party = byPlayer.get(p);
        if (party != null) platform.sendToPlayerServer(p, NetMessage.of(FROM, toSync(party, List.of())));
    }

    public void expireInvites(long now) {
        for (Iterator<Map.Entry<UUID, Invite>> it = invites.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Invite> e = it.next();
            if (e.getValue().expiresAt > now) continue;
            it.remove();
            if (platform.isOnline(e.getKey())) msg(e.getKey(), "PartyInviteExpired", "/player/", name(e.getValue().inviter));
        }
    }

    // ----- helpers -----

    private ProxyParty adminParty(UUID p) {
        ProxyParty party = byPlayer.get(p);
        if (party == null) {
            msg(p, "PartyNoParty");
            return null;
        }
        if (!party.isAdmin(p)) {
            msg(p, "PartyNotAdmin");
            return null;
        }
        return party;
    }

    private UUID memberByName(ProxyParty party, String name) {
        for (UUID m : party.members()) {
            String n = platform.nameOf(m);
            if (n != null && n.equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    private UUID firstOnline(ProxyParty party, UUID except) {
        for (UUID m : party.members()) {
            if (!m.equals(except) && platform.isOnline(m)) return m;
        }
        return null;
    }

    private void removeMember(ProxyParty party, UUID p) {
        party.remove(p);
        byPlayer.remove(p);
    }

    private void disband(ProxyParty party, List<UUID> alsoRemoved) {
        invites.values().removeIf(inv -> inv.party == party);
        List<UUID> removed = new ArrayList<>(alsoRemoved);
        removed.addAll(party.members());
        Set<String> servers = new HashSet<>();
        PartySync sync = new PartySync(party.id(), party.admin(), List.of(), true, removed);
        for (UUID m : removed) {
            byPlayer.remove(m);
            sendOncePerServer(m, sync, servers);
        }
        for (UUID m : party.members()) party.remove(m);
    }

    /** Sends the party to each server one of its (or a just removed) member is on. */
    private void sync(ProxyParty party, List<UUID> removed) {
        PartySync s = toSync(party, removed);
        Set<String> servers = new HashSet<>();
        for (UUID m : party.members()) sendOncePerServer(m, s, servers);
        for (UUID m : removed) sendOncePerServer(m, s, servers);
    }

    private void sendOncePerServer(UUID via, PartySync s, Set<String> done) {
        String server = platform.currentServer(via);
        if (server == null || !done.add(server)) return;
        platform.sendToPlayerServer(via, NetMessage.of(FROM, s));
    }

    PartySync toSync(ProxyParty party, List<UUID> removed) {
        List<PartySync.Member> members = new ArrayList<>();
        for (UUID m : party.members()) members.add(new PartySync.Member(m, name(m)));
        return new PartySync(party.id(), party.admin(), members, false, new ArrayList<>(removed));
    }

    private String name(UUID p) {
        String n = platform.nameOf(p);
        if (n != null) {
            names.put(p, n);
            return n;
        }
        return names.getOrDefault(p, "?");
    }

    private void msg(UUID p, String key) {
        platform.message(p, key, Map.of());
    }

    private void msg(UUID p, String key, String placeholder, String value) {
        platform.message(p, key, Map.of(placeholder, value == null ? "?" : value));
    }
}
