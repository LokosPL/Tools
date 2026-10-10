package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.lokos.tools.events.EventManager;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.Collection;
import java.util.List;

/** Publiczny panel eventów bez możliwości zarządzania wydarzeniami. */
public final class EventHubCommand implements BasicCommand {
    private final EventManager events;
    private final RankManager ranks;
    private final String permission;
    public EventHubCommand(EventManager events,RankManager ranks,String permission){
        this.events=events;this.ranks=ranks;this.permission=permission;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){
        return ToolsAccess.permitted(sender,ranks,permission);
    }
    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        if(args.length!=0){Messages.error(sender,"Użycie: /eventy");return;}
        if(!(sender instanceof Player player)){
            Messages.error(sender,"GUI dostępne tylko w grze.");return;
        }
        events.openHub(player);
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        return List.of();
    }
}
