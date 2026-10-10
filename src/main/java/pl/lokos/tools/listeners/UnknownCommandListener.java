package pl.lokos.tools.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import pl.lokos.tools.helpers.Messages;

/** Nie przechwytuje składni istniejących poleceń innych pluginów. */
public final class UnknownCommandListener implements Listener {
    @EventHandler(priority=EventPriority.HIGHEST)
    public void unknown(UnknownCommandEvent event) {
        if(event.getSender() instanceof Player)
            event.message(Messages.unknownComponent());
    }
}
