package pl.lokos.tools.basic;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.database.RegionRepository;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.listeners.RegionProtectionListener;
import pl.lokos.tools.listeners.RegionPlayerListener;
import pl.lokos.tools.listeners.RegionMenuListener;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.manager.RegionTeleportManager;
import pl.lokos.tools.region.RegionSelection;
import pl.lokos.tools.registry.RegionCommandRegistry;
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
    private RegionManager regionManager;
    private RegionTeleportManager regionTeleports;
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
                this.rankManager = new RankManager(this, new RankRepository(database), configurations.definitions());
                this.rankVisuals = new RankVisualManager(this, rankManager, config.ranks());
                rankManager.setVisuals(rankVisuals);
                if(config.regions().enabled()) {
                    this.regionManager=new RegionManager(this,new RegionRepository(database),rankManager,configurations.definitions());
                    this.regionTeleports=new RegionTeleportManager(this,regionManager,rankManager,config.regions());
                }
            } catch (RuntimeException error) {
                getLogger().severe("Nie mozna uruchomic MySQL: " + error.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            getLogger().warning("MySQL wylaczony. Wlacz enabled w plugins/Tools/MySql.json.");
        }

        new CommandRegistry(this).register(configurations.commands(), database, repository, playerData, rankManager);
        if(regionManager!=null) {
            NamespacedKey wandKey=new NamespacedKey(this,"region_wand");
            NamespacedKey menuKey=new NamespacedKey(this,"region_menu");
            RegionSelection selection=new RegionSelection();
            RegionMenuFactory menus=new RegionMenuFactory(regionManager,menuKey,config.regions().teleportSeconds());
            new RegionCommandRegistry(this).register(regionManager,selection,menus,rankManager,config.regions(),wandKey,configurations.commands());
            getServer().getPluginManager().registerEvents(
                    new RegionProtectionListener(regionManager,selection,wandKey),this);
            RegionPlayerListener playerRegions=new RegionPlayerListener(
                    this,regionManager,regionTeleports,config.regions().barTitle());
            getServer().getPluginManager().registerEvents(playerRegions,this);
            getServer().getPluginManager().registerEvents(
                    new RegionMenuListener(this,menus,regionManager,regionTeleports),this);
            regionManager.start();
            getServer().getScheduler().runTaskTimer(this,playerRegions::actionbar,20L,20L);
        }

        if (rankManager != null) {
            getServer().getPluginManager().registerEvents(
                    new RankListener(rankManager, rankVisuals, config.ranks()), this);
            rankManager.start();
            getServer().getScheduler().runTaskTimer(this, rankVisuals::tick, 4L, 4L);
            getServer().getScheduler().runTaskTimer(
                    this, rankVisuals::updateTab,
                    config.ranks().tabRefreshTicks(), config.ranks().tabRefreshTicks());
            getServer().getScheduler().runTaskTimer(this, rankManager::expire, 20L, 20L);
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
        if(regionTeleports!=null) regionTeleports.cancelAll();
        if(regionManager!=null) regionManager.shutdown();
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

    public RegionManager regions() {
        return regionManager;
    }

    public ToolsConfig config() {
        return config;
    }

    public ConfigRegistry configurations() {
        return configurations;
    }
}
