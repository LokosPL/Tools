package pl.lokos.tools.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Messages;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Szybka blokada przed rendererem rang; bez zapytań SQL lub plików w AsyncChatEvent. */
public final class ChatListener implements Listener {
    private final JavaPlugin plugin;
    private final ChatManager chats;
    private final Map<UUID,Long> lastWarn = new ConcurrentHashMap<>();
    public ChatListener(JavaPlugin plugin,ChatManager chats){this.plugin=plugin;this.chats=chats;}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void chat(AsyncChatEvent event){
        UUID uuid=event.getPlayer().getUniqueId();
        var decision=chats.check(uuid,System.currentTimeMillis());
        if(decision==ChatPolicy.Decision.ALLOW)return;
        event.setCancelled(true);
        String message=switch(decision){
            case MUTED -> {
                var mute=chats.mute(uuid);
                yield chats.config().mutedMessage().replace("{reason}",mute==null?"Wyciszenie":mute.reason().replace("&",""));
            }
            case CLOSED -> chats.config().chatDisabledMessage();
            case RANK -> chats.config().rankRestrictedMessage();
            case SLOW -> chats.config().slowMessage()
                    .replace("{count}",Integer.toString(chats.config().slowMaxMessages()))
                    .replace("{seconds}",Integer.toString(chats.config().slowWindowSeconds()));
            default -> "";
        };
        if(!plugin.isEnabled())return;
        long now=System.currentTimeMillis();
        Long previous=lastWarn.put(uuid,now);
        if(previous!=null && now-previous<1200L)return;
        plugin.getServer().getScheduler().runTask(plugin,()->{
            if(event.getPlayer().isOnline())Messages.error(event.getPlayer(),message);
        });
    }
    @EventHandler public void quit(PlayerQuitEvent event){
        UUID id=event.getPlayer().getUniqueId();
        chats.leave(id);
        lastWarn.remove(id);
    }
}
