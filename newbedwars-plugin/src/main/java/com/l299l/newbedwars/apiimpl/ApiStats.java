package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.view.LeaderboardEntry;
import com.l299l.newbedwars.api.view.PlayerStatsView;
import com.l299l.newbedwars.api.view.StatType;
import com.l299l.newbedwars.apiimpl.view.ApiPlayerStats;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.config.StorageType;
import com.l299l.newbedwars.config.data.sql.Database;
import com.l299l.newbedwars.config.data.sql.PlayerSqlRepo;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ApiStats {
    public static final int MAX_LIMIT = 100;
    private static final long SQL_TIMEOUT_SECONDS = 15;

    public interface SqlRunner {
        void submit(Database.Task task);
    }

    private final Supplier<PlayerManager> players;
    private final Supplier<Boolean> useSql;
    private final SqlRunner sql;
    private final Executor mainThread;

    ApiStats(Supplier<PlayerManager> players, Supplier<Boolean> useSql, SqlRunner sql, Executor mainThread) {
        this.players = players;
        this.useSql = useSql;
        this.sql = sql;
        this.mainThread = mainThread;
    }

    static ApiStats create() {
        return new ApiStats(() -> NewBedwars.plugin.getPlayerManager(),
                () -> Properties.StorageType == StorageType.MYSQL && NewBedwars.plugin.getDataManager() != null
                        && NewBedwars.plugin.getDataManager().getPlayerDataSql() != null,
                task -> Database.get(NewBedwars.plugin.getLogger()).submit("panel stats", task),
                command -> {
                    if (Bukkit.isPrimaryThread()) command.run();
                    else Bukkit.getScheduler().runTask(NewBedwars.plugin, command);
                });
    }

    static String column(StatType stat) {
        return stat.name().toLowerCase(Locale.ROOT);
    }

    private static int value(StatType stat, PlayerStats s) {
        return stat.of(new ApiPlayerStats(s));
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    public CompletableFuture<List<LeaderboardEntry>> leaderboard(StatType stat, int limit) {
        if (stat == null) return CompletableFuture.failedFuture(new IllegalArgumentException("stat"));
        int n = clamp(limit);
        if (useSql.get()) {
            return onSql(c -> {
                List<LeaderboardEntry> out = new ArrayList<>();
                for (Map.Entry<String, PlayerStats> e : PlayerSqlRepo.top(c, column(stat), n)) {
                    out.add(new LeaderboardEntry(e.getKey(), new ApiPlayerStats(e.getValue())));
                }
                return out;
            });
        }
        return onMain(() -> {
            List<Map.Entry<String, PlayerStats>> all = new ArrayList<>(players.get().getAllStats().entrySet());
            all.sort(Comparator.comparingInt((Map.Entry<String, PlayerStats> e) -> value(stat, e.getValue())).reversed()
                    .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));
            List<LeaderboardEntry> out = new ArrayList<>();
            for (Map.Entry<String, PlayerStats> e : all.subList(0, Math.min(n, all.size()))) {
                out.add(new LeaderboardEntry(e.getKey(), new ApiPlayerStats(e.getValue())));
            }
            return out;
        });
    }

    public CompletableFuture<Optional<PlayerStatsView>> load(String name) {
        if (name == null || name.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        String wanted = name.trim();
        if (useSql.get()) {
            return onSql(c -> {
                PlayerStats s = PlayerSqlRepo.loadStats(c, wanted);
                return Optional.<PlayerStatsView>ofNullable(s == null ? null : new ApiPlayerStats(s));
            });
        }
        return onMain(() -> {
            Map<String, PlayerStats> all = players.get().getAllStats();
            PlayerStats s = all.get(wanted);
            if (s == null) {
                for (Map.Entry<String, PlayerStats> e : all.entrySet()) {
                    if (e.getKey().equalsIgnoreCase(wanted)) {
                        s = e.getValue();
                        break;
                    }
                }
            }
            return Optional.<PlayerStatsView>ofNullable(s == null ? null : new ApiPlayerStats(s));
        });
    }

    public CompletableFuture<List<String>> find(String prefix, int limit) {
        String p = prefix == null ? "" : prefix.trim();
        int n = clamp(limit);
        if (useSql.get()) return onSql(c -> PlayerSqlRepo.findNames(c, p, n));
        return onMain(() -> {
            String lower = p.toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (String key : players.get().getAllStats().keySet()) {
                if (key.toLowerCase(Locale.ROOT).startsWith(lower)) names.add(key);
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names.subList(0, Math.min(n, names.size()));
        });
    }

    private interface SqlCall<T> {
        T call(Connection c) throws SQLException;
    }

    private <T> CompletableFuture<T> onSql(SqlCall<T> call) {
        CompletableFuture<T> future = new CompletableFuture<>();
        sql.submit(c -> {
            try {
                future.complete(call.call(c));
            } catch (SQLException | RuntimeException e) {
                future.completeExceptionally(e);
                if (e instanceof SQLException sqlError) throw sqlError;
            }
        });
        return future.orTimeout(SQL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private <T> CompletableFuture<T> onMain(Supplier<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        mainThread.execute(() -> {
            try {
                future.complete(work.get());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
