package pl.lokos.tools.basic;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.inventorys.InventoryRegistry;
import pl.lokos.tools.listeners.PlayerConnectionListener;
import pl.lokos.tools.manager.PlayerDataManager;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankVisualManager;
import pl.lokos.tools.listeners.RankListener;
import pl.lokos.tools.registry.CommandRegistry;
import pl.lokos.tools.registry.ConfigRegistry;
import pl.lokos.tools.tasks.AutosaveTask;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class ToolsPlugin extends JavaPlugin {
    private DatabaseManager database;
    private PlayerDataManager playerData;
    private RankManager rankManager;
    private RankVisualManager rankVisuals;
    private InventoryRegistry inventories;
    private BukkitTask autosaveTask;
    private ConfigRegistry configurations;
    private ToolsConfig config;

    @Override
    public void onEnable() {
        // Klasy Java definiuja wartosci domyslne; JSON jest tworzony dopiero
        // podczas startu serwera. Wszystkie konfiguracje ladujemy PRZED
        // rejestracja komend, listenerow, baz danych i taskow.
        try {
            this.configurations = new ConfigRegistry(getDataFolder().toPath());
            configurations.loadAll();
            this.config = configurations.tools();
        } catch (IOException error) {
            getLogger().severe("Nie mozna zaladowac konfiguracji: " + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.inventories = new InventoryRegistry();
        PlayerRepository repository = null;
        if (config.database().enabled()) {
            try {
                this.database = new DatabaseManager(this, config.database());
                repository = new PlayerRepository(database);
                this.playerData = new PlayerDataManager(this, repository);
                this.rankManager = new RankManager(this, new RankRepository(database));
                this.rankVisuals = new RankVisualManager(this, rankManager, config.ranks());
                rankManager.setVisuals(rankVisuals);
            } catch (RuntimeException error) {
                getLogger().severe("Nie mozna uruchomic MySQL: " + error.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            getLogger().warning("MySQL wylaczony. Wlacz database.enabled w plugins/Tools/config.json.");
        }

        new CommandRegistry(this).register(config.commands(), database, repository, playerData, rankManager);
        if (rankManager != null) {
            getServer().getPluginManager().registerEvents(
                    new RankListener(rankManager, rankVisuals, config.ranks()), this);
            rankManager.start();
            getServer().getScheduler().runTaskTimer(this, rankVisuals::tick, 4L, 4L);
            getServer().getScheduler().runTaskTimer(this, rankManager::expire, 200L, 200L);
        }

        if (playerData != null) {
            getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerData), this);
            for (var player : Bukkit.getOnlinePlayers()) {
                playerData.playerJoined(player);
            }
            long interval = config.autosaveSeconds() * 20L;
            this.autosaveTask = new AutosaveTask(playerData).runTaskTimer(this, interval, interval);
        }
        getLogger().info("Tools zostal wlaczony na Paper 26.3.");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (rankManager != null) rankManager.stop();
        if (database != null) {
            CompletableFuture<Void> pending = playerData != null
                    ? playerData.shutdownAndFlush()
                    : CompletableFuture.completedFuture(null);
            database.shutdown(pending);
        }
        getLogger().info("Tools zostal wylaczony.");
    }

    public InventoryRegistry inventories() {
        return inventories;
    }

    public PlayerDataManager playerData() {
        return playerData;
    }

    public RankManager ranks() {
        return rankManager;
    }

    public ToolsConfig config() {
        return config;
    }

    public ConfigRegistry configurations() {
        return configurations;
    }
}
