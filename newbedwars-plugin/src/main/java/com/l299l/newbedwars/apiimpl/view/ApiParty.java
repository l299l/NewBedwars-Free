package com.l299l.newbedwars.apiimpl.view;

import com.l299l.newbedwars.api.view.PartyView;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class ApiParty implements PartyView {
    private final PartyManager manager;
    private final Party party;

    public ApiParty(PartyManager manager, Party party) {
        this.manager = manager;
        this.party = party;
    }

    @Override
    public UUID getLeader() {
        return party.getAdmin();
    }

    @Override
    public List<UUID> getMembers() {
        return List.copyOf(party.getMembers());
    }

    @Override
    public List<Player> getOnlineMembers() {
        return manager == null ? List.of() : List.copyOf(manager.getOnlineMembers(party));
    }

    @Override
    public boolean contains(UUID player) {
        return player != null && party.contains(player);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ApiParty other && other.party == party;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(party);
    }
}
