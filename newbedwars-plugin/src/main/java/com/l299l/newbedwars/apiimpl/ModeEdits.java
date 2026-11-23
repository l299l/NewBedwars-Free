package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.mode.EditResult;
import com.l299l.newbedwars.api.mode.GeneratorConfigInfo;
import com.l299l.newbedwars.api.mode.ModeInfo;
import com.l299l.newbedwars.api.mode.PhaseInfo;
import com.l299l.newbedwars.api.mode.PhaseListInfo;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.phases.PhaseActionRegistry;
import com.l299l.newbedwars.config.BuiltInModes;
import com.l299l.newbedwars.config.GeneratorFiles;
import com.l299l.newbedwars.config.ModesFile;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.config.data.json.arenas.AtomicFileWriter;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

final class ModeEdits {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");
    private static final int MAX_VALUE = 100;

    interface Io {
        EditResult run() throws IOException;
    }

    record Plan(EditResult early, Io io, Runnable reload) {
        static Plan done(EditResult result) {
            return new Plan(result, null, null);
        }

        EditResult runNow() {
            if (early != null) return early;
            EditResult result;
            try {
                result = io.run();
            } catch (IOException | RuntimeException e) {
                return failed(e);
            }
            if (result.code() == EditResult.Code.OK) reload.run();
            return result;
        }
    }

    private ModeEdits() {}

    static EditResult ok() {
        return new EditResult(EditResult.Code.OK, null, null);
    }

    static EditResult code(EditResult.Code code) {
        return new EditResult(code, null, null);
    }

    static EditResult invalid(List<String> problems) {
        return new EditResult(EditResult.Code.INVALID, problems, null);
    }

    static EditResult failed(Exception e) {
        return new EditResult(EditResult.Code.FAILED, List.of(String.valueOf(e.getMessage() == null ? e : e.getMessage())), null);
    }

    static CompletableFuture<EditResult> execute(Supplier<Plan> prepare) {
        return ApiMaintenance.onMain(() -> {
            Plan plan = prepare.get();
            if (plan.early() != null) return CompletableFuture.completedFuture(plan.early());
            return offload(() -> {
                try {
                    return plan.io().run();
                } catch (IOException | RuntimeException e) {
                    return failed(e);
                }
            }, result -> {
                if (result.code() != EditResult.Code.OK) return result;
                try {
                    plan.reload().run();
                } catch (RuntimeException e) {
                    return failed(e);
                }
                return result;
            });
        });
    }

    static <T> CompletableFuture<T> offload(Callable<T> work, UnaryOperator<T> onMain) {
        CompletableFuture<T> future = new CompletableFuture<>();
        IOExecutor.submit(() -> {
            T value;
            try {
                value = work.call();
            } catch (Exception e) {
                future.completeExceptionally(e);
                return;
            }
            try {
                Bukkit.getScheduler().runTask(NewBedwars.plugin, () -> {
                    try {
                        future.complete(onMain.apply(value));
                    } catch (RuntimeException e) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private static ModesFile modesFile() {
        return new ModesFile(NewBedwars.plugin.getDataFolder().toPath(), "", NewBedwars.plugin.getLogger());
    }

    private static Path generatorsFolder() {
        return NewBedwars.plugin.getDataFolder().toPath().resolve("generators");
    }

    private static boolean plainId(String id) {
        if (id == null || !ID.matcher(id).matches()) return false;
        try {
            return new Yaml(new SafeConstructor(new LoaderOptions())).load(id) instanceof String;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String existing(Set<String> keys, String id) {
        for (String key : keys) if (key.equalsIgnoreCase(id)) return key;
        return null;
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    private static boolean tablistExists(String id) {
        com.l299l.newbedwars.tablist.TablistManager manager = NewBedwars.plugin.getTablistManager();
        return manager != null && manager.getConfig().find(id.trim()) != null;
    }

    private static String canonicalTablist(String id) {
        if (blank(id)) return null;
        return NewBedwars.plugin.getTablistManager().getConfig().find(id.trim()).id();
    }

    static List<String> validateMode(ModeInfo mode) {
        List<String> problems = new ArrayList<>();
        if (!plainId(mode.id())) {
            problems.add("invalid mode id (use 1-32 letters, digits, _ or -): " + mode.id());
        }
        if (!blank(mode.generators()) && !NewBedwars.plugin.getGeneratorLeveling().containsKey(mode.generators().trim())) {
            problems.add("unknown generator config: " + mode.generators());
        }
        if (!blank(mode.gamePhases()) && !Properties.GamePhases.containsKey(mode.gamePhases().trim())) {
            problems.add("unknown phase list: " + mode.gamePhases());
        }
        if (!blank(mode.shopGui()) && NewBedwars.plugin.getGuiManager().getGui(mode.shopGui().trim()) == null) {
            problems.add("unknown shop GUI: " + mode.shopGui());
        }
        if (!blank(mode.upgradeGui()) && NewBedwars.plugin.getGuiManager().getGui(mode.upgradeGui().trim()) == null) {
            problems.add("unknown upgrade GUI: " + mode.upgradeGui());
        }
        if (!blank(mode.tablist()) && !tablistExists(mode.tablist())) problems.add("unknown tablist: " + mode.tablist());
        if (mode.maxInTeam() < 0 || mode.maxInTeam() > MAX_VALUE) problems.add("maxInTeam must be 0-" + MAX_VALUE);
        if (mode.teams() < 0 || mode.teams() > MAX_VALUE) problems.add("teams must be 0-" + MAX_VALUE);
        return problems;
    }

    static Plan saveMode(ModeInfo mode) {
        if (mode == null) return Plan.done(invalid(List.of("no mode given")));
        List<String> problems = validateMode(mode);
        if (!problems.isEmpty()) return Plan.done(invalid(problems));
        String old = existing(Properties.Modes.keySet(), mode.id());
        String id = old != null ? old : mode.id();
        List<String> block = ModesFile.modeBlock(id, mode.generators(), mode.gamePhases(), mode.shopGui(), mode.upgradeGui(),
                mode.maxInTeam(), mode.teams(), canonicalTablist(mode.tablist()));
        return rewrite(text -> ModesFile.withMode(text, id, block), root -> ModesFile.hasMode(root, id));
    }

    static Plan deleteMode(String id) {
        String old = id == null ? null : existing(Properties.Modes.keySet(), id);
        if (old == null) return Plan.done(code(EditResult.Code.NOT_FOUND));
        if (existing(Set.copyOf(BuiltInModes.NAMES), old) != null) return Plan.done(code(EditResult.Code.BUILT_IN));
        return rewrite(text -> ModesFile.withoutMode(text, old), root -> !ModesFile.hasMode(root, old));
    }

    static List<String> validatePhaseList(PhaseListInfo list) {
        List<String> problems = new ArrayList<>();
        if (!plainId(list.id())) problems.add("invalid phase list id (use 1-32 letters, digits, _ or -): " + list.id());
        if (list.phases().isEmpty()) problems.add("a phase list needs at least one phase");
        int n = 0;
        for (PhaseInfo phase : list.phases()) {
            n++;
            if (blank(phase.name())) problems.add("phase " + n + " has no name");
            if (phase.duration() < 0) problems.add("phase " + n + " has a negative duration");
            for (String action : phase.actions()) {
                String problem = actionProblem(action);
                if (problem != null) problems.add("phase " + n + ": " + problem);
            }
        }
        return problems;
    }

    private static String actionProblem(String action) {
        if (blank(action) || action.indexOf('\n') >= 0 || action.indexOf('\r') >= 0) return "invalid action: " + action;
        String[] args = action.replace(" ", "").split(":", 2);
        String name = args[0].trim();
        if (name.isEmpty() || PhaseActionRegistry.get(name) == null) return "unknown action: " + action;
        String arg = args.length > 1 ? args[1] : "";
        if (name.equalsIgnoreCase("DIAMOND_GENERATOR") || name.equalsIgnoreCase("EMERALD_GENERATOR")) {
            try {
                Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                return "action needs a level number: " + action;
            }
        }
        return null;
    }

    static Plan savePhaseList(PhaseListInfo list) {
        if (list == null) return Plan.done(invalid(List.of("no phase list given")));
        List<String> problems = validatePhaseList(list);
        if (!problems.isEmpty()) return Plan.done(invalid(problems));
        String old = existing(Properties.GamePhases.keySet(), list.id());
        String id = old != null ? old : list.id();
        List<String> block = ModesFile.phaseBlock(id, list.phases());
        return rewrite(text -> ModesFile.withPhaseList(text, id, block), root -> ModesFile.hasPhaseList(root, id));
    }

    static Set<String> builtInPhaseLists() {
        Set<String> out = new TreeSet<>(BuiltInModes.NAMES);
        if (ModesFile.bundled().get(ModesFile.PHASES) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m && m.get("ID") != null) out.add(String.valueOf(m.get("ID")));
            }
        }
        return out;
    }

    static Plan deletePhaseList(String id) {
        String old = id == null ? null : existing(Properties.GamePhases.keySet(), id);
        if (old == null) return Plan.done(code(EditResult.Code.NOT_FOUND));
        if (builtInPhaseLists().contains(old)) return Plan.done(code(EditResult.Code.BUILT_IN));
        List<String> usedBy = new ArrayList<>();
        for (GameMode m : Properties.Modes.values()) if (old.equals(m.gamePhases())) usedBy.add("mode " + m.id());
        for (IArena arena : ArenaRegistry.all()) if (old.equals(arena.getGamePhasesId())) usedBy.add("arena " + arena.getArenaName());
        if (!usedBy.isEmpty()) return Plan.done(new EditResult(EditResult.Code.IN_USE, null, usedBy));
        return rewrite(text -> ModesFile.withoutPhaseList(text, old), root -> !ModesFile.hasPhaseList(root, old));
    }

    private static Plan rewrite(UnaryOperator<String> change, java.util.function.Predicate<Map<String, Object>> check) {
        ModesFile file = modesFile();
        java.util.concurrent.atomic.AtomicReference<Map<String, Object>> result = new java.util.concurrent.atomic.AtomicReference<>();
        return new Plan(null, () -> {
            result.set(file.edit(change, check));
            return ok();
        }, () -> {
            ModesFile.use(result.get(), ModesFile.bundled());
            Properties.reloadModes();
        });
    }

    static List<GeneratorConfigInfo> generatorFiles() throws IOException {
        List<GeneratorConfigInfo> out = new ArrayList<>();
        for (Map.Entry<String, Path> e : GeneratorFiles.scan(generatorsFolder()).entrySet()) {
            out.add(new GeneratorConfigInfo(e.getKey(), e.getValue().getFileName().toString(), GeneratorFiles.isBuiltIn(e.getKey())));
        }
        return out;
    }

    static String generatorText(String id) throws IOException {
        Path file = id == null ? null : GeneratorFiles.scan(generatorsFolder()).get(id);
        return file == null ? null : Files.readString(file, StandardCharsets.UTF_8);
    }

    static Plan saveGeneratorConfig(String id, String yaml) {
        if (!plainId(id)) return Plan.done(invalid(List.of("invalid generator config id (use 1-32 letters, digits, _ or -): " + id)));
        List<String> problems = GeneratorFiles.validate(id, yaml);
        if (!problems.isEmpty()) return Plan.done(invalid(problems));
        return new Plan(null, () -> {
            Path folder = generatorsFolder();
            Path file = GeneratorFiles.scan(folder).get(id);
            if (file == null) {
                file = folder.resolve(id + ".yml");
                if (Files.exists(file)) return invalid(List.of("the file " + file.getFileName() + " is used by another config"));
            }
            AtomicFileWriter.write(file, yaml, null, 0);
            return ok();
        }, () -> NewBedwars.plugin.reloadGeneratorConfigs());
    }

    static Plan deleteGeneratorConfig(String id) {
        if (id == null || !NewBedwars.plugin.getGeneratorLeveling().containsKey(id)) return Plan.done(code(EditResult.Code.NOT_FOUND));
        if (GeneratorFiles.isBuiltIn(id)) return Plan.done(code(EditResult.Code.BUILT_IN));
        List<String> usedBy = new ArrayList<>();
        for (GameMode m : Properties.Modes.values()) if (id.equals(m.generators())) usedBy.add("mode " + m.id());
        for (IArena arena : ArenaRegistry.all()) if (id.equals(arena.getGeneratorsLevelingId())) usedBy.add("arena " + arena.getArenaName());
        if (!usedBy.isEmpty()) return Plan.done(new EditResult(EditResult.Code.IN_USE, null, usedBy));
        return new Plan(null, () -> {
            Path file = GeneratorFiles.scan(generatorsFolder()).get(id);
            if (file == null) return code(EditResult.Code.NOT_FOUND);
            Files.delete(file);
            return ok();
        }, () -> NewBedwars.plugin.reloadGeneratorConfigs());
    }
}
