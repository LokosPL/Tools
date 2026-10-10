package pl.lokos.tools.msg;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Set;

/** Zarówno aliasy, jak i vanilla /minecraft:msg przechodzą przez ochronę Tools. */
public final class PrivateMessageListener implements Listener {
    private static final Set<String> WHISPER_ALIASES=Set.of(
            "tell","w","minecraft:msg","minecraft:tell","minecraft:w");
    private final PrivateMessageManager messages;
    public PrivateMessageListener(PrivateMessageManager messages){this.messages=messages;}

    /**
     * Chroni wyłączone skrzynki przed obejściem przez nazwę przestrzeni Minecraft.
     * Nie zmienia poleceń administracyjnych, ani nie dispatchuje komend rekurencyjnie.
     */
    public static String canonical(String raw) {
        if(raw==null || !raw.startsWith("/"))return raw;
        int split=raw.indexOf(' ');
        String root=(split<0?raw.substring(1):raw.substring(1,split))
                .toLowerCase(Locale.ROOT);
        if(!WHISPER_ALIASES.contains(root))return raw;
        return "/msg"+(split<0?"":raw.substring(split));
    }

    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true)
    public void command(PlayerCommandPreprocessEvent event){
        String incoming=event.getMessage();
        String canonical=canonical(incoming);
        if(!incoming.equals(canonical))event.setMessage(canonical);
    }

    @EventHandler public void quit(PlayerQuitEvent event){
        messages.leave(event.getPlayer().getUniqueId());
    }
}
