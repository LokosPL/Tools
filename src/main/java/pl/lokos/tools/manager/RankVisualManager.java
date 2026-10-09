package pl.lokos.tools.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.utils.ThreadChecks;

import java.util.*;

/**
 * TAB + tablist sortowanie + nazwy nad glowami za pomoca scoreboard team.
 * Wlasny napis w F5 jest osobnym, widocznym tylko dla wlasciciela TextDisplay.
 */
public final class RankVisualManager {
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final ToolsConfig.Ranks config;
    private final Scoreboard scoreboard;
    private final Map<UUID, TextDisplay> selfTags = new HashMap<>();
    private boolean closed;

    public RankVisualManager(JavaPlugin plugin, RankManager ranks, ToolsConfig.Ranks config) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.config = config;
        this.scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
    }

    public void refresh() {
        ThreadChecks.requirePrimaryThread();
        if (closed) return;
        // Scoreboard team grupuje graczy po priorytecie; mniejszy numer = wyzej.
        for (Team team : new ArrayList<>(scoreboard.getTeams())) team.unregister();
        RankSnapshot snapshot = ranks.snapshot();
        Map<String, Team> teams = new HashMap<>();
        List<RankSnapshot.Rank> sorted = new ArrayList<>(snapshot.ranks().values());
        sorted.sort(Comparator.comparingInt(r -> r.position() == null ? 9999 : r.position()));
        int index = 0;
        for (RankSnapshot.Rank rank : sorted) {
            Team team = scoreboard.registerNewTeam(String.format(Locale.ROOT, "r%04d", index++));
            team.prefix(Colors.color(rank.prefix()));
            team.suffix(Colors.color(rank.suffix()));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            teams.put(rank.name(), team);
        }
        Team common = scoreboard.registerNewTeam("r9999");
        common.prefix(Colors.color(config.defaultPrefix()));
        common.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparingInt((Player p) -> priority(snapshot.forPlayer(p.getUniqueId())))
                .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int order = 0;
        for (Player player : players) {
            RankSnapshot.Rank rank = snapshot.forPlayer(player.getUniqueId());
            teams.getOrDefault(rank == null ? "" : rank.name(), common).addEntry(player.getName());
            if (config.enabled()) {
                player.setScoreboard(scoreboard);
                player.setPlayerListOrder(order++);
                player.playerListName(label(player, rank));
                player.sendPlayerListHeaderAndFooter(
                        Colors.color(config.tabHeader().replace("{online}", Integer.toString(players.size()))),
                        Colors.color(config.tabFooter().replace("{online}", Integer.toString(players.size()))));
            }
            updateSelfTag(player, rank);
        }
    }

    private static int priority(RankSnapshot.Rank rank) {
        return rank == null || rank.position() == null ? 9999 : Math.max(0, rank.position());
    }

    private Component label(Player player, RankSnapshot.Rank rank) {
        String prefix = rank == null ? config.defaultPrefix() : rank.prefix();
        String suffix = rank == null ? "" : rank.suffix();
        return Colors.color(prefix).append(Component.text(player.getName(), NamedTextColor.WHITE))
                .append(Colors.color(suffix));
    }

    private void updateSelfTag(Player player, RankSnapshot.Rank rank) {
        UUID id = player.getUniqueId();
        if (!config.selfNameTag()) {
            TextDisplay old = selfTags.remove(id);
            if (old != null) old.remove();
            return;
        }
        TextDisplay tag = selfTags.get(id);
        if (tag == null || !tag.isValid() || !tag.getWorld().equals(player.getWorld())) {
            if (tag != null) tag.remove();
            tag = player.getWorld().spawn(tagLocation(player), TextDisplay.class, display -> {
                display.setVisibleByDefault(false);
                display.setBillboard(Display.Billboard.CENTER);
                display.setSeeThrough(true);
                display.setShadowed(false);
                display.setPersistent(false);
                display.setDefaultBackground(false);
            });
            selfTags.put(id, tag);
            player.showEntity(plugin, tag);
        }
        tag.text(label(player, rank));
    }

    private Location tagLocation(Player player) {
        return player.getLocation().add(0, player.isSneaking() ? 2.0 : 2.5, 0);
    }

    /** Wywolywane co 4 ticki tylko dla graczy online. */
    public void tick() {
        ThreadChecks.requirePrimaryThread();
        if (closed || !config.selfNameTag()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            TextDisplay tag = selfTags.get(player.getUniqueId());
            if (tag == null || !tag.isValid() || !tag.getWorld().equals(player.getWorld())) {
                updateSelfTag(player, ranks.snapshot().forPlayer(player.getUniqueId()));
            } else {
                Location desired = tagLocation(player);
                if (tag.getLocation().distanceSquared(desired) > 0.001) tag.teleport(desired);
            }
        }
    }

    public void leave(Player player) {
        TextDisplay tag = selfTags.remove(player.getUniqueId());
        if (tag != null) tag.remove();
    }

    public void stop() {
        if (closed) return;
        closed = true;
        for (TextDisplay tag : selfTags.values()) tag.remove();
        selfTags.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard());
            player.playerListName(Component.text(player.getName()));
        }
    }
}
