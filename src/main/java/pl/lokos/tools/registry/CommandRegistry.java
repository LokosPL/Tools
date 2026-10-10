package pl.lokos.tools.registry;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.commands.*;
import pl.lokos.tools.config.CommandsFile;
import pl.lokos.tools.database.*;
import pl.lokos.tools.manager.*;
import pl.lokos.tools.inventorys.RankMenuFactory;
import pl.lokos.tools.diagnostics.MonitoringService;
import pl.lokos.tools.config.HotReloadService;

public final class CommandRegistry {
    private final JavaPlugin plugin;
    public CommandRegistry(JavaPlugin plugin){this.plugin=plugin;}
    public void register(CommandsFile config,DatabaseManager database,PlayerRepository repository,
                         PlayerDataManager playerData,RankManager ranks,RankMenuFactory menus,
                         MonitoringService monitoring, HotReloadService hotReload) {
        if(ranks!=null && config.ranga().enabled()){
            declare(config.ranga().permission(),PermissionDefault.OP);
            plugin.registerCommand("ranga",config.ranga().description(),config.ranga().aliases(),
                    new RankCommand(plugin,ranks,config.ranga().permission(),menus));
        }
        if(config.tools().enabled()){
            declare(config.tools().permission(),PermissionDefault.OP);
            plugin.registerCommand("tools",config.tools().description(),config.tools().aliases(),
                    new ToolsCommand(plugin,database,repository,playerData,config.tools().permission(), monitoring,hotReload));
        }
    }
    private void declare(String name,PermissionDefault value){
        if(plugin.getServer().getPluginManager().getPermission(name)==null)
            plugin.getServer().getPluginManager().addPermission(new Permission(name,value));
    }
}
