package pl.lokos.tools.manager;

import net.kyori.adventure.text.Component;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Panel TAB wykonany z natywnego naglowka/stopki Paper. Zachowuje prawdziwa
 * liste graczy, skiny, pingi i sortowanie rang. Nie tworzy falszywych graczy.
 * Kazdy widz otrzymuje wlasne statystyki, bez zapytan do MySQL.
 */
public final class TabPanel {
    private TabPanel() {}

    public record Stats(String nick, String rank, int online, int maxOnline,
                        int ping, int kills, int deaths, long playedTicks, double tps) {}

    public record TopPlayer(String nick, int kills) {}

    public static Component header(ToolsConfig.Ranks settings, Stats stats) {
        String online = Integer.toString(stats.online());
        List<String> lines = new ArrayList<>();
        lines.add("&8&m                                            ");
        lines.add(expand(settings.tabHeader(), stats));
        lines.add(" ");
        if (settings.tabStatsEnabled()) {
            lines.add("&b&lSERWER &8│ &7Online: &a" + stats.online() + "&8/&7" + stats.maxOnline()
                    + " &8│ &7TPS: " + tpsText(stats.tps()));
            lines.add("&b&lTWÓJ PROFIL &8│ &7Ranga: &e" + safeText(stats.rank())
                    + " &8│ &7Ping: &a" + Math.max(0, stats.ping()) + " ms");
        }
        lines.add("&8&m                                            ");
        return toComponent(lines);
    }

    public static Component footer(ToolsConfig.Ranks settings, Stats stats, List<TopPlayer> leaders) {
        List<String> lines = new ArrayList<>();
        lines.add("&8&m                                            ");
        if (settings.tabStatsEnabled()) {
            lines.add("&b&lTWOJE STATYSTYKI");
            lines.add("&7Zabójstwa: &a" + Math.max(0, stats.kills())
                    + " &8│ &7Śmierci: &c" + Math.max(0, stats.deaths())
                    + " &8│ &7Czas gry: &e" + playtime(stats.playedTicks()));
            if (settings.tabTopKillsEnabled()) {
                lines.add(" ");
                lines.add("&b&lTOP ZABÓJSTW &8(na serwerze)");
                if (leaders.isEmpty()) {
                    lines.add("&8Brak graczy online.");
                } else {
                    int count = 0;
                    for (TopPlayer leader : leaders) {
                        if (count >= settings.tabTopLimit()) break;
                        count++;
                        lines.add("&7" + count + ". &f" + safeText(leader.nick())
                                + " &8— &a" + Math.max(0, leader.kills()));
                    }
                }
            }
        }
        lines.add(" ");
        lines.add(expand(settings.tabFooter(), stats));
        lines.add("&8&m                                            ");
        return toComponent(lines);
    }

    private static Component toComponent(List<String> lines) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i != 0) result = result.append(Component.newline());
            result = result.append(Colors.color(lines.get(i)));
        }
        return result;
    }

    public static String playtime(long ticks) {
        long seconds = Math.max(0, ticks) / 20L;
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        if (days > 0) return days + "d " + hours + "g " + minutes + "min";
        if (hours > 0) return hours + "g " + minutes + "min";
        return minutes + "min";
    }

    private static String tpsText(double tps) {
        double safe = Double.isFinite(tps) ? Math.max(0, Math.min(20, tps)) : 0;
        String color = safe >= 19 ? "&a" : safe >= 17 ? "&e" : "&c";
        return color + String.format(Locale.ROOT, "%.1f", safe);
    }

    private static String expand(String template, Stats stats) {
        return template.replace("{online}", Integer.toString(stats.online()))
                .replace("{max_online}", Integer.toString(stats.maxOnline()))
                .replace("{nick}", safeText(stats.nick()))
                .replace("{ranga}", safeText(stats.rank()))
                .replace("{ping}", Integer.toString(Math.max(0, stats.ping())))
                .replace("{zabojstwa}", Integer.toString(Math.max(0, stats.kills())))
                .replace("{smierci}", Integer.toString(Math.max(0, stats.deaths())))
                .replace("{czas_gry}", playtime(stats.playedTicks()))
                .replace("{tps}", String.format(Locale.ROOT, "%.1f",
                        Double.isFinite(stats.tps()) ? Math.max(0, Math.min(20, stats.tps())) : 0));
    }

    /** Gracz nie moze wstrzyknac sekwencji kolorow do naglowka. */
    private static String safeText(String text) {
        return text == null ? "" : text.replace("&", "").replace("\n", "").replace("\r", "");
    }
}
