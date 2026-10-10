package pl.lokos.tools.basic;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.config.SecurityConfig;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.whitelist.*;
import pl.lokos.tools.commands.WhitelistCommand;
import pl.lokos.tools.listeners.WhitelistListener;
import pl.lokos.tools.skins.SkinService;
import pl.lokos.tools.security.AntiBotGuard;
import pl.lokos.tools.helpers.Colors;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.database.RegionRepository;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.inventorys.RankMenuFactory;
import pl.lokos.tools.listeners.RankMenuListener;
import pl.lokos.tools.manager.RegionBorderPreview;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.helpers.ToolsPermissionCatalog;
import pl.lokos.tools.listeners.ToolsCommandVisibilityListener;
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
import pl.lokos.tools.registry.ServiceRegistry;
import pl.lokos.tools.registry.PluginServiceFactory;
import pl.lokos.tools.diagnostics.MonitoringService;
import pl.lokos.tools.config.HotReloadService;
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
    private RegionBorderPreview borderPreview;
    private InventoryRegistry inventories;
    private BukkitTask autosaveTask;
    private ConfigRegistry configurations;
    private ToolsConfig config;
    private ServiceRegistry services;
    private MonitoringService monitoring;
    private HotReloadService reloadService;
    private RankListener rankListener;
    private WhitelistService whitelistService;
    private WhitelistCommand pendingWhitelistCommand;
    private WhitelistMenu pendingWhitelistMenu;

    @Override
    public void onEnable() {
        // Klasy Java definiuja wartosci domyslne; JSON jest tworzony dopiero
        // podczas startu serwera. Wszystkie konfiguracje ladujemy PRZED
        // rejestracja komend, listenerow, baz danych i taskow.
        try {
            this.configurations = new ConfigRegistry(getDataFolder().toPath());
            configurations.loadAll();
            this.config = configurations.tools();
            Messages.configure(configurations.commands().messagePrefix());
        } catch (IOException error) {
            getLogger().severe("Nie mozna zaladowac konfiguracji: " + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            SecurityConfig security=new JsonConfigManager(getDataFolder().toPath()).load(
                    "Security.json", SecurityConfig.class, SecurityConfig::new, SecurityConfig::validate);
            this.whitelistService=new WhitelistService(this);
            if (getServer().getPluginManager().getPermission("tools.whitelist.admin")==null)
                getServer().getPluginManager().addPermission(
                        new Permission("tools.whitelist.admin",PermissionDefault.OP));
            WhitelistMenu whitelistMenu=new WhitelistMenu(whitelistService);
            WhitelistCommand whitelistCommand=new WhitelistCommand(this,whitelistService,whitelistMenu);
            registerCommand("whitelist","Zarządzanie whitelistą",
                    java.util.List.of("bialalista","wl"),whitelistCommand);
            this.pendingWhitelistCommand=whitelistCommand;
            this.pendingWhitelistMenu=whitelistMenu;
            getServer().getPluginManager().registerEvents(new SkinService(this,security.premiumSkins()),this);
            getServer().getPluginManager().registerEvents(
                    new AntiBotGuard(this,whitelistService,security.antiBot()),this);
        } catch (IOException error) {
            getLogger().severe("Nie można uruchomić whitelisty: "+error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.inventories = new InventoryRegistry();
        this.services = new ServiceRegistry();
        PlayerRepository repository = null;
        try {
            PluginServiceFactory.Core core = PluginServiceFactory.create(
                    this, configurations, config, services);
            this.database = core.database();
            repository = core.players();
            this.playerData = core.playerData();
            this.rankManager = core.ranks();
            this.rankVisuals = core.tab();
            this.regionManager = core.regions();
            this.regionTeleports = core.teleports();
            if (database == null)
                getLogger().warning("Baza jest wyłączona; moduł rang i regionów nie będzie dostępny.");
        } catch (RuntimeException error) {
            getLogger().severe("Nie udało się utworzyć serwisów: " + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.monitoring = services.register(MonitoringService.class,
                new MonitoringService(this, database));
        this.monitoring.configure(17.0, 85);
        getServer().getScheduler().runTaskTimer(this,
                monitoring.measured("monitoring", monitoring::check), 200L, 200L);
        if (rankManager != null) {
            this.rankListener = new RankListener(rankManager, rankVisuals, config.ranks());
        }
        this.reloadService = services.register(HotReloadService.class,
                new HotReloadService(this, configurations, config, rankVisuals, rankListener));
        getServer().getPluginManager().registerEvents(new WhitelistListener(
                this,whitelistService,pendingWhitelistMenu,pendingWhitelistCommand,reloadService),this);

        RankMenuFactory rankMenus=null;
        if(rankManager!=null) {
            rankMenus=new RankMenuFactory(new NamespacedKey(this,"rank_menu"),rankManager,configurations.commands());
            getServer().getPluginManager().registerEvents(new RankMenuListener(
                    this,rankManager,rankMenus,configurations.commands().ranga().permission()),this);
        }
        new CommandRegistry(this).register(configurations.commands(), database,
                repository, playerData, rankManager, rankMenus, monitoring, reloadService);
        if(regionManager!=null) {
            NamespacedKey wandKey=new NamespacedKey(this,"region_wand");
            NamespacedKey menuKey=new NamespacedKey(this,"region_menu");
            RegionSelection selection=new RegionSelection();
            this.borderPreview=new RegionBorderPreview(this);
            RegionMenuFactory menus=new RegionMenuFactory(regionManager,rankManager,menuKey,config.regions().teleportSeconds());
            new RegionCommandRegistry(this).register(regionManager,selection,menus,rankManager,config.regions(),wandKey,configurations.commands(),regionTeleports);
            getServer().getPluginManager().registerEvents(
                    new RegionProtectionListener(regionManager,selection,wandKey,configurations.commands().region().permission()),this);
            RegionPlayerListener playerRegions=new RegionPlayerListener(
                    this,regionManager,regionTeleports,config.regions().barTitle());
            getServer().getPluginManager().registerEvents(playerRegions,this);
            getServer().getPluginManager().registerEvents(
                    new RegionMenuListener(this,menus,regionManager,regionTeleports,borderPreview,configurations.commands().region().permission(),configurations.commands().lokalizacje().permission()),this);
            regionManager.start();
            getServer().getScheduler().runTaskTimer(this,
                    monitoring.measured("regiony.actionbar", playerRegions::actionbar),20L,20L);
        }

        getServer().getPluginManager().registerEvents(
                new ToolsCommandVisibilityListener(configurations.commands(),rankManager),this);

        if (rankManager != null) {
            getServer().getPluginManager().registerEvents(
                    rankListener, this);
            rankManager.start();
            getServer().getScheduler().runTaskTimer(this,
                    monitoring.measured("rangi.napisF5", rankVisuals::tick), 2L, 2L);
            getServer().getScheduler().runTaskTimer(
                    this, monitoring.measured("rangi.TAB", rankVisuals::updateTab),
                    config.ranks().tabRefreshTicks(), config.ranks().tabRefreshTicks());
            getServer().getScheduler().runTaskTimer(this,
                    monitoring.measured("rangi.wygasanie", rankManager::expire), 20L, 20L);
        }

        if (playerData != null) {
            getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerData), this);
            for (var player : Bukkit.getOnlinePlayers()) {
                playerData.playerJoined(player);
            }
            long interval = config.autosaveSeconds() * 20L;
            this.autosaveTask = getServer().getScheduler().runTaskTimer(this,
                    monitoring.measured("gracze.zapis", playerData::autosave), interval, interval);
        }
        getLogger().info("Tools zostal wlaczony na Paper 26.3.");
    }

    @Override
    public void onDisable() {
        // Użytkownicy dostają estetyczny powód przy planowym wyłączeniu.
        if (whitelistService != null) {
            // Komunikat wyłączenia wyłącznie przy rzeczywistym STOP serwera.
            // Wyłączenie samego pluginu nie może wyrzucać wszystkich graczy.
            if (Bukkit.isStopping()) {
                for (var player : Bukkit.getOnlinePlayers()) {
                    try { player.kick(Colors.color(whitelistService.state().shutdownMessage())); }
                    catch (RuntimeException ignored) { }
                }
            }
            whitelistService.close();
        }
        if (reloadService != null) reloadService.close();
        if (monitoring != null) monitoring.stop();
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if(regionTeleports!=null) regionTeleports.cancelAll();
        if(borderPreview!=null) borderPreview.shutdown();
        if(regionManager!=null) regionManager.shutdown();
        if (rankManager != null) rankManager.stop();
        if (database != null) {
            CompletableFuture<Void> pending = playerData != null
                    ? playerData.shutdownAndFlush()
                    : CompletableFuture.completedFuture(null);
            database.shutdown(pending);
        }
        if (services != null) services.close();
        getLogger().info("Tools został wyłączony.");
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

    public MonitoringService monitoring() { return monitoring; }
    public ConfigRegistry configurations() {
        return configurations;
    }
}
