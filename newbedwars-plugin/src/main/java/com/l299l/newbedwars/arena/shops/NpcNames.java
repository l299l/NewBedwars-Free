package com.l299l.newbedwars.arena.shops;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.placeholder.PapiText;
import org.bukkit.entity.LivingEntity;

final class NpcNames {
    private NpcNames() {
    }

    static void apply(LivingEntity npc, String key, Team team) {
        npc.setCustomName(render(template(key), team));
    }

    static void refresh(LivingEntity npc, String key, Team team) {
        if (!PapiText.isEnabled()) return;
        String template = template(key);
        if (template.indexOf('%') < 0) return;
        String name = render(template, team);
        if (!name.equals(npc.getCustomName())) npc.setCustomName(name);
    }

    private static String template(String key) {
        return NewBedwars.plugin.getMessages().getMsgToConsole(key);
    }

    private static String render(String template, Team team) {
        return PapiText.applyServerWide(template).replace("/team/", team.getColor() + team.getDisplayName());
    }
}
