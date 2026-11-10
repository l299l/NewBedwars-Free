package com.l299l.newbedwars.proxy.velocity;

import com.google.inject.Inject;
import com.l299l.newbedwars.common.protocol.NetChannel;
import com.l299l.newbedwars.common.protocol.NetCodec;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.sql.JdbcConnector;
import com.l299l.newbedwars.common.sql.ServerStatusRepo;
import com.l299l.newbedwars.common.update.Channel;
import com.l299l.newbedwars.proxy.core.party.PartyService;
import com.l299l.newbedwars.proxy.core.ProxyMessages;
import com.l299l.newbedwars.proxy.velocity.command.PartyCommand;
import com.l299l.newbedwars.proxy.velocity.command.StatusCommand;
import com.l299l.newbedwars.proxy.core.routing.Router;
import com.l299l.newbedwars.proxy.core.routing.ServerRegistry;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = "newbedwars-proxy", name = "NewBedwars-Proxy", version = "2.6-beta",
        description = "Network mode for NewBedwars: routing, server states and parties", authors = {"l299l"})
public final class NewBedwarsProxy {
    static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(NetChannel.MAIN);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;
    private final Object lock = new Object();
    private ProxyConfig config;
    private ServerRegistry servers;
    private PartyService parties;
    private Router router;
    private JdbcConnector database;
    private final Set<String> warnedServers = ConcurrentHashMap.newKeySet();
    private long lastDbError;
    private ProxyUpdateCheck updateCheck;

    @Inject
    public NewBedwarsProxy(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent e) throws Exception {
        config = ProxyConfig.load(dataDir);
        ProxyMessages messages = new ProxyMessages();
        for (String lang : List.of("en", "pl")) {
            messages.put(lang, ProxyConfig.loadLanguage(dataDir.resolve("lang").resolve(lang + ".yml"), "lang/" + lang + ".yml"));
        }
        VelocityPlatform platform = new VelocityPlatform(proxy, CHANNEL, messages, lock);
        servers = new ServerRegistry(config.gameServers, config.lobbyServers, config.stateStaleSeconds * 1000L);
        parties = new PartyService(platform, config.partyInviteSeconds * 1000L);
        router = new Router(platform, servers, parties, config.ticketTimeoutSeconds * 1000L);
        router.setLogger(logger::info);
        servers.setLogger(logger::warn);

        proxy.getChannelRegistrar().register(CHANNEL);
        var cm = proxy.getCommandManager();
        cm.register(cm.metaBuilder("party").aliases("p").plugin(this).build(), new PartyCommand(proxy, lock, parties, router));
        String version = proxy.getPluginManager().fromInstance(this)
                .flatMap(container -> container.getDescription().getVersion()).orElse("unknown");
        if (config.updatesCheck) {
            updateCheck = new ProxyUpdateCheck(logger, version,
                    Channel.parse(config.updatesChannel, Channel.BETA),
                    config.updatesNotify);
            proxy.getScheduler().buildTask(this, updateCheck::run).delay(10, TimeUnit.SECONDS).repeat(12, TimeUnit.HOURS).schedule();
        }
        cm.register(cm.metaBuilder("nbwproxy").plugin(this).build(),
                new StatusCommand(lock, servers, () -> updateCheck == null ? null : updateCheck.status()));

        proxy.getScheduler().buildTask(this, this::tick).repeat(1, TimeUnit.SECONDS).schedule();
        proxy.getScheduler().buildTask(this, () -> {
            synchronized (lock) {
                router.pushState(System.currentTimeMillis(), true);
            }
        }).repeat(5, TimeUnit.SECONDS).schedule();
        if (config.mysqlEnabled) {
            database = new JdbcConnector(config.mysqlHost, config.mysqlPort, config.mysqlDatabase, config.mysqlUser,
                    config.mysqlPassword, config.mysqlSslMode, config.mysqlAllowPublicKeyRetrieval, NewBedwarsProxy.class.getClassLoader());
            proxy.getScheduler().buildTask(this, this::pollDatabase).repeat(config.mysqlPollSeconds, TimeUnit.SECONDS).schedule();
        }
        logger.info("NewBedwars-Proxy enabled: game servers {}, lobby servers {}, MySQL {}",
                config.gameServers, config.lobbyServers, config.mysqlEnabled ? "on" : "off");
        for (String s : config.gameServers) if (proxy.getServer(s).isEmpty()) logger.warn("Game server '{}' is not in velocity.toml", s);
        for (String s : config.lobbyServers) if (proxy.getServer(s).isEmpty()) logger.warn("Lobby server '{}' is not in velocity.toml", s);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent e) {
        if (database != null) database.close();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            parties.expireInvites(now);
            router.tick(now);
            router.pushState(now, false);
        }
    }

    private void pollDatabase() {
        try {
            var c = database.get();
            long now = System.currentTimeMillis();
            List<ServerState> states = ServerStatusRepo.readAll(c);
            synchronized (lock) {
                servers.mergeFromDatabase(states, now);
            }
        } catch (Exception ex) {
            long now = System.currentTimeMillis();
            if (now - lastDbError > 60_000L) {
                lastDbError = now;
                logger.warn("Could not read {} from MySQL: {}", ServerStatusRepo.TABLE, ex.getMessage());
            }
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!CHANNEL.equals(e.getIdentifier())) return;
        e.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(e.getSource() instanceof ServerConnection conn)) return;
        NetMessage msg = NetCodec.decode(e.getData());
        if (msg == null) {
            if (warnedServers.add(conn.getServerInfo().getName())) {
                logger.warn("Dropped an unreadable message from {} (different NewBedwars version? Further ones from it are not logged)",
                        conn.getServerInfo().getName());
            }
            return;
        }
        synchronized (lock) {
            router.handle(conn.getServerInfo().getName(), conn.getPlayer().getUniqueId(), msg, System.currentTimeMillis());
        }
    }

    @Subscribe
    public void onServerConnected(ServerPostConnectEvent e) {
        Player p = e.getPlayer();
        p.getCurrentServer().ifPresent(conn -> {
            synchronized (lock) {
                router.onServerConnected(p.getUniqueId(), conn.getServerInfo().getName(), System.currentTimeMillis());
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent e) {
        synchronized (lock) {
            parties.onDisconnect(e.getPlayer().getUniqueId());
            router.onDisconnect(e.getPlayer().getUniqueId());
        }
    }
}
