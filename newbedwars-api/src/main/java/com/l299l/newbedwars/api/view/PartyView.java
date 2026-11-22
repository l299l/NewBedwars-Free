package com.l299l.newbedwars.api.view;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * A read-only view of a party. It shows the live party: calling the getters later returns the current members.
 *
 * @since 2.8-beta
 */
public interface PartyView {
    /** The party leader. */
    UUID getLeader();

    /** Every member including the leader, online or not. Empty once the party is disbanded. */
    List<UUID> getMembers();

    /** The members who are online on this server. */
    List<Player> getOnlineMembers();

    /** Whether the player is a member of this party. */
    boolean contains(UUID player);

    /** The number of members. */
    default int size() {
        return getMembers().size();
    }
}
