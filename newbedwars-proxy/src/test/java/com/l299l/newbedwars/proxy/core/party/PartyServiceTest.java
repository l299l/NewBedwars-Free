package com.l299l.newbedwars.proxy.core.party;

import com.l299l.newbedwars.proxy.core.FakePlatform;

import com.l299l.newbedwars.common.protocol.MessageType;
import com.l299l.newbedwars.common.protocol.payload.PartySync;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PartyServiceTest {
    FakePlatform pf;
    PartyService parties;
    UUID leo, bot, cat;

    @BeforeEach
    void setUp() {
        pf = new FakePlatform();
        parties = new PartyService(pf, 60_000);
        leo = pf.add("Leo", "lobby");
        bot = pf.add("Bot", "lobby");
        cat = pf.add("Cat", "game1");
    }

    private void party(UUID admin, UUID... members) {
        for (UUID m : members) {
            parties.invite(admin, pf.nameOf(m), 0);
            parties.accept(m);
        }
    }

    @Test
    void inviteAcceptCreatesPartyAndSyncsAcrossServers() {
        parties.invite(leo, "bot", 0);
        assertTrue(pf.has("Leo:PartyInviteSent{/player/=Bot}"));
        assertTrue(pf.has("Bot:PartyInviteReceived{/player/=Leo}"));
        assertTrue(pf.has("Bot:buttons"));
        parties.accept(bot);
        ProxyParty p = parties.partyOf(bot);
        assertNotNull(p);
        assertSame(p, parties.partyOf(leo));
        assertTrue(p.isAdmin(leo));
        assertTrue(pf.has("Bot:PartyAccepted{/player/=Leo}"));
        assertTrue(pf.has("Leo:PartyMemberJoined{/player/=Bot}"));
        // last sync: one message per server (both on lobby)
        PartySync last = pf.toPlayerServer.get(pf.toPlayerServer.size() - 1).payload(PartySync.class);
        assertEquals(2, last.members.size());
        assertEquals(leo, last.admin);
        assertEquals("Bot", last.members.get(1).name);
    }

    @Test
    void inviteChecks() {
        parties.invite(leo, "leo", 0);
        assertTrue(pf.has("Leo:PartyCantInviteSelf"));
        parties.invite(leo, "nobody", 0);
        assertTrue(pf.has("Leo:PartyPlayerNotFound{/player/=nobody}"));
        party(leo, bot);
        parties.invite(bot, "Cat", 0);
        assertTrue(pf.has("Bot:PartyNotAdmin"));
        parties.invite(cat, "Bot", 0);
        assertTrue(pf.has("Cat:PartyPlayerAlreadyInParty{/player/=Bot}"));
    }

    @Test
    void denyAndExpiry() {
        parties.invite(leo, "Bot", 0);
        parties.deny(bot);
        assertTrue(pf.has("Bot:PartyDenied{/player/=Leo}"));
        assertTrue(pf.has("Leo:PartyMemberDenied{/player/=Bot}"));
        parties.accept(bot);
        assertTrue(pf.has("Bot:PartyNoPendingInvite"));

        parties.invite(leo, "Cat", 0);
        parties.expireInvites(59_999);
        assertFalse(pf.has("Cat:PartyInviteExpired{/player/=Leo}"));
        parties.expireInvites(60_000);
        assertTrue(pf.has("Cat:PartyInviteExpired{/player/=Leo}"));
    }

    @Test
    void leaveOfTwoDisbands() {
        party(leo, bot);
        pf.toPlayerServer.clear();
        parties.leave(bot);
        assertNull(parties.partyOf(leo));
        assertNull(parties.partyOf(bot));
        assertTrue(pf.has("Leo:PartyDisbanded"));
        PartySync s = pf.toPlayerServer.get(0).payload(PartySync.class);
        assertTrue(s.disbanded);
        assertTrue(s.removed.contains(leo) && s.removed.contains(bot));
    }

    @Test
    void adminLeavingHandsOverAdmin() {
        party(leo, bot, cat);
        parties.leave(leo);
        ProxyParty p = parties.partyOf(bot);
        assertTrue(p.isAdmin(bot));
        assertTrue(pf.has("Bot:PartyYouAreAdmin"));
        assertTrue(pf.has("Cat:PartyMemberLeft{/player/=Leo}"));
        // sync went to lobby (Bot) and game1 (Cat)
        assertEquals(MessageType.PARTY_SYNC, pf.toPlayerServer.get(pf.toPlayerServer.size() - 1).type());
    }

    @Test
    void kickAndTransfer() {
        party(leo, bot, cat);
        parties.kick(bot, "Cat");
        assertTrue(pf.has("Bot:PartyNotAdmin"));
        parties.kick(leo, "Leo");
        assertTrue(pf.has("Leo:PartyCantKickSelf"));
        parties.kick(leo, "nobody");
        assertTrue(pf.has("Leo:PartyNotFound{/player/=nobody}"));
        parties.kick(leo, "cat");
        assertNull(parties.partyOf(cat));
        assertTrue(pf.has("Cat:PartyYouWereKicked"));
        parties.transfer(leo, "Bot");
        assertTrue(parties.partyOf(leo).isAdmin(bot));
        assertTrue(pf.has("Bot:PartyYouAreAdmin"));
        assertTrue(pf.has("Leo:PartyAdminTransferred{/player/=Bot}"));
    }

    @Test
    void listShowsAdminTag() {
        party(leo, bot);
        parties.list(bot);
        assertTrue(pf.has("Bot:PartyList{/size/=2}"));
        assertTrue(pf.has("Bot:PartyListMember{/admin/=%PartyListAdminTag%, /player/=Leo}"));
        assertTrue(pf.has("Bot:PartyListMember{/admin/=, /player/=Bot}"));
    }

    @Test
    void disconnectKeepsPartyWhileSomeoneIsOnline() {
        party(leo, bot);
        pf.names.remove(leo);
        parties.onDisconnect(leo);
        assertNotNull(parties.partyOf(bot));
        assertTrue(parties.partyOf(bot).isAdmin(bot));
        pf.names.remove(bot);
        parties.onDisconnect(bot);
        assertNull(parties.partyOf(bot));
        assertNull(parties.partyOf(leo));
    }

    @Test
    void serverSwitchResendsParty() {
        party(leo, bot);
        pf.toPlayerServer.clear();
        pf.toPlayerServerVia.clear();
        parties.onServerSwitch(bot);
        assertEquals(1, pf.toPlayerServer.size());
        assertEquals(bot, pf.toPlayerServerVia.get(0));
        parties.onServerSwitch(cat);
        assertEquals(1, pf.toPlayerServer.size());
    }
}
