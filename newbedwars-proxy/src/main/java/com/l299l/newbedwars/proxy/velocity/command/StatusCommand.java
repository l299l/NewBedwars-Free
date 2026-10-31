package com.l299l.newbedwars.proxy.velocity.command;

import com.l299l.newbedwars.proxy.core.routing.ServerRegistry;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** {@code /nbwproxy} — which game servers and arenas the proxy knows. */
public final class StatusCommand implements SimpleCommand {
    private final Object lock;
    private final ServerRegistry servers;

    public StatusCommand(Object lock, ServerRegistry servers) {
        this.lock = lock;
        this.servers = servers;
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
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        return inv.source().hasPermission("newbedwars.proxy.admin");
    }
}
