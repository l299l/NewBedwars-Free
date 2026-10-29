package com.l299l.newbedwars.utils;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamColorTest {

    @Test
    void everyChatColorGivesABlockOfEveryKind() {
        for (ChatColor color : ChatColor.values()) {
            for (TeamColor.Kind kind : TeamColor.Kind.values()) {
                assertNotNull(TeamColor.block(color, kind), color + " " + kind);
            }
        }
        assertNotNull(TeamColor.block(null, TeamColor.Kind.WOOL));
    }

    @Test
    void woolMatchesTheSetupConventions() {
        assertEquals(Material.RED_WOOL, TeamColor.wool(ChatColor.RED));
        assertEquals(Material.RED_WOOL, TeamColor.wool(ChatColor.DARK_RED));
        assertEquals(Material.BLUE_WOOL, TeamColor.wool(ChatColor.DARK_BLUE));
        assertEquals(Material.BLUE_WOOL, TeamColor.wool(ChatColor.BLUE));
        assertEquals(Material.LIME_WOOL, TeamColor.wool(ChatColor.GREEN));
        assertEquals(Material.GREEN_WOOL, TeamColor.wool(ChatColor.DARK_GREEN));
        assertEquals(Material.LIGHT_BLUE_WOOL, TeamColor.wool(ChatColor.AQUA));
        assertEquals(Material.CYAN_WOOL, TeamColor.wool(ChatColor.DARK_AQUA));
        assertEquals(Material.ORANGE_WOOL, TeamColor.wool(ChatColor.GOLD));
        assertEquals(Material.PINK_WOOL, TeamColor.wool(ChatColor.LIGHT_PURPLE));
        assertEquals(Material.PURPLE_WOOL, TeamColor.wool(ChatColor.DARK_PURPLE));
        assertEquals(Material.LIGHT_GRAY_WOOL, TeamColor.wool(ChatColor.GRAY));
        assertEquals(Material.GRAY_WOOL, TeamColor.wool(ChatColor.DARK_GRAY));
        assertEquals(Material.GRAY_WOOL, TeamColor.wool(null));
    }

    @Test
    void otherBlockFamiliesUseTheSameDye() {
        assertEquals(Material.RED_TERRACOTTA, TeamColor.block(ChatColor.RED, TeamColor.Kind.TERRACOTTA));
        assertEquals(Material.RED_STAINED_GLASS, TeamColor.block(ChatColor.RED, TeamColor.Kind.GLASS));
        assertEquals(Material.RED_STAINED_GLASS_PANE, TeamColor.block(ChatColor.RED, TeamColor.Kind.GLASS_PANE));
        assertEquals(Material.RED_CONCRETE, TeamColor.block(ChatColor.RED, TeamColor.Kind.CONCRETE));
        assertEquals(Material.RED_CONCRETE_POWDER, TeamColor.block(ChatColor.RED, TeamColor.Kind.CONCRETE_POWDER));
        assertEquals(Material.RED_CARPET, TeamColor.block(ChatColor.RED, TeamColor.Kind.CARPET));
        assertEquals(Material.RED_BED, TeamColor.block(ChatColor.RED, TeamColor.Kind.BED));
    }

    @Test
    void leatherAndFireworkColorsAreDistinctPerDye() {
        assertEquals(DyeColor.LIME.getColor(), TeamColor.leather(ChatColor.GREEN));
        assertNotEquals(TeamColor.leather(ChatColor.GREEN), TeamColor.leather(ChatColor.DARK_GREEN));
        assertEquals(DyeColor.ORANGE.getFireworkColor(), TeamColor.firework(ChatColor.GOLD));
    }

    @Test
    void parseKnowsDyeNamesAndChatColorNames() {
        assertEquals(ChatColor.DARK_BLUE, TeamColor.parse("BLUE"));
        assertEquals(ChatColor.GREEN, TeamColor.parse("LIME"));
        assertEquals(ChatColor.DARK_GREEN, TeamColor.parse("GREEN"));
        assertEquals(ChatColor.LIGHT_PURPLE, TeamColor.parse("pink"));
        assertEquals(ChatColor.GRAY, TeamColor.parse("LIGHT_GRAY"));
        assertEquals(ChatColor.DARK_GRAY, TeamColor.parse("GRAY"));
        assertEquals(ChatColor.RED, TeamColor.parse("RED"));
        assertEquals(ChatColor.AQUA, TeamColor.parse("AQUA"));
    }

    @Test
    void parseRejectsUnknownNamesAndFormats() {
        assertThrows(IllegalArgumentException.class, () -> TeamColor.parse("NOPE"));
        assertThrows(IllegalArgumentException.class, () -> TeamColor.parse("BOLD"));
    }

    @Test
    void parsedColorsRoundTripToTheSameDye() {
        for (String name : new String[]{"RED", "BLUE", "LIME", "GREEN", "YELLOW", "CYAN", "LIGHT_BLUE", "PINK", "PURPLE",
                "ORANGE", "GRAY", "LIGHT_GRAY", "BLACK", "WHITE"}) {
            DyeColor dye = TeamColor.dye(TeamColor.parse(name));
            assertEquals(DyeColor.valueOf(name), dye, name);
        }
    }

    @Test
    void undyedKindRecognisesEveryWhiteFamily() {
        assertEquals(TeamColor.Kind.WOOL, TeamColor.undyedKind(Material.WHITE_WOOL));
        assertEquals(TeamColor.Kind.TERRACOTTA, TeamColor.undyedKind(Material.WHITE_TERRACOTTA));
        assertEquals(TeamColor.Kind.TERRACOTTA, TeamColor.undyedKind(Material.TERRACOTTA));
        assertEquals(TeamColor.Kind.GLASS, TeamColor.undyedKind(Material.WHITE_STAINED_GLASS));
        assertEquals(TeamColor.Kind.GLASS_PANE, TeamColor.undyedKind(Material.WHITE_STAINED_GLASS_PANE));
        assertEquals(TeamColor.Kind.CONCRETE, TeamColor.undyedKind(Material.WHITE_CONCRETE));
        assertEquals(TeamColor.Kind.CONCRETE_POWDER, TeamColor.undyedKind(Material.WHITE_CONCRETE_POWDER));
        assertEquals(TeamColor.Kind.CARPET, TeamColor.undyedKind(Material.WHITE_CARPET));
        assertEquals(TeamColor.Kind.BED, TeamColor.undyedKind(Material.WHITE_BED));
    }

    @Test
    void undyedKindIgnoresEverythingElse() {
        assertNull(TeamColor.undyedKind(Material.RED_WOOL));
        assertNull(TeamColor.undyedKind(Material.WHITE_GLAZED_TERRACOTTA));
        assertNull(TeamColor.undyedKind(Material.WHITE_BANNER));
        assertNull(TeamColor.undyedKind(Material.OAK_PLANKS));
        assertNull(TeamColor.undyedKind(null));
    }

    @Test
    void recolorTurnsWhiteBlocksIntoTheTeamColor() {
        assertEquals(Material.LIME_WOOL, TeamColor.recolor(Material.WHITE_WOOL, ChatColor.GREEN));
        assertEquals(Material.BLUE_CONCRETE, TeamColor.recolor(Material.WHITE_CONCRETE, ChatColor.DARK_BLUE));
        assertEquals(Material.RED_STAINED_GLASS_PANE, TeamColor.recolor(Material.WHITE_STAINED_GLASS_PANE, ChatColor.RED));
        assertEquals(Material.WHITE_WOOL, TeamColor.recolor(Material.WHITE_WOOL, ChatColor.WHITE));
    }

    @Test
    void recolorLeavesOtherBlocksAlone() {
        assertEquals(Material.OAK_PLANKS, TeamColor.recolor(Material.OAK_PLANKS, ChatColor.RED));
        assertEquals(Material.RED_TERRACOTTA, TeamColor.recolor(Material.TERRACOTTA, ChatColor.RED));
        assertEquals(Material.WHITE_GLAZED_TERRACOTTA, TeamColor.recolor(Material.WHITE_GLAZED_TERRACOTTA, ChatColor.RED));
    }

    @Test
    void kindOfRecognisesDyedBlocksInAnyColor() {
        assertEquals(TeamColor.Kind.WOOL, TeamColor.kindOf(Material.PINK_WOOL));
        assertEquals(TeamColor.Kind.GLASS, TeamColor.kindOf(Material.LIGHT_BLUE_STAINED_GLASS));
        assertEquals(TeamColor.Kind.GLASS_PANE, TeamColor.kindOf(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        assertEquals(TeamColor.Kind.CONCRETE_POWDER, TeamColor.kindOf(Material.BLACK_CONCRETE_POWDER));
        assertEquals(TeamColor.Kind.TERRACOTTA, TeamColor.kindOf(Material.ORANGE_TERRACOTTA));
        assertEquals(TeamColor.Kind.BED, TeamColor.kindOf(Material.RED_BED));
    }

    @Test
    void kindOfIgnoresNonDyedBlocks() {
        assertNull(TeamColor.kindOf(Material.TERRACOTTA));
        assertNull(TeamColor.kindOf(Material.GLASS));
        assertNull(TeamColor.kindOf(Material.RED_GLAZED_TERRACOTTA));
        assertNull(TeamColor.kindOf(Material.STONE));
        assertNull(TeamColor.kindOf(null));
    }

    @Test
    void everyGeneratedBlockIsRecognisedAgain() {
        for (ChatColor color : ChatColor.values()) {
            if (!color.isColor()) continue;
            for (TeamColor.Kind kind : TeamColor.Kind.values()) {
                assertEquals(kind, TeamColor.kindOf(TeamColor.block(color, kind)), color + " " + kind);
            }
        }
    }
}
