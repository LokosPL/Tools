package pl.lokos.tools.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.lokos.tools.manager.PlayerDataManager;

public final class PlayerConnectionListener implements Listener {
    private final PlayerDataManager playerData;

    public PlayerConnectionListener(PlayerDataManager playerData) {
        this.playerData = playerData;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerData.playerJoined(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerData.playerQuit(event.getPlayer());
    }
}
