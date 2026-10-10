package pl.lokos.tools.registry;

import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.database.*;
import pl.lokos.tools.helpers.ToolsPermissionCatalog;
import pl.lokos.tools.manager.*;

/**
 * Jawny skład zależności: serwisy mają wyłącznie potrzebne im obiekty.
 * Utworzenie nowych modułów nie wymaga rozbudowywania klasy JavaPlugin.
 * Bez refleksji i nadmiarowego kontenera DI.
 */
public final class PluginServiceFactory {
    private PluginServiceFactory() {}

    public record Core(DatabaseManager database, PlayerRepository players,
                       PlayerDataManager playerData, RankManager ranks,
                       RankVisualManager tab, RegionManager regions,
                       RegionTeleportManager teleports) {}

    public static Core create(JavaPlugin plugin, ConfigRegistry definitions,
                              ToolsConfig config, ServiceRegistry services) {
        if (!config.database().enabled())
            return new Core(null, null, null, null, null, null, null);
        DatabaseManager database = services.register(DatabaseManager.class,
                new DatabaseManager(plugin, config.database()));
        services.register(DatabaseExecutor.class, database);

        PlayerRepository players = services.register(PlayerRepository.class,
                new PlayerRepository(services.require(DatabaseExecutor.class)));
        PlayerDataManager playerData = services.register(PlayerDataManager.class,
                new PlayerDataManager(plugin, players));
        RankRepository rankRepository = services.register(RankRepository.class,
                new RankRepository(services.require(DatabaseExecutor.class)));
        RankManager ranks = services.register(RankManager.class,
                new RankManager(plugin, rankRepository, definitions.definitions()));
        ToolsPermissionCatalog catalog=new ToolsPermissionCatalog(definitions.commands());
        ranks.setManagedPermissions(catalog.managedNodes());
        ranks.setAdminPermissions(catalog.administrativeNodes());
        RankVisualManager tab = services.register(RankVisualManager.class,
                new RankVisualManager(plugin, ranks, config.ranks()));
        ranks.setVisuals(tab);

        RegionManager regions = null;
        RegionTeleportManager teleports = null;
        if (config.regions().enabled()) {
            RegionRepository regionRepository = services.register(RegionRepository.class,
                    new RegionRepository(services.require(DatabaseExecutor.class)));
            regions = services.register(RegionManager.class,
                    new RegionManager(plugin, regionRepository, ranks, definitions.definitions()));
            teleports = services.register(RegionTeleportManager.class,
                    new RegionTeleportManager(plugin, regions, ranks, config.regions()));
        }
        return new Core(database, players, playerData, ranks, tab, regions, teleports);
    }
}
