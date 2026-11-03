package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartySync {
    public static class Member {
        public UUID uuid;
        public String name;

        public Member() {}

        public Member(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public String partyId;
    public UUID admin;
    public List<Member> members = new ArrayList<>();

    public boolean disbanded;

    public List<UUID> removed = new ArrayList<>();

    public PartySync() {}

    public PartySync(String partyId, UUID admin, List<Member> members, boolean disbanded, List<UUID> removed) {
        this.partyId = partyId;
        this.admin = admin;
        this.members = members == null ? new ArrayList<>() : members;
        this.disbanded = disbanded;
        this.removed = removed == null ? new ArrayList<>() : removed;
    }
}
