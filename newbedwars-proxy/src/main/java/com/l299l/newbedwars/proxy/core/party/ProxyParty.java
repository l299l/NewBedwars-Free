package com.l299l.newbedwars.proxy.core.party;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ProxyParty {
    private final String id;
    private UUID admin;
    private final Set<UUID> members = new LinkedHashSet<>();

    public ProxyParty(String id, UUID admin) {
        this.id = id;
        this.admin = admin;
        members.add(admin);
    }

    public String id() { return id; }
    public UUID admin() { return admin; }
    public boolean isAdmin(UUID p) { return admin.equals(p); }
    void setAdmin(UUID p) { admin = p; }
    public List<UUID> members() { return new ArrayList<>(members); }
    public boolean contains(UUID p) { return members.contains(p); }
    public int size() { return members.size(); }
    void add(UUID p) { members.add(p); }
    void remove(UUID p) { members.remove(p); }
}
