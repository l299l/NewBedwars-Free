package com.l299l.newbedwars.panel.command;

import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.PasswordHasher;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class UserCommands {
    public static final int PASSWORD_LENGTH = 16;
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Code { CREATED, RESET, DELETED, ROLE_CHANGED, EXISTS, NOT_FOUND, INVALID_NAME, UNKNOWN_ROLE, LAST_ADMIN, FORBIDDEN, SAVE_FAILED }

    public record Actor(String name, String ip, Role role) {
        public static Actor console(String name) {
            return new Actor(name, "server", null);
        }

        public boolean mayGrant(Role target) {
            return role == null || role.permissions().containsAll(target.permissions());
        }
    }

    public record Result(Code code, String name, String role, String password) {
        static Result of(Code code, String name) {
            return new Result(code, name, null, null);
        }

        public boolean success() {
            return code == Code.CREATED || code == Code.RESET || code == Code.DELETED || code == Code.ROLE_CHANGED;
        }
    }

    private final AccountStore accounts;
    private final SessionManager sessions;
    private final PasswordHasher hasher;
    private final AuditLog audit;
    private final LongSupplier clock;

    public UserCommands(AccountStore accounts, SessionManager sessions, PasswordHasher hasher, AuditLog audit,
                        LongSupplier clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.hasher = hasher;
        this.audit = audit;
        this.clock = clock;
    }

    public static String randomPassword() {
        char[] chars = new char[PASSWORD_LENGTH];
        for (int i = 0; i < chars.length; i++) chars[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        return new String(chars);
    }

    public Result create(String actor, String name, String roleName) {
        return create(Actor.console(actor), name, roleName);
    }

    public Result reset(String actor, String name) {
        return reset(Actor.console(actor), name);
    }

    public Result delete(String actor, String name) {
        return delete(Actor.console(actor), name);
    }

    public synchronized Result create(Actor actor, String name, String roleName) {
        if (!Account.isValidName(name)) return log(actor, "user.create", Result.of(Code.INVALID_NAME, name));
        Role role = accounts.role(roleName);
        if (role == null) return log(actor, "user.create", new Result(Code.UNKNOWN_ROLE, name, roleName, null));
        if (!actor.mayGrant(role)) return log(actor, "user.create", Result.of(Code.FORBIDDEN, name));
        if (accounts.find(name).isPresent()) return log(actor, "user.create", Result.of(Code.EXISTS, name));
        String password = randomPassword();
        Account account = new Account(name, hash(password), role.name(), true, clock.getAsLong());
        try {
            accounts.put(account);
        } catch (IOException e) {
            return log(actor, "user.create", Result.of(Code.SAVE_FAILED, name));
        }
        return log(actor, "user.create", new Result(Code.CREATED, name, role.name(), password));
    }

    public synchronized Result reset(Actor actor, String name) {
        Optional<Account> found = accounts.find(name);
        if (found.isEmpty()) return log(actor, "user.reset", Result.of(Code.NOT_FOUND, name));
        Account account = found.get();
        if (!mayTouch(actor, account)) return log(actor, "user.reset", Result.of(Code.FORBIDDEN, account.name()));
        String password = randomPassword();
        try {
            accounts.put(account.withPassword(hash(password), true));
        } catch (IOException e) {
            return log(actor, "user.reset", Result.of(Code.SAVE_FAILED, account.name()));
        }
        if (sessions != null) sessions.removeUser(account.name(), null);
        return log(actor, "user.reset", new Result(Code.RESET, account.name(), account.role(), password));
    }

    public synchronized Result delete(Actor actor, String name) {
        Optional<Account> found = accounts.find(name);
        if (found.isEmpty()) return log(actor, "user.delete", Result.of(Code.NOT_FOUND, name));
        Account account = found.get();
        if (!mayTouch(actor, account)) return log(actor, "user.delete", Result.of(Code.FORBIDDEN, account.name()));
        if (accounts.isLastAdmin(account.name())) return log(actor, "user.delete", Result.of(Code.LAST_ADMIN, account.name()));
        try {
            accounts.remove(account.name());
        } catch (IOException e) {
            return log(actor, "user.delete", Result.of(Code.SAVE_FAILED, account.name()));
        }
        if (sessions != null) sessions.removeUser(account.name(), null);
        return log(actor, "user.delete", new Result(Code.DELETED, account.name(), account.role(), null));
    }

    public synchronized Result setRole(Actor actor, String name, String roleName) {
        Optional<Account> found = accounts.find(name);
        if (found.isEmpty()) return log(actor, "user.role", Result.of(Code.NOT_FOUND, name));
        Account account = found.get();
        Role role = accounts.role(roleName);
        if (role == null) return log(actor, "user.role", new Result(Code.UNKNOWN_ROLE, account.name(), roleName, null));
        if (!mayTouch(actor, account) || !actor.mayGrant(role)) {
            return log(actor, "user.role", Result.of(Code.FORBIDDEN, account.name()));
        }
        if (!role.name().equals(Role.ADMIN) && accounts.isLastAdmin(account.name())) {
            return log(actor, "user.role", Result.of(Code.LAST_ADMIN, account.name()));
        }
        try {
            accounts.put(account.withRole(role.name()));
        } catch (IOException e) {
            return log(actor, "user.role", Result.of(Code.SAVE_FAILED, account.name()));
        }
        return log(actor, "user.role", new Result(Code.ROLE_CHANGED, account.name(), role.name(), null));
    }

    private boolean mayTouch(Actor actor, Account account) {
        Role current = accounts.role(account.role());
        return current == null || actor.mayGrant(current);
    }

    public List<Account> list() {
        return accounts.accounts();
    }

    public List<String> roleNames() {
        return accounts.roles().stream().map(Role::name).toList();
    }

    private String hash(String password) {
        char[] chars = password.toCharArray();
        try {
            return hasher.hash(chars);
        } finally {
            PasswordHasher.wipe(chars);
        }
    }

    private Result log(Actor actor, String action, Result result) {
        if (audit != null) {
            String target = result.role() != null && action.equals("user.role") ? result.name() + " -> " + result.role() : result.name();
            audit.record(actor.name(), actor.ip(), action, target, result.code().name().toLowerCase(Locale.ROOT));
        }
        return result;
    }
}
