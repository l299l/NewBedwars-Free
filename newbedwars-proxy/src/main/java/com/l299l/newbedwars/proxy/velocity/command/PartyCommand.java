package com.l299l.newbedwars.proxy.velocity.command;

import com.l299l.newbedwars.proxy.core.party.PartyService;
import com.l299l.newbedwars.proxy.core.routing.Router;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PartyCommand implements SimpleCommand {
    private final ProxyServer proxy;
    private final Object lock;
    private final PartyService parties;
    private final Router router;

    public PartyCommand(ProxyServer proxy, Object lock, PartyService parties, Router router) {
        this.proxy = proxy;
        this.lock = lock;
        this.parties = parties;
        this.router = router;
    }

    @Override
    public void execute(Invocation inv) {
        if (!(inv.source() instanceof Player p)) return;
        String[] a = inv.arguments();
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        synchronized (lock) {
            switch (sub) {
                case "invite" -> {
                    if (a.length < 2) help(p);
                    else parties.invite(p.getUniqueId(), a[1], now);
                }
                case "accept" -> parties.accept(p.getUniqueId());
                case "deny" -> parties.deny(p.getUniqueId());
                case "leave" -> parties.leave(p.getUniqueId());
                case "list" -> parties.list(p.getUniqueId());
                case "kick", "remove" -> {
                    if (a.length < 2) help(p);
                    else parties.kick(p.getUniqueId(), a[1]);
                }
                case "admin" -> {
                    if (a.length < 2) help(p);
                    else parties.transfer(p.getUniqueId(), a[1]);
                }
                default -> help(p);
            }
        }
    }

    private void help(Player p) {
        router.platformMessage(p.getUniqueId(), "PartyHelp");
    }

    @Override
    public List<String> suggest(Invocation inv) {
        String[] a = inv.arguments();
        List<String> out = new ArrayList<>();
        if (a.length <= 1) {
            String prefix = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
            for (String s : List.of("invite", "accept", "deny", "leave", "list", "kick", "remove", "admin")) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }
        if (a.length == 2 && inv.source() instanceof Player p) {
            String sub = a[0].toLowerCase(Locale.ROOT);
            String prefix = a[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            if (sub.equals("invite")) {
                for (Player o : proxy.getAllPlayers()) if (!o.equals(p)) names.add(o.getUsername());
            } else if (sub.equals("kick") || sub.equals("remove") || sub.equals("admin")) {
                synchronized (lock) {
                    names.addAll(parties.memberNames(p.getUniqueId()));
                }
            }
            for (String n : names) if (n.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(n);
        }
        return out;
    }
}
