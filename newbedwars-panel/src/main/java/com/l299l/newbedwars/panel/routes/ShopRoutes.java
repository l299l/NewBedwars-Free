package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.shop.ArenaShops;
import com.l299l.newbedwars.api.shop.ItemTexts;
import com.l299l.newbedwars.api.shop.ShopCategory;
import com.l299l.newbedwars.api.shop.ShopEditor;
import com.l299l.newbedwars.api.shop.ShopGui;
import com.l299l.newbedwars.api.shop.ShopItem;
import com.l299l.newbedwars.api.shop.ShopOptions;
import com.l299l.newbedwars.api.shop.ShopResult;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ShopRoutes {
    private static final int MAX_LIST = 64;

    private final ShopEditor shop;
    private final AuditLog audit;

    public ShopRoutes(ShopEditor shop, AuditLog audit) {
        this.shop = shop;
        this.audit = audit;
    }

    public void register(Router router) {
        Set<String> edit = Set.of(Permission.SHOP_EDIT);
        router.allowed("GET", "/api/shop/options", edit, r -> Response.ok(options(await(shop.getOptions()))));
        router.allowed("GET", "/api/shop/items", edit, this::items);
        router.allowed("GET", "/api/shop/items/{name}", edit, this::item);
        router.allowed("POST", "/api/shop/items/{name}", edit, this::saveItem);
        router.allowed("POST", "/api/shop/items/{name}/texts", edit, this::saveTexts);
        router.allowed("POST", "/api/shop/items/{name}/delete", edit, this::deleteItem);
        router.allowed("GET", "/api/shop/guis", edit, this::guis);
        router.allowed("GET", "/api/shop/guis/{id}", edit, this::gui);
        router.allowed("POST", "/api/shop/guis", edit, this::createGui);
        router.allowed("POST", "/api/shop/guis/{id}", edit, this::saveGui);
        router.allowed("POST", "/api/shop/guis/{id}/delete", edit, this::deleteGui);
        router.allowed("GET", "/api/shop/arenas", edit, this::arenas);
        router.allowed("POST", "/api/shop/arenas/{name}", edit, this::setArena);
    }

    private Response items(Request r) throws Exception {
        JsonArray out = new JsonArray();
        for (ShopItem item : await(shop.getItems())) out.add(item(item));
        JsonObject body = new JsonObject();
        body.add("items", out);
        return Response.ok(body);
    }

    private Response item(Request r) throws Exception {
        String name = r.param("name");
        Optional<ShopItem> item = await(shop.getItem(name));
        if (item.isEmpty()) throw new HttpError(404, "not_found");
        Optional<Map<String, ItemTexts>> texts = await(shop.getItemTexts(name));
        JsonObject body = new JsonObject();
        body.add("item", item(item.get()));
        JsonObject t = new JsonObject();
        texts.ifPresent(map -> map.forEach((lang, value) -> {
            JsonObject o = new JsonObject();
            o.addProperty("name", value.name());
            o.addProperty("iconDescription", value.iconDescription());
            o.addProperty("lore", value.lore());
            t.add(lang, o);
        }));
        body.add("texts", t);
        return Response.ok(body);
    }

    private Response saveItem(Request r) throws Exception {
        JsonObject j = r.json();
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        JsonElement e = j.get("enchantments");
        if (e != null && e.isJsonObject()) {
            if (e.getAsJsonObject().size() > MAX_LIST) throw new HttpError(400, "too_many");
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                enchantments.put(en.getKey(), integer(en.getValue()));
            }
        }
        ShopItem item = new ShopItem(r.param("name"), str(j, "material"), integer(j.get("amount")), integer(j.get("price")),
                str(j, "priceType"), str(j, "onBuy"), str(j, "logic"), bool(j, "permanent"), str(j, "permission"),
                enchantments, strings(j.get("flags")), List.of());
        return result(r, "shop.item.save", r.param("name"), await(shop.saveItem(item)));
    }

    private Response saveTexts(Request r) throws Exception {
        JsonObject j = r.json();
        JsonElement texts = j.get("texts");
        if (texts == null || !texts.isJsonObject()) throw new HttpError(400, "missing_texts");
        Map<String, ItemTexts> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : texts.getAsJsonObject().entrySet()) {
            if (!e.getValue().isJsonObject() || map.size() >= MAX_LIST) throw new HttpError(400, "bad_texts");
            JsonObject t = e.getValue().getAsJsonObject();
            map.put(e.getKey(), new ItemTexts(str(t, "name"), str(t, "iconDescription"), str(t, "lore")));
        }
        return result(r, "shop.item.texts", r.param("name"), await(shop.saveItemTexts(r.param("name"), map)));
    }

    private Response deleteItem(Request r) throws Exception {
        return result(r, "shop.item.delete", r.param("name"), await(shop.deleteItem(r.param("name"))));
    }

    private Response guis(Request r) throws Exception {
        JsonArray out = new JsonArray();
        for (ShopGui gui : await(shop.getGuis())) out.add(gui(gui));
        JsonObject body = new JsonObject();
        body.add("guis", out);
        return Response.ok(body);
    }

    private Response gui(Request r) throws Exception {
        Optional<ShopGui> gui = await(shop.getGui(r.param("id")));
        if (gui.isEmpty()) throw new HttpError(404, "not_found");
        JsonObject body = new JsonObject();
        body.add("gui", gui(gui.get()));
        return Response.ok(body);
    }

    private Response createGui(Request r) throws Exception {
        ShopGui gui = parseGui(r.json(), str(r.json(), "id"));
        return result(r, "shop.gui.create", gui.id(), await(shop.createGui(gui)));
    }

    private Response saveGui(Request r) throws Exception {
        ShopGui gui = parseGui(r.json(), r.param("id"));
        return result(r, "shop.gui.save", gui.id(), await(shop.saveGui(gui)));
    }

    private Response deleteGui(Request r) throws Exception {
        return result(r, "shop.gui.delete", r.param("id"), await(shop.deleteGui(r.param("id"))));
    }

    private Response arenas(Request r) throws Exception {
        JsonArray out = new JsonArray();
        for (ArenaShops a : await(shop.getArenaShops())) {
            JsonObject o = new JsonObject();
            o.addProperty("arena", a.arena());
            o.addProperty("waiting", a.waiting());
            o.addProperty("shopGui", a.shopGui());
            o.addProperty("upgradeGui", a.upgradeGui());
            out.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("arenas", out);
        return Response.ok(body);
    }

    private Response setArena(Request r) throws Exception {
        String shopGui = r.string("shopGui");
        String upgradeGui = r.string("upgradeGui");
        return result(r, "shop.arena", r.param("name"), await(shop.setArenaShops(r.param("name"), shopGui, upgradeGui)));
    }

    private Response result(Request r, String action, String target, ShopResult result) {
        audit.record(r.user(), r.ip(), action, target, result.code().name());
        JsonObject o = new JsonObject();
        o.addProperty("ok", result.isSuccess());
        o.addProperty("code", result.code().name());
        if (result.detail() != null) o.addProperty("detail", result.detail());
        return Response.ok(o);
    }

    static ShopGui parseGui(JsonObject j, String id) {
        List<ShopCategory> categories = new ArrayList<>();
        JsonElement cats = j.get("categories");
        if (cats != null && cats.isJsonArray()) {
            if (cats.getAsJsonArray().size() > MAX_LIST) throw new HttpError(400, "too_many");
            for (JsonElement c : cats.getAsJsonArray()) {
                if (!c.isJsonObject()) throw new HttpError(400, "bad_category");
                JsonObject o = c.getAsJsonObject();
                categories.add(new ShopCategory(str(o, "id"), str(o, "name"), str(o, "icon"), str(o, "description"),
                        strings(o.get("items"))));
            }
        }
        return new ShopGui(id, str(j, "title"), integer(j.get("size")), integer(j.get("headerSize")),
                integer(j.get("footerSize")), bool(j, "closeOnTransaction"), str(j, "fill"), str(j, "empty"),
                strings(j.get("slots")), categories, List.of(), List.of());
    }

    static JsonObject item(ShopItem i) {
        JsonObject o = new JsonObject();
        o.addProperty("name", i.name());
        o.addProperty("material", i.material());
        o.addProperty("amount", i.amount());
        o.addProperty("price", i.price());
        o.addProperty("priceType", i.priceType());
        o.addProperty("onBuy", i.onBuy());
        o.addProperty("logic", i.logic());
        o.addProperty("permanent", i.permanent());
        o.addProperty("permission", i.permission());
        JsonObject ench = new JsonObject();
        i.enchantments().forEach(ench::addProperty);
        o.add("enchantments", ench);
        o.add("flags", array(i.flags()));
        o.add("usedBy", array(i.usedBy()));
        return o;
    }

    static JsonObject gui(ShopGui g) {
        JsonObject o = new JsonObject();
        o.addProperty("id", g.id());
        o.addProperty("title", g.title());
        o.addProperty("size", g.size());
        o.addProperty("headerSize", g.headerSize());
        o.addProperty("footerSize", g.footerSize());
        o.addProperty("closeOnTransaction", g.closeOnTransaction());
        o.addProperty("fill", g.fill());
        o.addProperty("empty", g.empty());
        o.add("slots", array(g.slots()));
        JsonArray cats = new JsonArray();
        for (ShopCategory c : g.categories()) {
            JsonObject co = new JsonObject();
            co.addProperty("id", c.id());
            co.addProperty("name", c.name());
            co.addProperty("icon", c.icon());
            co.addProperty("description", c.description());
            co.add("items", array(c.items()));
            cats.add(co);
        }
        o.add("categories", cats);
        o.add("problems", array(g.problems()));
        o.add("usedBy", array(g.usedBy()));
        return o;
    }

    static JsonObject options(ShopOptions s) {
        JsonObject o = new JsonObject();
        o.add("materials", array(s.materials()));
        o.add("enchantments", array(s.enchantments()));
        o.add("flags", array(s.flags()));
        o.add("logicKeys", array(s.logicKeys()));
        o.add("upgrades", array(s.upgrades()));
        o.add("priceTypes", array(s.priceTypes()));
        o.add("onBuyActions", array(s.onBuyActions()));
        o.add("languages", array(s.languages()));
        return o;
    }

    private static JsonArray array(List<String> values) {
        JsonArray a = new JsonArray();
        values.forEach(a::add);
        return a;
    }

    private static String str(JsonObject j, String key) {
        JsonElement e = j.get(key);
        if (e == null || e.isJsonNull()) return null;
        if (!e.isJsonPrimitive()) throw new HttpError(400, "bad_" + key);
        return e.getAsString();
    }

    private static boolean bool(JsonObject j, String key) {
        JsonElement e = j.get(key);
        if (e == null || e.isJsonNull()) return false;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) throw new HttpError(400, "bad_" + key);
        return e.getAsBoolean();
    }

    private static int integer(JsonElement e) {
        if (e == null || e.isJsonNull()) return 0;
        try {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw new HttpError(400, "bad_number");
            double d = e.getAsDouble();
            if (d != Math.rint(d) || Math.abs(d) > 1_000_000) throw new HttpError(400, "bad_number");
            return (int) d;
        } catch (NumberFormatException ex) {
            throw new HttpError(400, "bad_number");
        }
    }

    private static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e == null || e.isJsonNull()) return out;
        if (!e.isJsonArray() || e.getAsJsonArray().size() > MAX_LIST) throw new HttpError(400, "bad_list");
        for (JsonElement v : e.getAsJsonArray()) {
            if (v == null || v.isJsonNull() || !v.isJsonPrimitive()) throw new HttpError(400, "bad_list");
            out.add(v.getAsString());
        }
        return out;
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            throw e;
        } catch (TimeoutException e) {
            throw new HttpError(504, "timeout");
        }
    }
}
