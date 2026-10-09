package pl.lokos.tools.registry;

import org.bukkit.NamespacedKey;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.commands.RegionCommand;
import pl.lokos.tools.commands.LocationsCommand;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.RegionSelection;

import java.util.List;

/** Wszystkie polecenia rejestrowane w kodzie Java Paper, bez plugin.yml commands. */
public final class RegionCommandRegistry {
    private final JavaPlugin plugin;
    public RegionCommandRegistry(JavaPlugin plugin){this.plugin=plugin;}
    public void register(RegionManager regions, RegionSelection selection,
                         RegionMenuFactory menus, RankManager ranks,ToolsConfig.Regions settings,
                         NamespacedKey wandKey) {
        permit("tools.region.admin",PermissionDefault.OP);
        permit("tools.region.bypass",PermissionDefault.OP);
        permit("tools.lokalizacje",PermissionDefault.TRUE);
        plugin.registerCommand("region","Zarządzanie ochroną regionów",List.of(),
                new RegionCommand(plugin,regions,selection,menus,ranks,settings,wandKey));
        plugin.registerCommand("lokalizacje","Menu teleportacji do lokalizacji",List.of("lokacje"),
                new LocationsCommand(regions,menus));
    }
    private void permit(String name,PermissionDefault defaultValue) {
        if(plugin.getServer().getPluginManager().getPermission(name)==null)
            plugin.getServer().getPluginManager().addPermission(new Permission(name,defaultValue));
    }
}
