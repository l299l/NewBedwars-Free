package com.l299l.newbedwars.commands.party;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyCommand implements TabExecutor {
    private final Messages msg;

    public PartyCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg.getMsgToConsole("OnlyPlayer"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(p);
            return true;
        }

        PartyManager pm = NewBedwars.plugin.getPartyManager();

        switch (args[0].toLowerCase()) {
            case "invite" -> handleInvite(p, args, pm);
            case "accept" -> handleAccept(p, pm);
            case "deny" -> handleDeny(p, pm);
            case "leave" -> handleLeave(p, pm);
            case "list" -> handleList(p, pm);
            case "remove", "kick" -> handleKick(p, args, pm);
            case "admin" -> handleAdmin(p, args, pm);
            default -> sendHelp(p);
        }
        return true;
    }

    private void handleInvite(Player p, String[] args, PartyManager pm) {
        if (args.length < 2) {
            sendHelp(p);
            return;
        }
        String targetName = args[1];
        if (targetName.equalsIgnoreCase(p.getName())) {
            msg.send(p, "PartyCantInviteSelf");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            msg.send(p, "PartyPlayerNotFound", new HashMap<>() {{ put("/player/", targetName); }});
            return;
        }
        if (pm.isPlayerInParty(target)) {
            msg.send(p, "PartyPlayerAlreadyInParty", new HashMap<>() {{ put("/player/", target.getName()); }});
            return;
        }

        Party party = pm.getParty(p);
        if (party == null) {
            party = pm.createParty(p);
        } else if (!party.isAdmin(p.getUniqueId())) {
            msg.send(p, "PartyNotAdmin");
            return;
        }

        final Party finalParty = party;
        pm.sendInvite(p, target, finalParty);
        msg.send(p, "PartyInviteSent", new HashMap<>() {{ put("/player/", target.getName()); }});
        final String inviterName = p.getName();
        msg.send(target, "PartyInviteReceived", new HashMap<>() {{ put("/player/", inviterName); }});
        sendInviteButtons(target);
    }

    private void handleAccept(Player p, PartyManager pm) {
        if (!pm.hasPendingInvite(p)) {
            msg.send(p, "PartyNoPendingInvite");
            return;
        }
        Party party = pm.getPendingInvite(p);
        String adminName = "?";
        if (party != null) {
            Player admin = Bukkit.getPlayer(party.getAdmin());
            if (admin != null) adminName = admin.getName();
        }
        final String finalAdminName = adminName;
        pm.acceptInvite(p);
        msg.send(p, "PartyAccepted", new HashMap<>() {{ put("/player/", finalAdminName); }});
        if (party != null) {
            final String joinName = p.getName();
            for (UUID memberId : party.getMembers()) {
                if (memberId.equals(p.getUniqueId())) continue;
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    msg.send(member, "PartyMemberJoined", new HashMap<>() {{ put("/player/", joinName); }});
                }
            }
        }
    }

    private void handleDeny(Player p, PartyManager pm) {
        if (!pm.hasPendingInvite(p)) {
            msg.send(p, "PartyNoPendingInvite");
            return;
        }
        Party party = pm.getPendingInvite(p);
        String adminName = "?";
        if (party != null) {
            Player admin = Bukkit.getPlayer(party.getAdmin());
            if (admin != null) adminName = admin.getName();
        }
        final String finalAdminName = adminName;
        pm.declineInvite(p);
        msg.send(p, "PartyDenied", new HashMap<>() {{ put("/player/", finalAdminName); }});
        if (party != null) {
            Player admin = Bukkit.getPlayer(party.getAdmin());
            if (admin != null) {
                final String denierName = p.getName();
                msg.send(admin, "PartyMemberDenied", new HashMap<>() {{ put("/player/", denierName); }});
            }
        }
    }

    private void handleLeave(Player p, PartyManager pm) {
        Party party = pm.getParty(p);
        if (party == null) {
            msg.send(p, "PartyNoParty");
            return;
        }
        boolean wasAdmin = party.isAdmin(p.getUniqueId());
        List<UUID> membersBefore = new ArrayList<>(party.getMembers());
        pm.leaveParty(p);
        msg.send(p, "PartyLeft");

        if (party.size() == 0) {
            return;
        }

        if (wasAdmin && party.size() > 0) {
            // Admin left — disbandParty or keep going? Spec says admin transfer happens automatically.
            // leaveParty already transferred admin. Notify remaining members.
        }
        final String leaverName = p.getName();
        for (UUID memberId : membersBefore) {
            if (memberId.equals(p.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                msg.send(member, "PartyMemberLeft", new HashMap<>() {{ put("/player/", leaverName); }});
            }
        }

        // If only 1 member left, disband
        if (party.size() == 1) {
            Player lastMember = Bukkit.getPlayer(party.getMembers().get(0));
            pm.disbandParty(party);
            if (lastMember != null) {
                msg.send(lastMember, "PartyDisbanded");
            }
        }
    }

    private void handleList(Player p, PartyManager pm) {
        Party party = pm.getParty(p);
        if (party == null) {
            msg.send(p, "PartyNoParty");
            return;
        }
        msg.send(p, "PartyList", new HashMap<>() {{ put("/size/", String.valueOf(party.size())); }});
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            String name = member != null ? member.getName() : memberId.toString().substring(0, 8) + "..";
            boolean isAdmin = party.isAdmin(memberId);
            msg.send(p, "PartyListMember", new HashMap<>() {{
                put("/player/", name);
                put("/admin/", isAdmin ? msg.getMsg(p, "PartyListAdminTag") : "");
            }});
        }
    }

    private void handleKick(Player p, String[] args, PartyManager pm) {
        Party party = pm.getParty(p);
        if (party == null) {
            msg.send(p, "PartyNoParty");
            return;
        }
        if (!party.isAdmin(p.getUniqueId())) {
            msg.send(p, "PartyNotAdmin");
            return;
        }
        if (args.length < 2) {
            sendHelp(p);
            return;
        }
        String targetName = args[1];
        if (targetName.equalsIgnoreCase(p.getName())) {
            msg.send(p, "PartyCantKickSelf");
            return;
        }
        UUID targetId = null;
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.getName().equalsIgnoreCase(targetName)) {
                targetId = memberId;
                break;
            }
        }
        if (targetId == null) {
            msg.send(p, "PartyNotFound", new HashMap<>() {{ put("/player/", targetName); }});
            return;
        }
        final UUID finalTargetId = targetId;
        final String finalTargetName = targetName;
        Player targetPlayer = Bukkit.getPlayer(targetId);
        pm.kickFromParty(party, targetId);
        msg.send(p, "PartyKicked", new HashMap<>() {{ put("/player/", finalTargetName); }});
        if (targetPlayer != null) {
            msg.send(targetPlayer, "PartyYouWereKicked");
        }
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(p.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                msg.send(member, "PartyKicked", new HashMap<>() {{ put("/player/", finalTargetName); }});
            }
        }
    }

    private void handleAdmin(Player p, String[] args, PartyManager pm) {
        Party party = pm.getParty(p);
        if (party == null) {
            msg.send(p, "PartyNoParty");
            return;
        }
        if (!party.isAdmin(p.getUniqueId())) {
            msg.send(p, "PartyNotAdmin");
            return;
        }
        if (args.length < 2) {
            sendHelp(p);
            return;
        }
        String targetName = args[1];
        UUID targetId = null;
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.getName().equalsIgnoreCase(targetName)) {
                targetId = memberId;
                break;
            }
        }
        if (targetId == null) {
            msg.send(p, "PartyNotFound", new HashMap<>() {{ put("/player/", targetName); }});
            return;
        }
        final UUID finalTargetId = targetId;
        final String finalTargetName = targetName;
        pm.transferAdmin(party, targetId);
        msg.send(p, "PartyAdminTransferred", new HashMap<>() {{ put("/player/", finalTargetName); }});
        Player newAdmin = Bukkit.getPlayer(targetId);
        if (newAdmin != null) {
            msg.send(newAdmin, "PartyYouAreAdmin");
        }
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(p.getUniqueId()) || memberId.equals(finalTargetId)) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                msg.send(member, "PartyAdminTransferred", new HashMap<>() {{ put("/player/", finalTargetName); }});
            }
        }
    }

    private void sendInviteButtons(Player target) {
        Component accept = Component.text("[Accept]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/party accept"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept the party invite")));
        Component deny = Component.text(" [Deny]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/party deny"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to deny the party invite")));
        target.sendMessage(accept.append(deny));
    }

    private void sendHelp(Player p) {
        p.sendMessage("§b§l=-= §fParty Commands §b§l=-=-");
        p.sendMessage("§e/party invite §b<player>  §f- Invite a player to your party");
        p.sendMessage("§e/party accept            §f- Accept a pending invite");
        p.sendMessage("§e/party deny              §f- Deny a pending invite");
        p.sendMessage("§e/party leave             §f- Leave your current party");
        p.sendMessage("§e/party list              §f- List all party members");
        p.sendMessage("§e/party kick §b<player>    §f- §cKick a member §7(admin only)");
        p.sendMessage("§e/party admin §b<player>   §f- §cTransfer admin role §7(admin only)");
        p.sendMessage("§7Tip: §fOnly the party admin can start a §e/bw join§f. Use §e/p§f as a shortcut.");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player p)) return null;
        PartyManager pm = NewBedwars.plugin.getPartyManager();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("invite", "accept", "deny", "leave", "list", "kick", "remove", "admin");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> !n.equals(p.getName()) && n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("kick") || sub.equals("remove") || sub.equals("admin")) {
                Party party = pm.getParty(p);
                if (party != null && party.isAdmin(p.getUniqueId())) {
                    return party.getMembers().stream()
                            .filter(id -> !id.equals(p.getUniqueId()))
                            .map(id -> {
                                Player m = Bukkit.getPlayer(id);
                                return m != null ? m.getName() : null;
                            })
                            .filter(n -> n != null && n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }
}
