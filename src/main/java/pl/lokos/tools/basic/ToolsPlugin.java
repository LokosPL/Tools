package pl.lokos.tools.basic;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.inventorys.InventoryRegistry;
import pl.lokos.tools.listeners.PlayerConnectionListener;
import pl.lokos.tools.manager.PlayerDataManager;
import pl.lokos.tools.registry.CommandRegistry;
import pl.lokos.tools.tasks.AutosaveTask;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class ToolsPlugin extends JavaPlugin {
    private DatabaseManager database;
    private PlayerDataManager playerData;
    private InventoryRegistry inventories;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        final ToolsConfig config;
        try {
            config = new JsonConfigManager(getDataFolder().toPath()).load();
        } catch (IOException error) {
            getLogger().severe("Nie mozna odczytac konfiguracji: " + error.getMessage());
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
                getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerData), this);
                for (var player : Bukkit.getOnlinePlayers()) {
                    playerData.playerJoined(player);
                }
                long interval = config.autosaveSeconds() * 20L;
                this.autosaveTask = new AutosaveTask(playerData).runTaskTimer(this, interval, interval);
            } catch (RuntimeException error) {
                getLogger().severe("Nie mozna uruchomic MySQL: " + error.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            getLogger().warning("MySQL wylaczony. Wlacz database.enabled w plugins/Tools/config.json.");
        }
        new CommandRegistry(this).register(database, repository, playerData);
        getLogger().info("Tools zostal wlaczony na Paper 26.3.");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
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
}
