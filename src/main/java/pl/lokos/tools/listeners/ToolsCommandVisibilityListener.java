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
import pl.lokos.tools.security.ToolsAccess;
import pl.lokos.tools.security.CommandVisibilityPolicy;

import java.util.*;

/**
 * Ukrywa w podpowiedziach oraz blokuje ręczne wpisanie komend Tools bez dostępu.
 * Paper/Brigadier odfiltrowuje również polecenia innych pluginów według ich wymagań.
 */
public final class ToolsCommandVisibilityListener implements Listener {
    private final Map<String, String> rootPermissions;
    private final RankManager ranks;
    private final Set<String> adminNodes;

    public ToolsCommandVisibilityListener(CommandsFile config, RankManager ranks) {
        Map<String, String> nodes=new HashMap<>();
        register(nodes,"tools",config.tools());
        register(nodes,"ranga",config.ranga());
        register(nodes,"region",config.region());
        register(nodes,"lokalizacje",config.lokalizacje());
        register(nodes,"chat",config.chat());
        register(nodes,"msg",config.msg());
        register(nodes,"reply",config.reply());
        register(nodes,"tp",config.tp());register(nodes,"vanish",config.vanish());
        register(nodes,"helpop",config.helpop());register(nodes,"gamemode",config.gamemode());
        register(nodes,"fly",config.fly());register(nodes,"broadcast",config.broadcast());
        register(nodes,"inventoryopen",config.inventoryopen());register(nodes,"speed",config.speed());
        register(nodes,"przedmiot",config.przedmiot());
        register(nodes,"antycheat",config.antycheat());
        register(nodes,"event",config.event());
        register(nodes,"skrzynia",config.skrzynia());
        register(nodes,"granica",config.granica());
        for(pl.lokos.tools.events.EventType type:pl.lokos.tools.events.EventType.values()){
            nodes.put(type.id(),"tools.event.info");
            nodes.put("tools:"+type.id(),"tools.event.info");
        }
        nodes.put("spawn", "tools.spawn");
        nodes.put("tools:spawn", "tools.spawn");
        // Whitelist była wcześniej pominięta: klient widział vanilla i aliasy Tools.
        for(String name:List.of("whitelist","bialalista","wl",
                "minecraft:whitelist","tools:whitelist","tools:bialalista","tools:wl"))
            nodes.put(name,"tools.whitelist.admin");
        this.rootPermissions=Map.copyOf(nodes);
        this.adminNodes=Set.of(config.tools().permission(),config.ranga().permission(),
                config.region().permission(),"tools.whitelist.admin",config.chat().permission(),
                config.tp().permission(),config.vanish().permission(),config.gamemode().permission(),
                config.fly().permission(),config.broadcast().permission(),
                config.inventoryopen().permission(),config.speed().permission(),
                config.przedmiot().permission(),config.antycheat().permission(),
                config.event().permission(),config.skrzynia().permission(),config.granica().permission());
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

    public static boolean allowed(Player player,RankManager ranks,String permission) {
        return ToolsAccess.allowed(player,ranks,permission,false);
    }

    private boolean canUse(Player player,String permission){
        // MSG jest publiczne domyślnie, a odrębne uprawnienia Tools chronią administrację.
        if("tools.msg.use".equals(permission))return player.hasPermission(permission);
        if("tools.helpop.use".equals(permission)||"tools.event.info".equals(permission))return true;
        if("tools.antycheat.admin".equals(permission))
            return ToolsAccess.allowed(player,ranks,permission,true)
                    || ToolsAccess.allowed(player,ranks,"tools.antycheat.alerts",false);
        return ToolsAccess.allowed(player,ranks,permission,adminNodes.contains(permission));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void send(PlayerCommandSendEvent event){
        Player player=event.getPlayer();
        boolean technicalAdmin=ToolsAccess.admin(player,ranks,"tools.admin");
        event.getCommands().removeIf(command -> {
            String root=command.toLowerCase(Locale.ROOT);
            String required=rootPermissions.get(root);
            // Zarejestrowane komendy Tools mają pierwszeństwo przed blacklistą
            // vanilla (/tp i /gamemode były na niej jeszcze przed ich wdrożeniem).
            if(required!=null)return !canUse(player,required);
            return !technicalAdmin && CommandVisibilityPolicy.hideFromUnprivileged(root);
        });
    }

    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)
    public void manual(PlayerCommandPreprocessEvent event){
        String raw=event.getMessage();
        if(raw==null || !raw.startsWith("/"))return;
        String command=raw.substring(1).split("\\s+",2)[0].toLowerCase(Locale.ROOT);
        String required=rootPermissions.get(command);
        Player player=event.getPlayer();
        boolean forbidden;
        if(required!=null)forbidden=!canUse(player,required);
        else forbidden=CommandVisibilityPolicy.hideFromUnprivileged(command)
                && !ToolsAccess.admin(player,ranks,"tools.admin");
        if(!forbidden)return;
        event.setCancelled(true);
        Messages.unknown(player);
    }
}
