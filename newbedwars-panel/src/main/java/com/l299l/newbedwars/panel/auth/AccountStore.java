package com.l299l.newbedwars.panel.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class AccountStore {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Path file;
    private final SecretFile crypto;
    private final Map<String, Account> accounts = new TreeMap<>();
    private final Map<String, Role> roles = new TreeMap<>();

    private AccountStore(Path file, SecretFile crypto) {
        this.file = file;
        this.crypto = crypto;
    }

    public static AccountStore open(Path file, byte[] key) throws IOException {
        AccountStore store = new AccountStore(file, new SecretFile(key));
        if (Files.exists(file)) store.load();
        return store;
    }

    private void load() throws IOException {
        byte[] plain;
        try {
            plain = crypto.decrypt(Files.readAllBytes(file));
        } catch (GeneralSecurityException e) {
            throw new IOException(file.getFileName() + " can not be read with this key (wrong panel.key / NBW_PANEL_KEY, or the file was changed)");
        }
        JsonObject root = JsonParser.parseString(new String(plain, StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonElement e : root.getAsJsonArray("roles")) {
            JsonObject r = e.getAsJsonObject();
            Set<String> permissions = new TreeSet<>();
            for (JsonElement p : r.getAsJsonArray("permissions")) {
                if (Permission.isKnown(p.getAsString())) permissions.add(p.getAsString());
            }
            Role role = Role.custom(r.get("name").getAsString(), permissions);
            roles.put(role.name(), role);
        }
        for (JsonElement e : root.getAsJsonArray("accounts")) {
            JsonObject a = e.getAsJsonObject();
            Account account = new Account(a.get("name").getAsString(), a.get("hash").getAsString(),
                    a.get("role").getAsString(), a.get("mustChange").getAsBoolean(), a.get("created").getAsLong());
            accounts.put(account.key(), account);
        }
    }

    private void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray roleArray = new JsonArray();
        for (Role role : roles.values()) {
            JsonObject r = new JsonObject();
            r.addProperty("name", role.name());
            JsonArray permissions = new JsonArray();
            new TreeSet<>(role.permissions()).forEach(permissions::add);
            r.add("permissions", permissions);
            roleArray.add(r);
        }
        root.add("roles", roleArray);
        JsonArray accountArray = new JsonArray();
        for (Account account : accounts.values()) {
            JsonObject a = new JsonObject();
            a.addProperty("name", account.name());
            a.addProperty("hash", account.passwordHash());
            a.addProperty("role", account.role());
            a.addProperty("mustChange", account.mustChangePassword());
            a.addProperty("created", account.created());
            accountArray.add(a);
        }
        root.add("accounts", accountArray);
        try {
            SecretFile.writeAtomically(file, crypto.encrypt(GSON.toJson(root).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IOException("could not encrypt " + file.getFileName(), e);
        }
    }

    public synchronized Optional<Account> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(accounts.get(Account.key(name)));
    }

    public synchronized List<Account> accounts() {
        return new ArrayList<>(accounts.values());
    }

    public synchronized boolean isEmpty() {
        return accounts.isEmpty();
    }

    public synchronized void put(Account account) throws IOException {
        Role role = role(account.role());
        if (role == null) throw new IllegalArgumentException("unknown_role");
        if (!role.name().equals(account.role())) account = account.withRole(role.name());
        Account old = accounts.put(account.key(), account);
        try {
            save();
        } catch (IOException e) {
            if (old == null) accounts.remove(account.key());
            else accounts.put(old.key(), old);
            throw e;
        }
    }

    public synchronized boolean remove(String name) throws IOException {
        Account old = accounts.remove(Account.key(name));
        if (old == null) return false;
        try {
            save();
        } catch (IOException e) {
            accounts.put(old.key(), old);
            throw e;
        }
        return true;
    }

    public synchronized boolean isLastAdmin(String name) {
        Account account = accounts.get(Account.key(name));
        if (account == null || !Role.ADMIN.equals(account.role())) return false;
        return accounts.values().stream().filter(a -> Role.ADMIN.equals(a.role())).count() == 1;
    }

    public synchronized Role role(String name) {
        if (name == null) return null;
        for (Role role : Role.BUILT_IN) {
            if (role.name().equalsIgnoreCase(name)) return role;
        }
        return roles.get(name.toLowerCase(Locale.ROOT));
    }

    public synchronized List<Role> roles() {
        List<Role> all = new ArrayList<>(Role.BUILT_IN);
        all.addAll(roles.values());
        return all;
    }

    public synchronized void putRole(Role role) throws IOException {
        if (role.builtIn()) throw new IllegalArgumentException("built_in_role");
        Role old = roles.put(role.name(), role);
        try {
            save();
        } catch (IOException e) {
            if (old == null) roles.remove(role.name());
            else roles.put(role.name(), old);
            throw e;
        }
    }

    public synchronized boolean removeRole(String name) throws IOException {
        if (Role.isBuiltIn(name)) throw new IllegalArgumentException("built_in_role");
        String key = name.toLowerCase(Locale.ROOT);
        if (accounts.values().stream().anyMatch(a -> a.role().equals(key))) throw new IllegalStateException("role_in_use");
        Role old = roles.remove(key);
        if (old == null) return false;
        try {
            save();
        } catch (IOException e) {
            roles.put(key, old);
            throw e;
        }
        return true;
    }
}
