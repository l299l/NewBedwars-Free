package com.l299l.newbedwars.npc.citizens;

import com.l299l.newbedwars.npc.JoinNpcAction;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

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

    public JoinNpcAction getAction() {
        return JoinNpcAction.tryParse(action, target);
    }

    public void clearAction() {
        action = null;
        target = null;
    }

    public void setAction(JoinNpcAction joinAction) {
        action = joinAction.getType().name();
        target = joinAction.getTarget();
    }
}
