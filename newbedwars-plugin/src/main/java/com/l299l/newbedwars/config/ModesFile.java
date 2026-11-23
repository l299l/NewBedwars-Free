package com.l299l.newbedwars.config;

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
