package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.shop.ItemTexts;
import com.l299l.newbedwars.api.shop.ShopEditor;
import com.l299l.newbedwars.api.shop.ShopGui;
import com.l299l.newbedwars.api.shop.ShopItem;
import com.l299l.newbedwars.api.shop.ShopOptions;
import com.l299l.newbedwars.api.shop.ShopResult;
import com.l299l.newbedwars.arena.shops.editor.ShopEditService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ApiShopEditor implements ShopEditor {

    private static <T> CompletableFuture<T> run(Supplier<T> action) {
        return ApiMaintenance.onMain(() -> CompletableFuture.completedFuture(action.get()));
    }

    private static CompletableFuture<ShopResult> change(Supplier<ShopResult> action) {
        return run(() -> {
            try {
                return action.get();
            } catch (RuntimeException e) {
                com.l299l.newbedwars.NewBedwars.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Shop editor change failed", e);
                return new ShopResult(ShopResult.Code.FAILED, String.valueOf(e.getMessage()));
            }
        });
    }

    @Override
    public CompletableFuture<List<ShopItem>> getItems() {
        return run(ShopEditService::listItems);
    }

    @Override
    public CompletableFuture<Optional<ShopItem>> getItem(String name) {
        return run(() -> ShopEditService.item(name));
    }

    @Override
    public CompletableFuture<ShopResult> saveItem(ShopItem item) {
        return change(() -> ShopEditService.saveItem(item));
    }

    @Override
    public CompletableFuture<ShopResult> deleteItem(String name) {
        return change(() -> ShopEditService.deleteItem(name));
    }

    @Override
    public CompletableFuture<Optional<Map<String, ItemTexts>>> getItemTexts(String name) {
        return run(() -> ShopEditService.texts(name));
    }

    @Override
    public CompletableFuture<ShopResult> saveItemTexts(String name, Map<String, ItemTexts> texts) {
        return change(() -> ShopEditService.saveTexts(name, texts));
    }

    @Override
    public CompletableFuture<List<ShopGui>> getGuis() {
        return run(ShopEditService::listGuis);
    }

    @Override
    public CompletableFuture<Optional<ShopGui>> getGui(String id) {
        return run(() -> ShopEditService.gui(id));
    }

    @Override
    public CompletableFuture<ShopResult> createGui(ShopGui gui) {
        return change(() -> ShopEditService.createGui(gui));
    }

    @Override
    public CompletableFuture<ShopResult> saveGui(ShopGui gui) {
        return change(() -> ShopEditService.saveGui(gui));
    }

    @Override
    public CompletableFuture<ShopResult> deleteGui(String id) {
        return change(() -> ShopEditService.deleteGui(id));
    }

    @Override
    public CompletableFuture<List<com.l299l.newbedwars.api.shop.ArenaShops>> getArenaShops() {
        return run(ShopEditService::arenaShops);
    }

    @Override
    public CompletableFuture<ShopResult> setArenaShops(String arena, String shopGui, String upgradeGui) {
        return change(() -> ShopEditService.setArenaShops(arena, shopGui, upgradeGui));
    }

    @Override
    public CompletableFuture<ShopOptions> getOptions() {
        return run(ShopEditService::options);
    }
}
