package pl.lokos.tools.helpers;

import org.bukkit.command.CommandSender;

/** Jednolita stylistyka: &c błędy, &a potwierdzenia, &7 treść, &8 separatory. */
public final class Messages {
    private Messages() {}
    private static volatile String prefix="";
    public static void configure(String value){prefix=value==null?"":value;}
    private static String format(String text){return prefix+text;}

    public static void success(CommandSender to, String value) {
        to.sendMessage(Colors.color(format("&a&lSUKCES &8-> &7" + value)));
    }

    public static void error(CommandSender to, String value) {
        to.sendMessage(Colors.color(format("&c&lBŁĄD &8-> &c" + value)));
    }

    public static void info(CommandSender to, String value) {
        to.sendMessage(Colors.color(format("&a • &7" + value)));
    }

    public static void line(CommandSender to, String value) {
        to.sendMessage(Colors.color(format(value)));
    }

    public static void title(CommandSender to, String value) {
        to.sendMessage(Colors.color(format("&a&l" + value + " &8----------------")));
    }

    public static void usage(CommandSender to, String syntax) {
        to.sendMessage(Colors.color(format("&8Składnia: &7" + syntax)));
    }

    public static void hint(CommandSender to, String value) {
        to.sendMessage(Colors.color(format("&a&lTIP &8-> &7" + value)));
    }
}
