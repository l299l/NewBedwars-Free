package com.l299l.newbedwars.arena.shops.customitems;

public enum PurchaseResult {
    BOUGHT(null),
    NOT_ENOUGH("NotEnoughMoney"),
    NO_PERMISSION("ShopNoPermission"),
    ALREADY_OWNED("ShopAlreadyOwned"),
    MAXED("ShopUpgradeMaxed"),
    UNAVAILABLE("ShopUnavailable"),
    CANCELLED(null);

    private final String messageKey;

    PurchaseResult(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }

    public boolean bought() {
        return this == BOUGHT;
    }
}
