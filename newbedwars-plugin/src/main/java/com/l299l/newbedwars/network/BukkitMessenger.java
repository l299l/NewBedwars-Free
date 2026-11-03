package com.l299l.newbedwars.network;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.l299l.newbedwars.common.protocol.NetChannel;
import com.l299l.newbedwars.common.protocol.NetCodec;
import com.l299l.newbedwars.common.protocol.NetMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public final class BukkitMessenger implements Messenger, PluginMessageListener {
    private final Plugin plugin;
    private final BiConsumer<Player, NetMessage> handler;

    public BukkitMessenger(Plugin plugin, BiConsumer<Player, NetMessage> handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    public void register() {
        var m = Bukkit.getMessenger();
        m.registerOutgoingPluginChannel(plugin, NetChannel.MAIN);
        m.registerIncomingPluginChannel(plugin, NetChannel.MAIN, this);
        m.registerOutgoingPluginChannel(plugin, NetChannel.BUNGEE);
    }

    public void unregister() {
        var m = Bukkit.getMessenger();
        m.unregisterIncomingPluginChannel(plugin, NetChannel.MAIN, this);
        m.unregisterOutgoingPluginChannel(plugin, NetChannel.MAIN);
        m.unregisterOutgoingPluginChannel(plugin, NetChannel.BUNGEE);
    }

    @Override
    public boolean send(NetMessage msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (send(p, msg)) return true;
        }
        return false;
    }

    @Override
    public boolean send(Player via, NetMessage msg) {
        if (via == null || !via.isOnline()) return false;
        if (!via.getListeningPluginChannels().contains(NetChannel.MAIN)) {
            warnOnce("[Network] Can not send " + msg.type() + " through " + via.getName()
                    + ": the channel " + NetChannel.MAIN + " is not registered. Is NewBedwars-Proxy installed on Velocity?");
            return false;
        }
        try {
            via.sendPluginMessage(plugin, NetChannel.MAIN, NetCodec.encode(msg));
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[Network] Could not send " + msg.type() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean warnedUnreadable;
    private long lastWarn;

    private void warnOnce(String text) {
        long now = System.currentTimeMillis();
        if (now - lastWarn < 30_000L) return;
        lastWarn = now;
        plugin.getLogger().warning(text);
    }

    @Override
    public void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, NetChannel.BUNGEE, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!NetChannel.MAIN.equals(channel)) return;
        NetMessage msg = NetCodec.decode(message);
        if (msg == null) {
            if (!warnedUnreadable) {
                warnedUnreadable = true;
                plugin.getLogger().warning("[Network] Dropped an unreadable message (other protocol version? Further ones are not logged)");
            }
            return;
        }
        handler.accept(player, msg);
    }
}
