package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.listeners.ToolsCommandVisibilityListener;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.RegionManager;

public final class LocationsCommand implements BasicCommand {
    private static final pl.lokos.tools.helpers.CommandMessages display = new pl.lokos.tools.helpers.CommandMessages("lokalizacje");
    private final RegionManager regions;
    private final RegionMenuFactory menus;
    private final String commandPermission;
    public LocationsCommand(RegionManager regions,RegionMenuFactory menus,String permission) {
        this.regions=regions;this.menus=menus;this.commandPermission=permission;
    }
    @Override public String permission() {return commandPermission;}
    @Override public void execute(CommandSourceStack source,String[] args) {
        if(!(source.getSender() instanceof Player player)) {
            CommandTextRegistry.error(source.getSender(),"lokalizacje","onlyPlayer");return;
        }
        if(!ToolsCommandVisibilityListener.allowed(player,regions.ranks(),commandPermission)) {
            display.unknown(player);return;
        }
        if(args.length!=0) {display.usage(player,"/lokalizacje");return;}
        if(!regions.ready()) {CommandTextRegistry.error(player,"lokalizacje","notReady");return;}
        menus.locations(player,0);
    }
}
