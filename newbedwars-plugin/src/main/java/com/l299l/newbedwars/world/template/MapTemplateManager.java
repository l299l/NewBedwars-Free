package com.l299l.newbedwars.world.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class MapTemplateManager {
    public static final String DIRTY_MARKER = ".nbw-dirty";
    public static final String META_FILE = "template.json";
    public static final String TEMPLATE_DIR = "template";
    public static final long MAX_UNZIPPED_BYTES = 2L * 1024 * 1024 * 1024;
    private static final Logger LOG = Logger.getLogger("NewBedwars");

    private MapTemplateManager() {}

    public static Path arenaRoot(Path mapsRoot, String arena) {
        return mapsRoot.resolve(arena);
    }

    public static Path templateDir(Path mapsRoot, String arena) {
        return arenaRoot(mapsRoot, arena).resolve(TEMPLATE_DIR);
    }

    public static boolean hasTemplate(Path mapsRoot, String arena) {
        return isWorldFolder(templateDir(mapsRoot, arena));
    }

    public static boolean isWorldFolder(Path dir) {
        return Files.isRegularFile(dir.resolve("level.dat")) || Files.isDirectory(dir.resolve("region"));
    }

    public static boolean isDirty(Path mapsRoot, String arena) {
        return Files.exists(arenaRoot(mapsRoot, arena).resolve(DIRTY_MARKER));
    }

    public static void markDirty(Path mapsRoot, String arena) throws IOException {
        Path root = arenaRoot(mapsRoot, arena);
        Files.createDirectories(root);
        Files.writeString(root.resolve(DIRTY_MARKER), String.valueOf(System.currentTimeMillis()), StandardCharsets.UTF_8);
    }

    public static void clearDirty(Path mapsRoot, String arena) throws IOException {
        Files.deleteIfExists(arenaRoot(mapsRoot, arena).resolve(DIRTY_MARKER));
    }

    public static void createTemplate(Path mapsRoot, String arena, Path worldFolder, Collection<String> excludes) throws IOException {
        Path root = arenaRoot(mapsRoot, arena);
        Files.createDirectories(root);
        Path fresh = root.resolve(TEMPLATE_DIR + ".new");
        deleteTree(fresh);
        copyTree(worldFolder, fresh, excludes);
        if (!isWorldFolder(fresh)) {
            deleteTree(fresh);
            throw new IOException("World folder " + worldFolder + " has no level.dat or region folder (was the world saved?)");
        }
        Path current = templateDir(mapsRoot, arena);
        try {
            repairIncompleteFiles(worldFolder, fresh, current);
        } catch (IOException e) {
            deleteTree(fresh);
            throw e;
        }
        deleteTree(current);
        Files.move(fresh, current);
        long files = countFiles(current);
        Files.writeString(root.resolve(META_FILE),
                "{\n  \"created\": " + System.currentTimeMillis()
                        + ",\n  \"files\": " + files
                        + ",\n  \"bytes\": " + sizeOf(current) + "\n}\n", StandardCharsets.UTF_8);
    }

    private static final int REPAIR_ATTEMPTS = 50;
    private static final long REPAIR_WAIT_MS = 100;

    static void repairIncompleteFiles(Path source, Path copy, Path previous) throws IOException {
        List<Path> broken = new ArrayList<>();
        try (Stream<Path> s = Files.walk(copy)) {
            s.filter(p -> p.getFileName().toString().endsWith(".dat")).filter(p -> !isCompleteDat(p)).forEach(broken::add);
        }
        for (Path file : broken) {
            Path relative = copy.relativize(file);
            Path from = source.resolve(relative.toString());
            boolean fixed = false;
            for (int i = 0; i < REPAIR_ATTEMPTS && !fixed; i++) {
                if (i > 0) sleep();
                if (Files.isRegularFile(from)) {
                    Files.copy(from, file, StandardCopyOption.REPLACE_EXISTING);
                    fixed = isCompleteDat(file);
                }
            }
            Path old = previous == null ? null : previous.resolve(relative.toString());
            if (!fixed && old != null && Files.isRegularFile(old) && isCompleteDat(old)) {
                Files.copy(old, file, StandardCopyOption.REPLACE_EXISTING);
                fixed = true;
            }
            if (!fixed) throw new IOException("World file " + relative + " is incomplete (still being written); try again");
        }
    }

    static boolean isCompleteDat(Path file) {
        try (InputStream raw = Files.newInputStream(file)) {
            byte[] head = raw.readNBytes(2);
            if (head.length < 2 || (head[0] & 0xff) != 0x1f || (head[1] & 0xff) != 0x8b) return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read " + file, e);
            return false;
        }
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(file))) {
            in.transferTo(OutputStream.nullOutputStream());
            return true;
        } catch (IOException e) {
            LOG.log(Level.FINE, "Incomplete world file " + file + ": " + e.getMessage());
            return false;
        }
    }

    private static void sleep() throws IOException {
        try {
            Thread.sleep(REPAIR_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for world files");
        }
    }

    public static void restoreTemplate(Path mapsRoot, String arena, Path worldFolder, Collection<String> excludes) throws IOException {
        Path template = templateDir(mapsRoot, arena);
        if (!hasTemplate(mapsRoot, arena)) {
            throw new IOException("Arena " + arena + " has no valid template");
        }
        deleteTree(worldFolder);
        copyTree(template, worldFolder, excludes);
        if (!isWorldFolder(worldFolder)) {
            throw new IOException("Restored world of " + arena + " has no world data");
        }
    }

    public static boolean zipTemplate(Path mapsRoot, String arena, Path target) throws IOException {
        if (!hasTemplate(mapsRoot, arena)) return false;
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            addTree(zip, templateDir(mapsRoot, arena), "");
        }
        return true;
    }

    public static void restoreTemplateFromZip(Path mapsRoot, String arena, Path zip) throws IOException {
        Path root = arenaRoot(mapsRoot, arena);
        Files.createDirectories(root);
        Path fresh = root.resolve(TEMPLATE_DIR + ".new");
        deleteTree(fresh);
        unzip(zip, fresh);
        if (!isWorldFolder(fresh)) {
            deleteTree(fresh);
            throw new IOException("Backup " + zip.getFileName() + " does not contain a world");
        }
        deleteTree(templateDir(mapsRoot, arena));
        Files.move(fresh, templateDir(mapsRoot, arena));
    }

    public static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
    }

    public static void rotate(Path dir, String extension, int keep) throws IOException {
        if (!Files.isDirectory(dir)) return;
        List<Path> files = listBackups(dir, extension);
        for (int i = keep; i < files.size(); i++) {
            Files.deleteIfExists(files.get(i));
        }
    }

    public static List<Path> listBackups(Path dir, String extension) throws IOException {
        if (!Files.isDirectory(dir)) return new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> files = new ArrayList<>(s.filter(p -> p.getFileName().toString().endsWith(extension)).toList());
            files.sort(Collections.reverseOrder());
            return files;
        }
    }

    public static void copyTree(Path source, Path target, Collection<String> excludes) throws IOException {
        Set<String> skip = Set.copyOf(excludes);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(source) && skip.contains(dir.getFileName().toString())) return FileVisitResult.SKIP_SUBTREE;
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (skip.contains(file.getFileName().toString())) return FileVisitResult.CONTINUE;
                Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void addTree(ZipOutputStream zip, Path source, String prefix) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = prefix + source.relativize(file).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(file, zip);
                zip.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void unzip(Path zip, Path target) throws IOException {
        unzip(zip, target, MAX_UNZIPPED_BYTES);
    }

    static void unzip(Path zip, Path target, long maxBytes) throws IOException {
        long total = 0;
        byte[] buffer = new byte[8192];
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Files.createDirectories(normalizedTarget);
        try (InputStream in = Files.newInputStream(zip); ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = normalizedTarget.resolve(entry.getName()).normalize();
                if (!out.startsWith(normalizedTarget)) {
                    throw new IOException("Illegal zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out)) {
                        int read;
                        while ((read = zis.read(buffer)) > 0) {
                            total += read;
                            if (total > maxBytes) throw new IOException("Zip " + zip.getFileName() + " is larger than " + maxBytes + " bytes unpacked");
                            os.write(buffer, 0, read);
                        }
                    }
                }
            }
        }
    }

    private static long countFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).count();
        }
    }

    private static long sizeOf(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            long total = 0;
            for (Path p : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) total += Files.size(p);
            return total;
        }
    }
}
