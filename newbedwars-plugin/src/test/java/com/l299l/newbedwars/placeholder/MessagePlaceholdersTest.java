package com.l299l.newbedwars.placeholder;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagePlaceholdersTest {
    private final Player viewer = named("Reader");
    private final Player victim = named("Vic");
    private final Player killer = named("Kil");

    private static Player named(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        return player;
    }

    @BeforeEach
    void setUp() {
        PapiText.use((player, text) -> text.replace("%lp_prefix%", "[" + player.getName() + "]"));
    }

    @AfterEach
    void tearDown() {
        PapiText.disable();
    }

    @Test
    void plainPlaceholdersFollowTheSubjectOfTheMessage() {
        assertEquals("[Reader]", MessagePlaceholders.apply(viewer, MessageContext.viewer(), "%lp_prefix%"));
        assertEquals("[Vic]", MessagePlaceholders.apply(viewer, MessageContext.about(victim), "%lp_prefix%"));
    }

    @Test
    void targetsChooseAnotherPlayer() {
        assertEquals("[Vic] by [Kil] seen by [Reader]", MessagePlaceholders.apply(viewer,
                MessageContext.kill(victim, killer),
                "%victim:lp_prefix% by %killer:lp_prefix% seen by %viewer:lp_prefix%"));
        assertEquals("[Reader] [Vic]", MessagePlaceholders.apply(viewer, MessageContext.about(victim),
                "%viewer:lp_prefix% %lp_prefix%"));
    }

    @Test
    void killerPlaceholderIsRemovedWhenNoPlayerKilledTheVictim() {
        assertEquals("[Vic] by /killer/", MessagePlaceholders.apply(viewer, MessageContext.kill(victim, null),
                "%victim:lp_prefix% by %killer:lp_prefix%/killer/"));
    }

    @Test
    void targetsThatTheMessageDoesNotHaveStayAsText() {
        assertEquals("%killer:lp_prefix% [Vic]", MessagePlaceholders.apply(viewer, MessageContext.about(victim),
                "%killer:lp_prefix% %lp_prefix%"));
    }

    @Test
    void placeholdersOfAnOfflineSubjectAreRemovedInsteadOfShowingTheReaders() {
        assertEquals("/player/ 50% /percent/%", MessagePlaceholders.apply(viewer, MessageContext.about(null),
                "%lp_prefix%/player/ 50% /percent/%"));
    }

    @Test
    void resultsOfTargetedPlaceholdersAreNotEvaluatedAgain() {
        PapiText.use((player, text) -> text.equals("%lp_nick%") ? "%lp_prefix%"
                : text.replace("%lp_prefix%", "evaluated"));
        assertEquals("%lp_prefix%", MessagePlaceholders.apply(viewer, MessageContext.about(victim),
                "%viewer:lp_nick%"));
    }

    @Test
    void messagesAreUnchangedWithoutPlaceholderApi() {
        PapiText.disable();
        assertEquals("%viewer:lp_prefix% %lp_prefix%", MessagePlaceholders.apply(viewer,
                MessageContext.about(null), "%viewer:lp_prefix% %lp_prefix%"));
    }
}
