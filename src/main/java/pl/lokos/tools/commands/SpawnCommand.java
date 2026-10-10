package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.manager.RegionTeleportManager;

/** /spawn używa dokładnie tego samego silnika teleportacji, co /lokalizacje. */
public final class SpawnCommand implements BasicCommand {
    private final RegionManager regions;
    private final RegionTeleportManager teleport;
    public SpawnCommand(RegionManager regions,RegionTeleportManager teleport) {
        this.regions=regions;this.teleport=teleport;
    }
    @Override public String permission() { return "tools.spawn"; }
    @Override public void execute(CommandSourceStack stack,String[] args) {
        if(!(stack.getSender() instanceof Player player)) {
            Messages.error(stack.getSender(),"Ta komenda jest dostępna tylko w grze.");return;
        }
        if(args.length!=0) {Messages.usage(player,"/spawn");return;}
        if(!regions.ready()) {Messages.error(player,"Lokalizacje jeszcze się wczytują.");return;}
        var main = regions.mainSpawn();
        if(main==null || main.spawn()==null) {
            Messages.error(player,"Spawn nie został ustawiony.");
            Messages.hint(player,"Administrator może ustawić główną lokalizację przez /region.");return;
        }
        teleport.start(player,main.name());
    }
}
