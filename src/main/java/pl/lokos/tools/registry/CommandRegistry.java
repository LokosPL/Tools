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
import pl.lokos.tools.chat.ChatManager;
import pl.lokos.tools.msg.PrivateMessageManager;
import pl.lokos.tools.helpers.ToolsPermissionCatalog;
import pl.lokos.tools.staff.StaffManager;
import pl.lokos.tools.commands.StaffCommand;
import pl.lokos.tools.commands.InventoryAudit;
import pl.lokos.tools.items.SpecialItemService;
import pl.lokos.tools.items.SpecialItemMenu;
import pl.lokos.tools.anticheat.AntiCheatManager;
import pl.lokos.tools.events.*;
import pl.lokos.tools.border.BorderManager;
import pl.lokos.tools.crates.CrateManager;

public final class CommandRegistry {
    private final JavaPlugin plugin;
    public CommandRegistry(JavaPlugin plugin){this.plugin=plugin;}
    public void register(CommandsFile config,DatabaseManager database,PlayerRepository repository,
                         PlayerDataManager playerData,RankManager ranks,RankMenuFactory menus,
                         MonitoringService monitoring, HotReloadService hotReload,ChatManager chats,
                         PrivateMessageManager privateMessages,StaffManager staff,InventoryAudit inventoryAudit,
                         SpecialItemService items,SpecialItemMenu itemMenu,AntiCheatManager anticheat,
                         EventManager events,CrateManager crates,BorderManager border) {
        if(ranks!=null && config.ranga().enabled()){
            declare(config.ranga().permission(),PermissionDefault.OP);
            plugin.registerCommand("ranga",config.ranga().description(),config.ranga().aliases(),
                    new RankCommand(plugin,ranks,config.ranga().permission(),menus));
        }
        if(chats!=null && config.chat().enabled()){
            declare(config.chat().permission(),PermissionDefault.OP);
            plugin.registerCommand("chat",config.chat().description(),config.chat().aliases(),
                    new ChatCommand(plugin,chats,ranks,config.chat().permission()));
        }
        if(privateMessages!=null && config.msg().enabled()){
            declare("tools.msg.use",PermissionDefault.TRUE);
            declare("tools.msg.staff",PermissionDefault.FALSE);
            declare("tools.msg.protected",PermissionDefault.FALSE);
            declare("tools.msg.bypass.cooldown",PermissionDefault.FALSE);
            var adminNodes=new ToolsPermissionCatalog(config).administrativeNodes();
            plugin.registerCommand("msg",config.msg().description(),config.msg().aliases(),
                    new PrivateMessageCommand(plugin,privateMessages,chats,ranks,adminNodes,false,
                            config.chat().permission()));
            if(config.reply().enabled())
                plugin.registerCommand("reply",config.reply().description(),config.reply().aliases(),
                        new PrivateMessageCommand(plugin,privateMessages,chats,ranks,adminNodes,true,
                                config.chat().permission()));
        }
        if(staff!=null){
            var definitions=java.util.List.of(
                    new Object[]{"tp",config.tp(),StaffCommand.Kind.TP},
                    new Object[]{"vanish",config.vanish(),StaffCommand.Kind.VANISH},
                    new Object[]{"helpop",config.helpop(),StaffCommand.Kind.HELPOP},
                    new Object[]{"gamemode",config.gamemode(),StaffCommand.Kind.GAMEMODE},
                    new Object[]{"fly",config.fly(),StaffCommand.Kind.FLY},
                    new Object[]{"broadcast",config.broadcast(),StaffCommand.Kind.BROADCAST},
                    new Object[]{"inventoryopen",config.inventoryopen(),StaffCommand.Kind.INVENTORYOPEN},
                    new Object[]{"speed",config.speed(),StaffCommand.Kind.SPEED});
            for(Object[] item:definitions) {
                String name=(String)item[0];CommandsFile.Entry entry=(CommandsFile.Entry)item[1];
                if(!entry.enabled())continue;
                declare(entry.permission(),name.equals("helpop")?PermissionDefault.TRUE:PermissionDefault.OP);
                plugin.registerCommand(name,entry.description(),entry.aliases(),
                        new StaffCommand(plugin,ranks,staff,inventoryAudit,(StaffCommand.Kind)item[2],entry.permission()));
            }
        }
        if(items!=null && config.przedmiot().enabled()){
            declare(config.przedmiot().permission(),PermissionDefault.OP);
            plugin.registerCommand("przedmiot",config.przedmiot().description(),
                    config.przedmiot().aliases(),
                    new SpecialItemCommand(items,itemMenu,ranks,config.przedmiot().permission()));
        }
        if(anticheat!=null&&config.antycheat().enabled()){
            declare(config.antycheat().permission(),PermissionDefault.OP);
            declare("tools.antycheat.alerts",PermissionDefault.OP);
            plugin.registerCommand("antycheat",config.antycheat().description(),
                    config.antycheat().aliases(),new AntiCheatCommand(plugin,anticheat,ranks,
                            config.antycheat().permission()));
        }
        if(events!=null&&config.event().enabled()){
            declare(config.event().permission(),PermissionDefault.OP);
            declare("tools.event.info",PermissionDefault.TRUE);
            plugin.registerCommand("event",config.event().description(),config.event().aliases(),
                    new EventCommand(plugin,events,ranks,null,config.event().permission()));
            for(EventType type:EventType.values())
                plugin.registerCommand(type.id(),"Informacje: "+type.title(),java.util.List.of(),
                        new EventCommand(plugin,events,ranks,type,"tools.event.info"));
        }
        if(crates!=null&&config.skrzynia().enabled()){
            declare(config.skrzynia().permission(),PermissionDefault.OP);
            plugin.registerCommand("skrzynia",config.skrzynia().description(),config.skrzynia().aliases(),
                    new CrateCommand(crates,ranks,config.skrzynia().permission()));
        }
        if(border!=null&&config.granica().enabled()){
            declare(config.granica().permission(),PermissionDefault.OP);
            plugin.registerCommand("granica",config.granica().description(),config.granica().aliases(),
                    new BorderCommand(plugin,border,ranks,config.granica().permission()));
        }
        if(config.tools().enabled()){
            declare(config.tools().permission(),PermissionDefault.OP);
            plugin.registerCommand("tools",config.tools().description(),config.tools().aliases(),
                    new ToolsCommand(plugin,database,repository,playerData,config.tools().permission(), monitoring,hotReload,ranks));
        }
    }
    private void declare(String name,PermissionDefault value){
        if(plugin.getServer().getPluginManager().getPermission(name)==null)
            plugin.getServer().getPluginManager().addPermission(new Permission(name,value));
    }
}
