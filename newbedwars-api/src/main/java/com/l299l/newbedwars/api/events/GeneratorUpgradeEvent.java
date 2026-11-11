package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.GeneratorKind;
import org.bukkit.event.HandlerList;

/** Fired when a game phase upgrades the diamond or emerald generators. Available from 2.7-beta. */
public class GeneratorUpgradeEvent extends ArenaEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GeneratorKind kind;
    private final int level;

    public GeneratorUpgradeEvent(ArenaView arena, GeneratorKind kind, int level) {
        super(arena);
        this.kind = kind;
        this.level = level;
    }

    /** Which generators were upgraded. */
    public GeneratorKind getKind() {
        return kind;
    }

    /** The new level (tier). */
    public int getLevel() {
        return level;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
