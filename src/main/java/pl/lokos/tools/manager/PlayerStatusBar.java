package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.region.Region;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jedyny nadawca action bara Tools. Brak regionu i powiadomień oznacza brak pakietu,
 * nie pusty tekst kasujący komunikaty innych pluginów.
 */
public final class PlayerStatusBar {
    public enum Priority { INFO, WARNING, PROTECTION }
    private record Notice(String message, long untilMillis, Priority priority) {}

    private final RegionManager regions;
    private final ToolsConfig.ActionBar settings;
    private final Map<UUID, Notice> notices = new HashMap<>();
    private final Map<UUID, Long> protectedCooldown = new HashMap<>();

    public PlayerStatusBar(RegionManager regions, ToolsConfig.ActionBar settings) {
        this.regions = regions;
        this.settings = settings;
    }

    /** Wszystkie podsystemy przekazują powiadomienia tutaj, nigdy do sendActionBar. */
    public void notice(Player player, String message, long durationMillis) {
        notice(player, message, durationMillis, Priority.INFO);
    }

    public void notice(Player player, String message, long durationMillis, Priority priority) {
        if (!settings.enabled() || message == null || message.isBlank()) return;
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Notice previous = notices.get(id);
        if (previous != null && previous.untilMillis() > now
                && previous.priority().ordinal() > priority.ordinal()) return;
        notices.put(id, new Notice(message, now + Math.max(250, Math.min(5000, durationMillis)), priority));
    }

    public void protectedArea(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        if (now - protectedCooldown.getOrDefault(id, 0L) < 800L) return;
        protectedCooldown.put(id, now);
        notice(player, settings.protectedMessage(), settings.protectionMillis(), Priority.PROTECTION);
    }

    public void deniedEntry(Player player) {
        notice(player, CommandTextRegistry.text("region", "noEntry"), 2600, Priority.WARNING);
    }

    public void remove(UUID uuid) {
        notices.remove(uuid);
        protectedCooldown.remove(uuid);
    }

    public void tick() {
        if (!settings.enabled()) return;
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Notice current = notices.get(id);
            if (current != null && current.untilMillis() <= now) {
                notices.remove(id);
                current = null;
            }
            Region region = regions.visibleAt(player.getLocation());
            String message = format(region == null ? null : region.name(),
                    current == null ? null : current.message(), settings);
            if (message != null) player.sendActionBar(Colors.color(message));
        }
    }

    /** Funkcja bez Bukkit API; null znaczy: nie wysyłaj żadnego action bara. */
    public static String format(String region, String notice, ToolsConfig.ActionBar settings) {
        if (!settings.enabled()) return null;
        String extra = notice == null ? "" : notice.replace('\n', ' ').replace('\r', ' ').trim();
        if (extra.length() > 180) extra = extra.substring(0, 177) + "…";
        if (region == null || region.isBlank())
            return extra.isBlank() ? null : extra;
        String safe = region.replace('\n', ' ').replace('\r', ' ').replace('&', ' ');
        if (safe.length() > 24) safe = safe.substring(0, 21) + "…";
        String location = settings.locationTemplate().replace("{region}", safe);
        return extra.isBlank() ? location : location + settings.separator() + extra;
    }
}
