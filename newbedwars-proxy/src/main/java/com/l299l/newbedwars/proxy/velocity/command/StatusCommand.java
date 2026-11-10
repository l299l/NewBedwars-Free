package com.l299l.newbedwars.proxy.velocity.command;

import com.l299l.newbedwars.proxy.core.routing.ServerRegistry;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.function.Supplier;

public final class StatusCommand implements SimpleCommand {
    private final Object lock;
    private final ServerRegistry servers;
    private final Supplier<String> updateStatus;

    public StatusCommand(Object lock, ServerRegistry servers, Supplier<String> updateStatus) {
        this.lock = lock;
        this.servers = servers;
        this.updateStatus = updateStatus;
    }

    @Override
    public void execute(Invocation inv) {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            inv.source().sendMessage(Component.text("NewBedwars-Proxy - lobbies " + servers.lobbyServers(), NamedTextColor.AQUA));
            for (var c : servers.candidates(now)) {
                String arenas = c.state == null ? "no state yet" : c.state.arenas.stream()
                        .map(x -> x.name + "(" + x.state + " " + x.players + "/" + x.maxPlayers + ")").toList().toString();
                inv.source().sendMessage(Component.text(" " + c.serverId + (c.fresh ? " [fresh] " : " [stale] ")
                        + (c.state != null && c.state.restarting ? "[restarting] " : "") + arenas,
                        c.fresh ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            }
        }
        String update = updateStatus.get();
        if (update != null) inv.source().sendMessage(Component.text(update, NamedTextColor.YELLOW));
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        return inv.source().hasPermission("newbedwars.proxy.admin");
    }
}
