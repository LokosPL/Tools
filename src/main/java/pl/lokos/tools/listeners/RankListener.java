package pl.lokos.tools.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.manager.RankVisualManager;

public final class RankListener implements Listener {
    private final RankManager ranks;
    private final RankVisualManager visuals;
    private final ToolsConfig.Ranks config;

    public RankListener(RankManager ranks, RankVisualManager visuals, ToolsConfig.Ranks config) {
        this.ranks = ranks;
        this.visuals = visuals;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void join(PlayerJoinEvent event) {
        ranks.apply(event.getPlayer());
        RankSnapshot.Rank rank = ranks.snapshot().forPlayer(event.getPlayer().getUniqueId());
        String message = rank == null ? null : rank.joinMessage();
        if (message == null || message.isBlank()) {
            event.joinMessage(null);
        } else {
            event.joinMessage(Colors.color(message
                    .replace("{nick}", event.getPlayer().getName())
                    .replace("{ranga}", rank.name())));
        }
        visuals.refresh();
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        ranks.leave(event.getPlayer());
        event.quitMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void chat(AsyncChatEvent event) {
        // Renderer korzysta z immutable snapshot i nie wywoluje Bukkit API na workerze.
        RankSnapshot snapshot = ranks.snapshot();
        RankSnapshot.Rank rank = snapshot.forPlayer(event.getPlayer().getUniqueId());
        String prefix = rank == null ? config.defaultPrefix() : rank.prefix();
        String suffix = rank == null ? "" : rank.suffix();
        Component rankText = Colors.color(prefix);
        Component endText = Colors.color(suffix);
        event.renderer((sender, displayName, message, viewer) ->
                rankText.append(Component.text(sender.getName(), NamedTextColor.WHITE))
                        .append(endText)
                        .append(Colors.color(" &8» &f"))
                        .append(message));
    }
}
