package com.l299l.newbedwars.network;

import com.l299l.newbedwars.common.protocol.NetMessage;
import org.bukkit.entity.Player;

public interface Messenger {

    boolean send(NetMessage msg);

    boolean send(Player via, NetMessage msg);

    void connect(Player player, String server);
}
