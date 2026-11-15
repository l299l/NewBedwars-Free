package com.l299l.newbedwars.arena.setup.auto;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class ScanData {

    public enum Block { DIAMOND, EMERALD, IRON, GOLD }

    public record Bed(Pos pos, ChatColor color) {}

    public record GenBlock(Pos pos, Block type) {}

    private final List<Bed> beds = new ArrayList<>();
    private final List<GenBlock> blocks = new ArrayList<>();
    private final List<SignMark> signs = new ArrayList<>();
    private Integer lowestSolidY;

    public List<Bed> beds() {
        return beds;
    }

    public List<GenBlock> blocks() {
        return blocks;
    }

    public List<SignMark> signs() {
        return signs;
    }

    public Integer lowestSolidY() {
        return lowestSolidY;
    }

    public void solidAt(int y) {
        if (lowestSolidY == null || y < lowestSolidY) lowestSolidY = y;
    }

    public void merge(ScanData other) {
        beds.addAll(other.beds);
        blocks.addAll(other.blocks);
        signs.addAll(other.signs);
        if (other.lowestSolidY != null) solidAt(other.lowestSolidY);
    }
}
