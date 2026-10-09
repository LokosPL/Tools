package pl.lokos.tools.helpers;

import org.bukkit.command.CommandSender;

/** Jednolita stylistyka: &c błędy, &a potwierdzenia, &7 treść, &8 separatory. */
public final class Messages {
    private Messages() {}

    public static void success(CommandSender to, String value) {
        to.sendMessage(Colors.color("&a&lSUKCES &8-> &7" + value));
    }

    public static void error(CommandSender to, String value) {
        to.sendMessage(Colors.color("&c&lBŁĄD &8-> &c" + value));
    }

    public static void info(CommandSender to, String value) {
        to.sendMessage(Colors.color("&a • &7" + value));
    }

    public static void line(CommandSender to, String value) {
        to.sendMessage(Colors.color(value));
    }

    public static void title(CommandSender to, String value) {
        to.sendMessage(Colors.color("&a&l" + value + " &8----------------"));
    }

    public static void usage(CommandSender to, String syntax) {
        to.sendMessage(Colors.color("&8Składnia: &7" + syntax));
    }

    public static void hint(CommandSender to, String value) {
        to.sendMessage(Colors.color("&a&lTIP &8-> &7" + value));
    }
}
