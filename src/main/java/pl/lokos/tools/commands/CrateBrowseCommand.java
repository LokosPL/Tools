package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.lokos.tools.crates.CrateManager;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.Collection;
import java.util.List;

/** Publiczne panele skrzyń i kluczy, bez tworzenia lub wydawania kluczy. */
public final class CrateBrowseCommand implements BasicCommand {
    private final CrateManager crates;
    private final RankManager ranks;
    private final String permission;
    private final boolean keys;
    public CrateBrowseCommand(CrateManager crates,RankManager ranks,String permission,boolean keys){
        this.crates=crates;this.ranks=ranks;this.permission=permission;this.keys=keys;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){
        return ToolsAccess.permitted(sender,ranks,permission);
    }
    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        if(args.length!=0){Messages.error(sender,"Użycie: /"+(keys?"klucze":"skrzynie"));return;}
        if(!(sender instanceof Player player)){
            Messages.error(sender,"GUI dostępne tylko w grze.");return;
        }
        if(keys)crates.keySummary(player);
        crates.browseMenu(player);
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        return List.of();
    }
}
