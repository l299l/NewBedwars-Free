package com.l299l.newbedwars.api.events;

import com.l299l.newbedwars.api.view.ArenaView;
import org.bukkit.event.Cancellable;

/**
 * Base of arena events that can be cancelled. Subclasses still declare their own static {@code HANDLERS} and
 * {@code getHandlerList()}, because Bukkit needs one handler list per event class.
 */
public abstract class CancellableArenaEvent extends ArenaEvent implements Cancellable {
    private boolean cancelled;

    protected CancellableArenaEvent(ArenaView arena) {
        super(arena);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
