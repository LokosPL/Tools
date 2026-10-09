package pl.lokos.tools.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.RankFormatting;
import pl.lokos.tools.utils.ThreadChecks;

import java.util.*;

/**
 * TAB + tablist sortowanie + nazwy nad glowami za pomoca scoreboard team.
 * Wlasny napis w F5 jest osobnym, widocznym tylko dla wlasciciela TextDisplay.
 */
public final class RankVisualManager {
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private volatile ToolsConfig.Ranks config;
    private final Scoreboard scoreboard;
    private final Map<UUID, TextDisplay> selfTags = new HashMap<>();
    private boolean closed;

    public RankVisualManager(JavaPlugin plugin, RankManager ranks, ToolsConfig.Ranks config) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.config = config;
        this.scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
    }

    public void applySettings(ToolsConfig.Ranks next) {
        ThreadChecks.requirePrimaryThread();
        Objects.requireNonNull(next).validate();
        config = next;
        refresh();
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
            team.prefix(Colors.color("&7" + RankFormatting.prefix(rank.prefix())));
            team.color(NamedTextColor.GRAY);
            team.suffix(Colors.color("&7" + RankFormatting.suffix(rank.suffix())));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            teams.put(rank.name(), team);
        }
        Team common = scoreboard.registerNewTeam("r9999");
        common.prefix(Colors.color(RankFormatting.prefix(config.defaultPrefix())));
        common.color(NamedTextColor.GRAY);
        common.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparingInt((Player p) -> priority(snapshot.forPlayer(p.getUniqueId())))
                .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int order = 1;
        for (Player player : players) {
            RankSnapshot.Rank rank = snapshot.forPlayer(player.getUniqueId());
            teams.getOrDefault(rank == null ? "" : rank.name(), common).addEntry(player.getName());
            if (config.enabled()) {
                player.setScoreboard(scoreboard);
                player.setPlayerListOrder(order++);
                player.playerListName(label(player, rank));

            }
            updateSelfTag(player, rank);
        }
        updateTab();
    }

    /**
     * Odswiezamy lekkie statystyki graczy okresowo z API Bukkit, bez
     * zapytan do MySQL i bez przebudowy teamow przy kazdym odswiezeniu.
     */
    public void updateTab() {
        ThreadChecks.requirePrimaryThread();
        if (closed || !config.enabled()) return;
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        double[] tpsValues = Bukkit.getTPS();
        double tps = tpsValues.length == 0 ? 20.0 : tpsValues[0];
        RankSnapshot ranksSnapshot = ranks.snapshot();
        for (Player viewer : online) {
            RankSnapshot.Rank rank = ranksSnapshot.forPlayer(viewer.getUniqueId());
            TabPanel.Stats stats = new TabPanel.Stats(
                    viewer.getName(), rank == null ? "Gracz" : rank.name(),
                    online.size(), Bukkit.getMaxPlayers(), viewer.getPing(),
                    viewer.getStatistic(Statistic.PLAYER_KILLS),
                    viewer.getStatistic(Statistic.DEATHS),
                    viewer.getStatistic(Statistic.PLAY_ONE_MINUTE), tps);
            viewer.sendPlayerListHeaderAndFooter(
                    TabPanel.header(config, stats),
                    TabPanel.footer(config, stats));
        }
    }

    private static int priority(RankSnapshot.Rank rank) {
        return rank == null || rank.position() == null ? 9999 : Math.max(0, rank.position());
    }

    private Component label(Player player, RankSnapshot.Rank rank) {
        String prefix = rank == null ? config.defaultPrefix() : rank.prefix();
        String suffix = rank == null ? "" : rank.suffix();
        return Colors.color("&7" + RankFormatting.prefix(prefix))
                .append(Component.text(player.getName(), NamedTextColor.GRAY))
                .append(Colors.color("&7" + RankFormatting.suffix(suffix)));
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
                display.setTeleportDuration(2);
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
                if (tag.getLocation().distanceSquared(desired) > 0.004) tag.teleport(desired);
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
            player.playerListName(Component.text(player.getName(), NamedTextColor.GRAY));
        }
    }
}
