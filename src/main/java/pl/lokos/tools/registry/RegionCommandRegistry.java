package pl.lokos.tools.registry;

import org.bukkit.NamespacedKey;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.commands.*;
import pl.lokos.tools.config.*;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.*;
import pl.lokos.tools.region.RegionSelection;

public final class RegionCommandRegistry {
    private final JavaPlugin plugin;
    public RegionCommandRegistry(JavaPlugin plugin){this.plugin=plugin;}
    public void register(RegionManager regions,RegionSelection selection,RegionMenuFactory menus,
                         RankManager ranks,ToolsConfig.Regions settings,NamespacedKey wandKey,
                         CommandsFile commands){
        permit("tools.region.bypass",PermissionDefault.OP);
        if(commands.region().enabled()){
            permit(commands.region().permission(),PermissionDefault.OP);
            plugin.registerCommand("region",commands.region().description(),commands.region().aliases(),
                    new RegionCommand(plugin,regions,selection,menus,ranks,settings,wandKey,
                            commands.region().permission()));
        }
        if(commands.lokalizacje().enabled()){
            permit(commands.lokalizacje().permission(),PermissionDefault.FALSE);
            plugin.registerCommand("lokalizacje",commands.lokalizacje().description(),
                    commands.lokalizacje().aliases(),
                    new LocationsCommand(regions,menus,commands.lokalizacje().permission()));
        }
        permit("tools.lokalizacje.instant",PermissionDefault.FALSE);
    }
    private void permit(String name,PermissionDefault value){
        if(plugin.getServer().getPluginManager().getPermission(name)==null)
            plugin.getServer().getPluginManager().addPermission(new Permission(name,value));
    }
}
