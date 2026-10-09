package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.RegionManager;

public final class LocationsCommand implements BasicCommand {
    private final RegionManager regions;
    private final RegionMenuFactory menus;
    public LocationsCommand(RegionManager regions,RegionMenuFactory menus) {
        this.regions=regions;this.menus=menus;
    }
    @Override public String permission() {return "tools.lokalizacje";}
    @Override public void execute(CommandSourceStack source,String[] args) {
        if(!(source.getSender() instanceof Player player)) {
            Messages.error(source.getSender(),"Menu lokalizacji jest dostępne tylko w grze.");return;
        }
        if(args.length!=0) {Messages.usage(player,"/lokalizacje");return;}
        if(!regions.ready()) {Messages.error(player,"Lokalizacje jeszcze się wczytują.");return;}
        menus.locations(player,0);
    }
}
