package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.maintenance.ArenaBackup;
import com.l299l.newbedwars.api.maintenance.ArenaMaintenance;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ArenaRoutes {
    public static final Pattern ZIP_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]{0,100}\\.zip");
    private static final long TIMEOUT_MINUTES = 10;

    private final ArenaMaintenance maintenance;
    private final AuditLog audit;
    private final long maxUploadBytes;

    public ArenaRoutes(ArenaMaintenance maintenance, AuditLog audit, long maxUploadBytes) {
        this.maintenance = maintenance;
        this.audit = audit;
        this.maxUploadBytes = maxUploadBytes;
    }

    public void register(Router router) {
        action(router, "enable", Permission.ARENA_ENABLE, (r) -> maintenance.enable(r.param("name")));
        action(router, "disable", Permission.ARENA_DISABLE, (r) -> maintenance.disable(r.param("name")));
        action(router, "backup", Permission.ARENA_BACKUP, (r) -> maintenance.backup(r.param("name")));
        action(router, "resetmap", Permission.ARENA_RESETMAP, (r) -> maintenance.resetMap(r.param("name")));
        action(router, "delete", Permission.ARENA_DELETE, (r) -> maintenance.delete(r.param("name")));
        action(router, "restore", Permission.ARENA_RESTORE, (r) -> {
            try {
                return maintenance.restore(r.param("name"), r.string("backup"));
            } catch (IOException e) {
                throw new HttpError(400, "bad_json");
            }
        });
        action(router, "copy", Permission.ARENA_COPY, (r) -> {
            try {
                return maintenance.copy(r.param("name"), r.required("newName"));
            } catch (IOException e) {
                throw new HttpError(400, "bad_json");
            }
        });
        router.allowed("POST", "/api/arenas/{name}/export", Set.of(Permission.ARENA_EXPORT), this::export);
        router.allowed("GET", "/api/arenas/{name}/backups", Set.of(Permission.ARENA_RESTORE, Permission.ARENA_BACKUP), this::backups);
        router.allowed("GET", "/api/exports", Set.of(Permission.ARENA_EXPORT, Permission.ARENA_IMPORT), r -> exports());
        router.allowed("GET", "/api/exports/{file}", Set.of(Permission.ARENA_EXPORT), this::download);
        router.allowed("POST", "/api/import", Set.of(Permission.ARENA_IMPORT), this::importArena);
    }

    private void action(Router router, String name, String permission, Function<Request, CompletableFuture<MaintenanceResult>> call) {
        router.allowed("POST", "/api/arenas/{name}/" + name, Set.of(permission), r -> {
            MaintenanceResult result = await(call.apply(r));
            audit.record(r.user(), r.ip(), "arena." + name, r.param("name"), result.code().name());
            return Response.ok(json(result, result.detail()));
        });
    }

    private Response export(Request r) throws Exception {
        MaintenanceResult result = await(maintenance.export(r.param("name")));
        audit.record(r.user(), r.ip(), "arena.export", r.param("name"), result.code().name());
        String file = result.isSuccess() && result.detail() != null ? Path.of(result.detail()).getFileName().toString() : result.detail();
        return Response.ok(json(result, file));
    }

    private Response backups(Request r) throws Exception {
        List<ArenaBackup> list;
        try {
            list = await(maintenance.listBackups(r.param("name")));
        } catch (NoSuchElementException e) {
            throw new HttpError(404, "arena_not_found");
        }
        JsonArray array = new JsonArray();
        for (ArenaBackup backup : list) {
            JsonObject o = new JsonObject();
            o.addProperty("id", backup.id());
            o.addProperty("arenaFile", backup.hasArenaFile());
            o.addProperty("map", backup.hasMap());
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("backups", array);
        return Response.ok(body);
    }

    private Response exports() throws IOException {
        JsonArray array = new JsonArray();
        Path folder = maintenance.getExportFolder();
        if (Files.isDirectory(folder)) {
            try (Stream<Path> files = Files.list(folder)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(f -> ZIP_NAME.matcher(f.getFileName().toString()).matches())
                        .sorted(Comparator.comparing((Path f) -> f.toFile().lastModified()).reversed()).toList()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("name", file.getFileName().toString());
                    o.addProperty("size", Files.size(file));
                    o.addProperty("modified", Files.getLastModifiedTime(file).toMillis());
                    array.add(o);
                }
            }
        }
        JsonObject body = new JsonObject();
        body.add("files", array);
        return Response.ok(body);
    }

    private Path exportFile(String name) {
        if (name == null || !ZIP_NAME.matcher(name).matches()) throw new HttpError(400, "bad_file_name");
        Path folder = maintenance.getExportFolder().toAbsolutePath().normalize();
        Path file = folder.resolve(name).normalize();
        if (!file.getParent().equals(folder)) throw new HttpError(400, "bad_file_name");
        return file;
    }

    private Response download(Request r) {
        Path file = exportFile(r.param("file"));
        if (!Files.isRegularFile(file)) throw new HttpError(404, "not_found");
        audit.record(r.user(), r.ip(), "export.download", file.getFileName().toString(), "OK");
        return Response.download(file, file.getFileName().toString());
    }

    private Response importArena(Request r) throws Exception {
        String existing = r.query("file");
        if (existing != null) {
            Path file = exportFile(existing);
            if (!Files.isRegularFile(file)) throw new HttpError(404, "not_found");
            return runImport(r, file.getFileName().toString(), null);
        }
        Path file = exportFile(r.query("name"));
        String length = r.header("Content-Length");
        if (length != null) {
            try {
                if (Long.parseLong(length.trim()) > maxUploadBytes) throw new HttpError(413, "too_large");
            } catch (NumberFormatException e) {
                throw new HttpError(400, "bad_length");
            }
        }
        if (Files.exists(file)) throw new HttpError(409, "file_exists");
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), ".upload-", ".tmp");
        try {
            copyLimited(r.body(), temp);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, file);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
        audit.record(r.user(), r.ip(), "import.upload", file.getFileName().toString(), "OK");
        return runImport(r, file.getFileName().toString(), file);
    }

    private Response runImport(Request r, String name, Path uploaded) throws Exception {
        MaintenanceResult result = await(maintenance.importArena(name));
        audit.record(r.user(), r.ip(), "arena.import", name, result.code().name());
        if (!result.isSuccess() && uploaded != null) Files.deleteIfExists(uploaded);
        return Response.ok(json(result, result.detail()));
    }

    private void copyLimited(InputStream in, Path target) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        try (InputStream input = in; OutputStream out = Files.newOutputStream(target)) {
            int n;
            while ((n = input.read(buffer)) > 0) {
                total += n;
                if (total > maxUploadBytes) throw new HttpError(413, "too_large");
                out.write(buffer, 0, n);
            }
        }
        if (total == 0) throw new HttpError(400, "empty_file");
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        try {
            return future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            throw e;
        } catch (TimeoutException e) {
            throw new HttpError(504, "timeout");
        }
    }

    private static JsonObject json(MaintenanceResult result, String detail) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", result.isSuccess());
        o.addProperty("code", result.code().name());
        o.addProperty("arena", result.arena());
        if (result.message() != null) o.addProperty("message", result.message());
        if (detail != null) o.addProperty("detail", detail);
        return o;
    }
}
