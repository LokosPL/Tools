package pl.lokos.tools.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.region.Region;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jedyny nadawca action bara Tools. Wykorzystuje tick Paper, nie tworzy
 * osobnego taska ani zapytań do bazy podczas odświeżania widoku.
 */
public final class PlayerStatusBar {
    private record Notice(String message, long untilMillis) {}
    private final RegionManager regions;
    private final Map<UUID, Notice> notices = new HashMap<>();

    public PlayerStatusBar(RegionManager regions) {
        this.regions = regions;
    }

    /** Nie wysyła bezpośrednio actionbara, tylko ustawia krótki komunikat boczny. */
    public void notice(Player player, String message, long durationMillis) {
        long now = System.currentTimeMillis();
        notices.put(player.getUniqueId(),
                new Notice(message, now + Math.max(250, Math.min(5000, durationMillis))));
    }

    public void protectedArea(Player player) {
        notice(player, CommandTextRegistry.text("region", "protectedAction"), 2350);
    }

    public void deniedEntry(Player player) {
        notice(player, CommandTextRegistry.text("region", "noEntry"), 2600);
    }

    public void remove(UUID uuid) {
        notices.remove(uuid);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Region region = regions.visibleAt(player.getLocation());
            String area = region == null ? "Dzicz" : region.name();
            if (region != null && region.parent() != null)
                area = region.parent() + " › " + region.name();
            if (region != null && regions.inHalo(player.getLocation()))
                area = region.name() + " / ochrona";
            int xp = Math.max(0, Math.min(100, Math.round(player.getExp() * 100)));
            Notice notice = notices.get(player.getUniqueId());
            String extra = notice != null && notice.untilMillis() > now ? notice.message() : "";
            if (notice != null && notice.untilMillis() <= now)
                notices.remove(player.getUniqueId());
            player.sendActionBar(Colors.color(format(area, player.getLevel(), xp, extra)));
        }
    }

    /**
     * Pure function; długość ograniczona, więc alert nigdy nie całkowicie
     * wypiera głównej lokalizacji i postępu z widoku gracza.
     */
    public static String format(String location, int level, int xpPercent, String notice) {
        String safe = location == null ? "Dzicz" : location.replace('\n', ' ').replace('\r', ' ')
                .replace('&', ' ');
        if (safe.length() > 24) safe = safe.substring(0, 21) + "…";
        String base = "&#FFD166⌖ &7Lokalizacja: &#FFE5A2" + safe
                + " &8│ &7Poziom: &#71D7ED" + Math.max(0, level)
                + " &8│ &7XP: &#86E6BC" + Math.max(0, Math.min(100, xpPercent)) + "%";
        if (notice == null || notice.isBlank()) return base;
        String side = notice.replace('\n', ' ').replace('\r', ' ');
        if (side.length() > 105) side = side.substring(0, 102) + "…";
        return base + " &8│ " + side;
    }
}
