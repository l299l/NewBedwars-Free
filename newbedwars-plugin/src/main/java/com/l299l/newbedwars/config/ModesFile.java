package com.l299l.newbedwars.config;

import com.l299l.newbedwars.api.mode.PhaseInfo;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModesFile {
    public static final String FILE = "modes.yml";
    public static final String PHASES = "GamePhases";
    public static final String MODES = "Modes";
    private static final Pattern TOP_KEY = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*):.*$");
    private static final Pattern PHASE_ENTRY = Pattern.compile("^(\\s*)- ID:\\s*\"?([^\"\\s]+)\"?\\s*$");
    private static final Pattern MODE_ENTRY = Pattern.compile("^(\\s+)([A-Za-z0-9_\\-]+):\\s*$");

    private static volatile Map<String, Object> current = Map.of();
    private static volatile Map<String, Object> bundled = Map.of();

    private final Path file;
    private final String bundledText;
    private final Logger log;

    public ModesFile(Path dataFolder, String bundledText, Logger log) {
        this.file = dataFolder.resolve(FILE);
        this.bundledText = bundledText;
        this.log = log;
    }

    public static Map<String, Object> current() {
        return current;
    }

    public static Map<String, Object> bundled() {
        return bundled;
    }

    public static void use(Map<String, Object> currentRoot, Map<String, Object> bundledRoot) {
        current = currentRoot == null ? Map.of() : currentRoot;
        bundled = bundledRoot == null ? Map.of() : bundledRoot;
    }

    public void load(Object legacyPhases, Object legacyModes) {
        Map<String, Object> bundledRoot = parse(bundledText);
        if (bundledRoot == null) bundledRoot = Map.of();
        try {
            if (Files.isRegularFile(file) && (legacyPhases != null || legacyModes != null)) {
                log.warning("GamePhases/Modes in config.yml are no longer read and were removed from it; edit them in " + FILE
                        + " (the old config.yml is in data/backups/config/)");
            }
            if (!Files.isRegularFile(file)) {
                if (legacyPhases != null || legacyModes != null) {
                    write(migrated(legacyPhases, legacyModes));
                    log.info("Game phases and modes were moved from config.yml to " + FILE);
                } else {
                    write(bundledText);
                }
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Object> root = parse(text);
            if (root == null) {
                log.severe(FILE + " is not valid YAML; the built-in game phases and modes are used until it is fixed");
                use(Map.of(), bundledRoot);
                return;
            }
            String completed = addMissing(text, root, bundledRoot);
            if (!completed.equals(text)) {
                Map<String, Object> check = parse(completed);
                if (check != null && contains(check, bundledRoot)) {
                    write(completed);
                    root = check;
                } else {
                    log.warning("Could not add the missing built-in modes to " + FILE + " (unusual formatting); they are still available");
                }
            }
            use(root, bundledRoot);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read or write " + file, e);
            use(Map.of(), bundledRoot);
        }
    }

    public Map<String, Object> edit(UnaryOperator<String> change, Predicate<Map<String, Object>> check) throws IOException {
        String text = Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : bundledText;
        String changed = change.apply(text);
        if (changed == null) throw new IOException("the block could not be edited in place");
        Map<String, Object> root = parse(changed);
        if (root == null || !check.test(root)) throw new IOException(FILE + " could not be edited in place (unusual formatting); nothing was changed");
        write(changed);
        return root;
    }

    public static boolean hasMode(Map<String, Object> root, String id) {
        return root.get(MODES) instanceof Map<?, ?> m && m.get(id) instanceof Map<?, ?>;
    }

    public static boolean hasPhaseList(Map<String, Object> root, String id) {
        return phaseIds(root.get(PHASES)).contains(id);
    }

    public static List<String> modeBlock(String id, String generators, String gamePhases, String shopGui,
                                         String upgradeGui, int maxInTeam, int teams) {
        return modeBlock(id, generators, gamePhases, shopGui, upgradeGui, maxInTeam, teams, null);
    }

    public static List<String> modeBlock(String id, String generators, String gamePhases, String shopGui,
                                         String upgradeGui, int maxInTeam, int teams, String tablist) {
        List<String> out = new ArrayList<>();
        out.add("  " + id + ":");
        if (notBlank(generators)) out.add("    Generators: " + quote(generators.trim()));
        if (notBlank(gamePhases)) out.add("    GamePhases: " + quote(gamePhases.trim()));
        if (notBlank(shopGui)) out.add("    ShopGui: " + quote(shopGui.trim()));
        if (notBlank(upgradeGui)) out.add("    UpgradeGui: " + quote(upgradeGui.trim()));
        if (maxInTeam > 0) out.add("    MaxInTeam: " + maxInTeam);
        if (teams > 0) out.add("    Teams: " + teams);
        if (notBlank(tablist)) out.add("    Tablist: " + quote(tablist.trim()));
        if (out.size() == 1) out.add("    MaxInTeam: 0");
        return out;
    }

    public static List<String> phaseBlock(String id, List<PhaseInfo> phases) {
        List<String> out = new ArrayList<>();
        out.add("  - ID: " + id);
        out.add("    Phases:");
        for (PhaseInfo phase : phases) {
            out.add("      - Name: " + quote(phase.name() == null ? "" : phase.name()));
            out.add("        Duration: " + Math.max(0, phase.duration()));
            if (phase.actions().isEmpty()) {
                out.add("        Actions: []");
            } else {
                out.add("        Actions:");
                for (String action : phase.actions()) out.add("          - " + quote(action));
            }
        }
        return out;
    }

    public static String withMode(String text, String id, List<String> block) {
        return replaceEntry(text, MODES, MODE_ENTRY, id, block);
    }

    public static String withoutMode(String text, String id) {
        return replaceEntry(text, MODES, MODE_ENTRY, id, null);
    }

    public static String withPhaseList(String text, String id, List<String> block) {
        return replaceEntry(text, PHASES, PHASE_ENTRY, id, block);
    }

    public static String withoutPhaseList(String text, String id) {
        return replaceEntry(text, PHASES, PHASE_ENTRY, id, null);
    }

    private static String replaceEntry(String text, String section, Pattern entry, String id, List<String> block) {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        int start = topKey(lines, section);
        int[] range = start < 0 ? null : locate(lines, start, entry, id);
        if (range == null) {
            if (block == null) return null;
            insert(lines, section, entry, block);
            return String.join("\n", lines);
        }
        int indent = entryIndent(lines, start, entry);
        List<String> shifted = new ArrayList<>();
        if (block != null) for (String line : block) shifted.add(shift(line, indent - 2));
        for (int i = range[1] - 1; i >= range[0]; i--) lines.remove(i);
        lines.addAll(range[0], shifted);
        return String.join("\n", lines);
    }

    private static int entryIndent(List<String> lines, int start, Pattern entry) {
        int end = sectionEnd(lines, start);
        for (int i = start + 1; i < end; i++) {
            Matcher m = entry.matcher(lines.get(i));
            if (m.matches()) return m.group(1).length();
        }
        return 2;
    }

    private static int[] locate(List<String> lines, int start, Pattern entry, String id) {
        int end = sectionEnd(lines, start);
        int indent = entryIndent(lines, start, entry);
        int from = -1;
        for (int i = start + 1; i <= end; i++) {
            Matcher m = i < end ? entry.matcher(lines.get(i)) : null;
            boolean isEntry = m != null && m.matches() && m.group(1).length() == indent;
            if (from >= 0 && (isEntry || i == end)) {
                int to = i;
                while (to > from + 1 && (lines.get(to - 1).isBlank() || lines.get(to - 1).trim().startsWith("#"))) to--;
                return new int[]{from, to};
            }
            if (isEntry && m.group(2).equals(id)) from = i;
        }
        return null;
    }

    private static boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    public static String quote(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20 || c == 0x7f) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    String migrated(Object legacyPhases, Object legacyModes) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setIndicatorIndent(2);
        options.setIndentWithIndicator(true);
        options.setWidth(160);
        Yaml yaml = new Yaml(options);
        StringBuilder sb = new StringBuilder(header());
        if (legacyModes instanceof Map<?, ?> modes && !modes.isEmpty()) {
            sb.append(yaml.dump(Map.of(MODES, modes)));
        }
        if (legacyPhases instanceof List<?> phases && !phases.isEmpty()) {
            sb.append('\n').append(yaml.dump(Map.of(PHASES, phases)));
        }
        return sb.toString();
    }

    String addMissing(String text, Map<String, Object> root, Map<String, Object> bundledRoot) {
        Set<String> havePhases = phaseIds(root.get(PHASES));
        Set<String> haveModes = root.get(MODES) instanceof Map<?, ?> m ? keys(m) : Set.of();
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        List<String> source = List.of(bundledText.split("\n", -1));
        List<String> added = new ArrayList<>();
        List<String> modeBlocks = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : entries(source, MODES, MODE_ENTRY).entrySet()) {
            if (haveModes.contains(e.getKey())) continue;
            modeBlocks.addAll(e.getValue());
            added.add(e.getKey());
        }
        insert(lines, MODES, MODE_ENTRY, modeBlocks);
        List<String> phaseBlocks = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : entries(source, PHASES, PHASE_ENTRY).entrySet()) {
            if (havePhases.contains(e.getKey())) continue;
            phaseBlocks.addAll(e.getValue());
            if (!added.contains(e.getKey())) added.add(e.getKey() + " (phases)");
        }
        insert(lines, PHASES, PHASE_ENTRY, phaseBlocks);
        if (added.isEmpty()) return text;
        log.info("Added the built-in " + String.join(", ", added) + " to " + FILE);
        return String.join("\n", lines);
    }

    private static void insert(List<String> lines, String section, Pattern entry, List<String> block) {
        if (block.isEmpty()) return;
        int start = topKey(lines, section);
        if (start < 0) {
            while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) lines.remove(lines.size() - 1);
            lines.add("");
            lines.add(section + ":");
            lines.addAll(block);
            lines.add("");
            return;
        }
        int end = sectionEnd(lines, start);
        int indent = -1;
        for (int i = start + 1; i < end; i++) {
            Matcher m = entry.matcher(lines.get(i));
            if (m.matches()) {
                indent = m.group(1).length();
                break;
            }
        }
        int shift = indent < 0 ? 0 : indent - 2;
        List<String> shifted = new ArrayList<>();
        for (String line : block) shifted.add(shift(line, shift));
        if (lines.get(start).trim().endsWith("[]") || lines.get(start).trim().endsWith("{}")) {
            lines.set(start, section + ":");
        }
        lines.addAll(end, shifted);
    }

    private static String shift(String line, int shift) {
        if (shift == 0 || line.isBlank()) return line;
        if (shift > 0) return " ".repeat(shift) + line;
        int remove = Math.min(-shift, line.length() - line.stripLeading().length());
        return line.substring(remove);
    }

    private static Map<String, List<String>> entries(List<String> lines, String section, Pattern entry) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        int start = topKey(lines, section);
        if (start < 0) return out;
        int end = sectionEnd(lines, start);
        String id = null;
        List<String> block = null;
        for (int i = start + 1; i < end; i++) {
            String line = lines.get(i);
            Matcher m = entry.matcher(line);
            if (m.matches() && m.group(1).length() == 2) {
                if (id != null) out.put(id, block);
                id = m.group(2);
                block = new ArrayList<>();
            }
            if (block != null && !line.trim().startsWith("#")) block.add(line);
        }
        if (id != null) out.put(id, block);
        for (List<String> b : out.values()) {
            while (!b.isEmpty() && b.get(b.size() - 1).isBlank()) b.remove(b.size() - 1);
        }
        return out;
    }

    private static int topKey(List<String> lines, String key) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = TOP_KEY.matcher(lines.get(i));
            if (m.matches() && m.group(1).equals(key)) return i;
        }
        return -1;
    }

    private static int sectionEnd(List<String> lines, int start) {
        int next = lines.size();
        for (int i = start + 1; i < lines.size(); i++) {
            if (TOP_KEY.matcher(lines.get(i)).matches()) {
                next = i;
                break;
            }
        }
        int end = next;
        while (end > start + 1) {
            String line = lines.get(end - 1);
            if (line.isBlank() || line.startsWith("#")) end--;
            else break;
        }
        return end;
    }

    private static boolean contains(Map<String, Object> root, Map<String, Object> bundledRoot) {
        if (!phaseIds(root.get(PHASES)).containsAll(phaseIds(bundledRoot.get(PHASES)))) return false;
        Set<String> modes = root.get(MODES) instanceof Map<?, ?> m ? keys(m) : Set.of();
        Set<String> want = bundledRoot.get(MODES) instanceof Map<?, ?> m ? keys(m) : Set.of();
        return modes.containsAll(want);
    }

    private static Set<String> phaseIds(Object list) {
        Set<String> out = new LinkedHashSet<>();
        if (list instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Map<?, ?> m && m.get("ID") != null) out.add(String.valueOf(m.get("ID")));
            }
        }
        return out;
    }

    private static Set<String> keys(Map<?, ?> map) {
        Set<String> out = new LinkedHashSet<>();
        for (Object k : map.keySet()) out.add(String.valueOf(k));
        return out;
    }

    private String header() {
        int cut = bundledText.indexOf("\n" + MODES + ":");
        if (cut < 0) cut = bundledText.indexOf("\n" + PHASES + ":");
        return cut < 0 ? "" : bundledText.substring(0, cut + 1);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String text) {
        try {
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(text);
            if (root == null) return new LinkedHashMap<>();
            return root instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void write(String text) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(FILE + ".tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
