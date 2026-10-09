package pl.lokos.tools.manager;

import net.kyori.adventure.text.Component;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Szeroki, trzykolumnowy panel oparty o natywny naglowek/stopke Paper.
 * Nie wstrzykujemy sztucznych profili graczy i nie zastepujemy prawdziwej listy.
 * Stala szerokosc wierszy utrzymuje duzy TAB nawet przy jednym graczu.
 */
public final class TabPanel {
    private static final int LEFT = 31;
    private static final int CENTER = 31;
    private static final int RIGHT = 31;
    private static final String BORDER = "&8" + "━".repeat(111);
    private static final String DIVIDER = "&8" + "─".repeat(111);

    private TabPanel() {}

    public record Stats(String nick, String rank, int online, int maxOnline,
                        int ping, int kills, int deaths, long playedTicks, double tps) {}

    public record TopPlayer(String nick, int kills) {}

    public static Component header(ToolsConfig.Ranks settings, Stats stats) {
        List<String> lines = new ArrayList<>();
        lines.add(BORDER);
        lines.add(row("&#55CDFF&l         T O O L S",
                "&#EAC56F&l    S E R W E R",
                "&a&l        M I N E C R A F T"));
        lines.add(row("&7" + expand(settings.tabHeader(), stats),
                "&7Witaj, &f" + safeText(stats.nick()),
                "&7Miłej gry na serwerze!"));
        lines.add(DIVIDER);
        if (settings.tabStatsEnabled()) {
            lines.add(row("&#57D8FF&l       INFORMACJE",
                    "&#FFD36B&l       TWOJE KONTO",
                    "&#89EC9D&l       STATYSTYKI"));
            lines.add(row("&7Gracze online: &a" + stats.online() + "&8/&7" + stats.maxOnline(),
                    "&7Ranga: &e" + safeText(stats.rank()),
                    "&7Zabójstwa: &a" + Math.max(0, stats.kills())));
            lines.add(row("&7TPS serwera: " + tpsText(stats.tps()),
                    "&7Ping: &a" + Math.max(0, stats.ping()) + " ms",
                    "&7Śmierci: &c" + Math.max(0, stats.deaths())));
            lines.add(row("&7Tryb: &fSurvival",
                    "&7Nick: &f" + safeText(stats.nick()),
                    "&7Czas gry: &e" + playtime(stats.playedTicks())));
            lines.add(DIVIDER);
        }
        lines.add(row("&b&l GRACZE ONLINE", "&8↓ &7Posortowani według rang &8↓", "&b&l LISTA GRACZY"));
        lines.add(" ");
        return component(lines);
    }

    public static Component footer(ToolsConfig.Ranks settings, Stats stats, List<TopPlayer> leaders) {
        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(DIVIDER);
        if (settings.tabStatsEnabled()) {
            lines.add(row("&b&l       TWOJE WYNIKI",
                    "&6&l       TOP ZABÓJSTW",
                    "&d&l       INFORMACJE"));
            List<String> ranking = new ArrayList<>();
            if (settings.tabTopKillsEnabled()) {
                int count = 0;
                for (TopPlayer leader : leaders) {
                    if (count >= settings.tabTopLimit()) break;
                    ranking.add("&7" + (++count) + ". &f" + safeText(leader.nick())
                            + " &8— &a" + Math.max(0, leader.kills()));
                }
            }
            if (ranking.isEmpty()) ranking.add("&8Brak danych.");
            int rows = Math.max(5, ranking.size());
            for (int i = 0; i < rows; i++) {
                String left = switch (i) {
                    case 0 -> "&7Zabójstwa: &a" + Math.max(0, stats.kills());
                    case 1 -> "&7Śmierci: &c" + Math.max(0, stats.deaths());
                    case 2 -> "&7Czas gry: &e" + playtime(stats.playedTicks());
                    case 3 -> "&7Twój ping: &a" + Math.max(0, stats.ping()) + " ms";
                    case 4 -> "&7Twoja ranga: &e" + safeText(stats.rank());
                    default -> " ";
                };
                String middle = i < ranking.size() ? ranking.get(i) : " ";
                String right = switch (i) {
                    case 0 -> "&7Gracze: &a" + stats.online();
                    case 1 -> "&7Limit: &e" + stats.maxOnline();
                    case 2 -> "&7TPS: " + tpsText(stats.tps());
                    case 3 -> "&7Wpisz &e/ranga lista";
                    case 4 -> "&7Życzymy udanej gry!";
                    default -> " ";
                };
                lines.add(row(left, middle, right));
            }
            lines.add(DIVIDER);
        }
        lines.add(row("&7ZAPRASZAJ ZNAJOMYCH", expand(settings.tabFooter(), stats), "&7DZIĘKUJEMY ZA GRĘ"));
        lines.add(BORDER);
        return component(lines);
    }

    private static String row(String left, String middle, String right) {
        return "&8┃ " + pad(left, LEFT) + "&8 ┃ " + pad(middle, CENTER)
                + "&8 ┃ " + pad(right, RIGHT) + "&8 ┃";
    }

    private static String pad(String content, int columnWidth) {
        String safe = content == null ? "" : content;
        int visible = Colors.plain(safe).codePointCount(0, Colors.plain(safe).length());
        return safe + " ".repeat(Math.max(1, columnWidth - visible));
    }

    private static Component component(List<String> lines) {
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

    private static String safeText(String text) {
        return text == null ? "" : text.replace("&", "").replace("\n", "").replace("\r", "");
    }
}
