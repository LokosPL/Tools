package pl.lokos.tools.helpers;

import org.bukkit.command.CommandSender;

/**
 * Jeden styl wiadomosci. Nie wysylamy prefiksu [Tools] do kazdej linijki.
 * Komunikaty bledow sa czerwone, potwierdzenia zielone, pomoc szara.
 */
public final class Messages {
    private Messages() {}

    public static void success(CommandSender to, String value) {
        to.sendMessage(Colors.color("&a✔ &a" + value));
    }

    public static void error(CommandSender to, String value) {
        to.sendMessage(Colors.color("&c✖ &lBłąd! &c" + value));
    }

    public static void info(CommandSender to, String value) {
        to.sendMessage(Colors.color("&8• &7" + value));
    }

    public static void line(CommandSender to, String value) {
        to.sendMessage(Colors.color(value));
    }

    public static void title(CommandSender to, String value) {
        to.sendMessage(Colors.color("&8&m          &r &#55BBFF&l" + value + " &8&m          "));
    }

    public static void usage(CommandSender to, String syntax) {
        to.sendMessage(Colors.color("&8↳ &7Poprawna składnia: &e&n" + syntax));
    }

    public static void hint(CommandSender to, String value) {
        to.sendMessage(Colors.color("&8↳ &7" + value));
    }
}
