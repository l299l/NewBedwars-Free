package com.l299l.newbedwars.parties;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PartyTest {

    @Test
    void constructorSetsAdminAndAddsToMembers() {
        UUID id = UUID.randomUUID();
        Party party = new Party(id);
        assertEquals(id, party.getAdmin());
        assertTrue(party.contains(id));
        assertEquals(1, party.size());
    }

    @Test
    void addMemberIncreasesSize() {
        UUID admin = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(admin);
        party.addMember(member);
        assertEquals(2, party.size());
        assertTrue(party.contains(member));
    }

    @Test
    void addMemberIgnoresDuplicate() {
        UUID admin = UUID.randomUUID();
        Party party = new Party(admin);
        party.addMember(admin);
        assertEquals(1, party.size());
    }

    @Test
    void removeMemberDecreasesSize() {
        UUID admin = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(admin);
        party.addMember(member);
        assertTrue(party.removeMember(member));
        assertEquals(1, party.size());
        assertFalse(party.contains(member));
    }

    @Test
    void removeMemberReturnsFalseWhenAbsent() {
        UUID admin = UUID.randomUUID();
        Party party = new Party(admin);
        assertFalse(party.removeMember(UUID.randomUUID()));
    }

    @Test
    void isAdminTrueForAdmin() {
        UUID admin = UUID.randomUUID();
        Party party = new Party(admin);
        assertTrue(party.isAdmin(admin));
    }

    @Test
    void isAdminFalseForMember() {
        UUID admin = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(admin);
        party.addMember(member);
        assertFalse(party.isAdmin(member));
    }

    @Test
    void setAdminChangesAdmin() {
        UUID admin = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(admin);
        party.addMember(member);
        party.setAdmin(member);
        assertEquals(member, party.getAdmin());
        assertTrue(party.isAdmin(member));
        assertFalse(party.isAdmin(admin));
    }

    @Test
    void sizeAccurateAfterMultipleOperations() {
        UUID admin = UUID.randomUUID();
        Party party = new Party(admin);
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        party.addMember(m1);
        party.addMember(m2);
        assertEquals(3, party.size());
        party.removeMember(m1);
        assertEquals(2, party.size());
        party.removeMember(m2);
        assertEquals(1, party.size());
    }
}
