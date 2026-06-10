package com.l299l.newbedwars.arena.shops;

public enum Upgrade {
    PROTECTION(5), SHARPNESS(4), HASTE(2), FORGE(8), HEALPOOL(1), ALARMTRAP(2), BLINDTRAP(2), MININGFATIGUETRAP(2), CUSTOMTRAP(0), CUSTOMUPGRADE(0), DRAGONBUFF(2);

    public final int maxLevel;

    Upgrade(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}
