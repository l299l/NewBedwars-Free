package com.l299l.newbedwars.parties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Party {
    private UUID admin;
    private final List<UUID> members;

    public Party(UUID admin) {
        this.admin = admin;
        this.members = new ArrayList<>();
        this.members.add(admin);
    }

    public UUID getAdmin() {
        return admin;
    }

    public void setAdmin(UUID admin) {
        this.admin = admin;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public boolean isAdmin(UUID uuid) {
        return admin.equals(uuid);
    }

    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) {
            members.add(uuid);
        }
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public int size() {
        return members.size();
    }

    public boolean contains(UUID uuid) {
        return members.contains(uuid);
    }
}
