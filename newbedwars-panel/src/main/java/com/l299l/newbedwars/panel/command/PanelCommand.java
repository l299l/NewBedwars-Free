package com.l299l.newbedwars.panel.command;

import com.l299l.newbedwars.panel.NewBedwarsPanel;
import com.l299l.newbedwars.panel.PanelMessages;
import com.l299l.newbedwars.panel.auth.Account;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class PanelCommand implements TabExecutor {
    public static final String PERMISSION = "newbedwars.panel.manage";
    private static final List<String> ROOT = List.of("status", "user");
    private static final List<String> USER = List.of("create", "reset", "delete", "list");

    private final NewBedwarsPanel plugin;

    public PanelCommand(NewBedwarsPanel plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            send(sender, "NoPermission");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("status")) {
            status(sender);
        } else if (sub.equals("user") && args.length >= 2) {
            user(sender, args);
        } else {
            send(sender, "Usage");
        }
        return true;
    }

    private void status(CommandSender sender) {
        NewBedwarsPanel.Status status = plugin.status();
        if (status.url() == null) {
            send(sender, "StatusStopped");
        } else {
            send(sender, "StatusRunning", status.url());
            String certificate;
            if (status.fingerprint() != null) certificate = text(sender, "StatusSelfSigned", status.fingerprint());
            else if (status.https()) certificate = text(sender, "StatusOwnCertificate");
            else certificate = text(sender, "StatusPlainHttp");
            send(sender, "StatusCertificate", certificate);
        }
        UserCommands users = plugin.userCommands();
        if (users == null) return;
        int count = users.list().size();
        send(sender, "StatusAccounts", count, status.sessions());
        if (count == 0) send(sender, "StatusNoAdmin");
    }

    private void user(CommandSender sender, String[] args) {
        UserCommands users = plugin.userCommands();
        if (users == null) {
            send(sender, "NotRunning");
            return;
        }
        String actor = sender instanceof Player ? sender.getName() : "console";
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length != 4) {
                    send(sender, "UsageCreate", String.join(", ", users.roleNames()));
                    return;
                }
                runAsync(sender, () -> users.create(actor, args[2], args[3]));
            }
            case "reset" -> {
                if (args.length != 3) {
                    send(sender, "UsageReset");
                    return;
                }
                runAsync(sender, () -> users.reset(actor, args[2]));
            }
            case "delete" -> {
                if (args.length != 3) {
                    send(sender, "UsageDelete");
                    return;
                }
                report(sender, users.delete(actor, args[2]));
            }
            case "list" -> list(sender, users);
            default -> send(sender, "Usage");
        }
    }

    private void list(CommandSender sender, UserCommands users) {
        List<Account> accounts = users.list();
        if (accounts.isEmpty()) {
            send(sender, "ListEmpty");
            return;
        }
        send(sender, "ListHeader", accounts.size());
        for (Account account : accounts) {
            String note = account.mustChangePassword() ? text(sender, "ListMustChange") : "";
            sender.sendMessage(color(text(sender, "ListEntry", account.name(), account.role(), note)));
        }
    }

    private void runAsync(CommandSender sender, Supplier<UserCommands.Result> task) {
        send(sender, "Working");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UserCommands.Result result = task.get();
            Bukkit.getScheduler().runTask(plugin, () -> report(sender, result));
        });
    }

    private void report(CommandSender sender, UserCommands.Result result) {
        switch (result.code()) {
            case CREATED -> {
                send(sender, "UserCreated", result.name(), result.role());
                password(sender, result.password());
            }
            case RESET -> {
                send(sender, "PasswordReset", result.name());
                password(sender, result.password());
            }
            case DELETED -> send(sender, "UserDeleted", result.name());
            case EXISTS -> send(sender, "UserExists", result.name());
            case NOT_FOUND -> send(sender, "UserNotFound", result.name());
            case INVALID_NAME -> send(sender, "InvalidName", result.name());
            case UNKNOWN_ROLE -> send(sender, "UnknownRole", result.role(),
                    String.join(", ", plugin.userCommands() == null ? List.of() : plugin.userCommands().roleNames()));
            case LAST_ADMIN -> send(sender, "LastAdmin");
            case SAVE_FAILED -> send(sender, "SaveFailed");
        }
    }

    private void password(CommandSender sender, String password) {
        send(sender, "OneTimePassword", password);
        send(sender, "OneTimePasswordNote");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();
        if (args.length == 1) return matching(ROOT, args[0]);
        if (!args[0].equalsIgnoreCase("user")) return List.of();
        if (args.length == 2) return matching(USER, args[1]);
        UserCommands users = plugin.userCommands();
        if (users == null) return List.of();
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3 && (sub.equals("reset") || sub.equals("delete"))) {
            return matching(users.list().stream().map(Account::name).toList(), args[2]);
        }
        if (args.length == 4 && sub.equals("create")) return matching(users.roleNames(), args[3]);
        return List.of();
    }

    private static List<String> matching(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) result.add(option);
        }
        return result;
    }

    private String language(CommandSender sender) {
        PanelMessages messages = plugin.messages();
        return messages.language(sender instanceof Player player ? player.getLocale() : null);
    }

    private String text(CommandSender sender, String key, Object... args) {
        return plugin.messages().get(language(sender), key, args);
    }

    private void send(CommandSender sender, String key, Object... args) {
        sender.sendMessage(color(text(sender, "Prefix") + text(sender, key, args)));
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
