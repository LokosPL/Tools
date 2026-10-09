package pl.lokos.tools.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import pl.lokos.tools.config.CommandsFile;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;

import java.util.*;

/**
 * Ukrywa w podpowiedziach oraz blokuje ręczne wpisanie komend Tools bez dostępu.
 * Paper/Brigadier odfiltrowuje również polecenia innych pluginów według ich wymagań.
 */
public final class ToolsCommandVisibilityListener implements Listener {
    private final Map<String, String> rootPermissions;
    private final RankManager ranks;

    public ToolsCommandVisibilityListener(CommandsFile config, RankManager ranks) {
        Map<String, String> nodes=new HashMap<>();
        register(nodes,"tools",config.tools());
        register(nodes,"ranga",config.ranga());
        register(nodes,"region",config.region());
        register(nodes,"lokalizacje",config.lokalizacje());
        this.rootPermissions=Map.copyOf(nodes);
        this.ranks=ranks;
    }

    private static void register(Map<String,String> nodes,String primary,CommandsFile.Entry entry){
        if(!entry.enabled())return;
        nodes.put(primary.toLowerCase(Locale.ROOT),entry.permission());
        nodes.put("tools:"+primary.toLowerCase(Locale.ROOT),entry.permission());
        for(String alias:entry.aliases()){
            String normalized=alias.toLowerCase(Locale.ROOT);
            nodes.put(normalized,entry.permission());
            nodes.put("tools:"+normalized,entry.permission());
        }
    }

    public Map<String,String> registeredRoots(){return rootPermissions;}

    public static boolean allowed(Player player,RankManager ranks,String permission){
        // Gracze bez aktywnej rangi korzystają z uprawnień rangi 'gracz'.
        // Operator nadal może zarządzać serwerem.
        return player.isOp() || (ranks!=null &&
                (ranks.snapshot().permissionsFor(player.getUniqueId()).contains("*")
                || ranks.snapshot().permissionsFor(player.getUniqueId()).contains(permission)));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void send(PlayerCommandSendEvent event){
        Player player=event.getPlayer();
        event.getCommands().removeIf(command -> {
            String required=rootPermissions.get(command.toLowerCase(Locale.ROOT));
            return required!=null && !allowed(player,ranks,required);
        });
    }

    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)
    public void manual(PlayerCommandPreprocessEvent event){
        String raw=event.getMessage();
        if(raw==null || !raw.startsWith("/"))return;
        String command=raw.substring(1).split("\\s+",2)[0].toLowerCase(Locale.ROOT);
        String required=rootPermissions.get(command);
        if(required==null || allowed(event.getPlayer(),ranks,required))return;
        event.setCancelled(true);
        Messages.unknown(event.getPlayer());
    }
}
