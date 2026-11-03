package com.l299l.newbedwars.proxy.velocity;

import com.l299l.newbedwars.common.protocol.NetCodec;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.proxy.core.ProxyMessages;
import com.l299l.newbedwars.proxy.core.ProxyPlatform;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

final class VelocityPlatform implements ProxyPlatform {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ProxyServer proxy;
    private final ChannelIdentifier channel;
    private final ProxyMessages messages;
    private final Object lock;

    VelocityPlatform(ProxyServer proxy, ChannelIdentifier channel, ProxyMessages messages, Object lock) {
        this.proxy = proxy;
        this.channel = channel;
        this.messages = messages;
        this.lock = lock;
    }

    private Optional<Player> player(UUID id) {
        return id == null ? Optional.empty() : proxy.getPlayer(id);
    }

    @Override
    public boolean isOnline(UUID player) {
        return player(player).isPresent();
    }

    @Override
    public String nameOf(UUID player) {
        return player(player).map(Player::getUsername).orElse(null);
    }

    @Override
    public UUID findOnline(String name) {
        return proxy.getPlayer(name).map(Player::getUniqueId).orElse(null);
    }

    @Override
    public String currentServer(UUID player) {
        return player(player).flatMap(Player::getCurrentServer).map(c -> c.getServerInfo().getName()).orElse(null);
    }

    @Override
    public void message(UUID player, String key, Map<String, String> replacements) {
        player(player).ifPresent(p -> {
            String text = messages.format(p.getEffectiveLocale(), key, replacements);
            for (String line : text.split("\n")) p.sendMessage(LEGACY.deserialize(line));
        });
    }

    @Override
    public void inviteButtons(UUID player) {
        player(player).ifPresent(p -> {
            var locale = p.getEffectiveLocale();
            Component accept = Component.text(messages.get(locale, "PartyButtonAccept"), NamedTextColor.GREEN, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/party accept"))
                    .hoverEvent(HoverEvent.showText(Component.text(messages.get(locale, "PartyButtonAcceptHover"))));
            Component deny = Component.text(" " + messages.get(locale, "PartyButtonDeny"), NamedTextColor.RED, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/party deny"))
                    .hoverEvent(HoverEvent.showText(Component.text(messages.get(locale, "PartyButtonDenyHover"))));
            p.sendMessage(accept.append(deny));
        });
    }

    @Override
    public boolean sendToPlayerServer(UUID player, NetMessage msg) {
        Optional<ServerConnection> conn = player(player).flatMap(Player::getCurrentServer);
        return conn.isPresent() && conn.get().sendPluginMessage(channel, NetCodec.encode(msg));
    }

    @Override
    public boolean sendToServer(String server, NetMessage msg) {
        Optional<RegisteredServer> s = proxy.getServer(server);
        return s.isPresent() && !s.get().getPlayersConnected().isEmpty() && s.get().sendPluginMessage(channel, NetCodec.encode(msg));
    }

    @Override
    public void connect(UUID player, String server, Consumer<Boolean> done) {
        Optional<Player> p = player(player);
        Optional<RegisteredServer> target = proxy.getServer(server);
        if (p.isEmpty() || target.isEmpty()) {
            done.accept(false);
            return;
        }
        p.get().createConnectionRequest(target.get()).connect().whenComplete((result, error) -> {
            boolean ok = error == null && result != null && result.isSuccessful();
            synchronized (lock) {
                done.accept(ok);
            }
        });
    }

    @Override
    public int playerCount(String server) {
        return proxy.getServer(server).map(s -> s.getPlayersConnected().size()).orElse(0);
    }

    @Override
    public boolean serverExists(String server) {
        return proxy.getServer(server).isPresent();
    }
}
