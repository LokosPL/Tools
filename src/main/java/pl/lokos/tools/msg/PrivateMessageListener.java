package pl.lokos.tools.msg;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Porządek danych odpowiedzi po rozłączeniu. */
public final class PrivateMessageListener implements Listener {
    private final PrivateMessageManager messages;
    public PrivateMessageListener(PrivateMessageManager messages){this.messages=messages;}
    @EventHandler public void quit(PlayerQuitEvent event){
        messages.leave(event.getPlayer().getUniqueId());
    }
}
