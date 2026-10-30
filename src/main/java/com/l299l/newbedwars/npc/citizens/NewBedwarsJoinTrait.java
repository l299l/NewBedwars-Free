package com.l299l.newbedwars.npc.citizens;

import com.l299l.newbedwars.npc.JoinNpcAction;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

/** Citizens trait holding a join NPC's action; Citizens saves it with the NPC. */
public class NewBedwarsJoinTrait extends Trait {
    public static final String NAME = "newbedwarsjoin";

    private String action;
    private String target;

    public NewBedwarsJoinTrait() {
        super(NAME);
    }

    @Override
    public void load(DataKey key) {
        action = key.getString("action");
        target = key.getString("target");
        if (target != null && target.isEmpty()) target = null;
    }

    @Override
    public void save(DataKey key) {
        if (action == null) {
            key.removeKey("action");
            key.removeKey("target");
            return;
        }
        key.setString("action", action);
        if (target != null) {
            key.setString("target", target);
        } else {
            key.removeKey("target");
        }
    }

    /** The action, or null when none (or an invalid one) is stored. */
    public JoinNpcAction getAction() {
        return JoinNpcAction.tryParse(action, target);
    }

    /** Forgets the action, so a removed trait that Citizens still hands out does nothing. */
    public void clearAction() {
        action = null;
        target = null;
    }

    public void setAction(JoinNpcAction joinAction) {
        action = joinAction.getType().name();
        target = joinAction.getTarget();
    }
}
