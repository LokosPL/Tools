package pl.lokos.tools.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Kolory: &a, &l, &r i HEX w formacie &#RRGGBB. */
public final class Colors {
    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private Colors() {}

    public static Component color(String raw) {
        return SERIALIZER.deserialize(normalize(raw == null ? "" : raw));
    }

    public static String normalize(String raw) {
        Matcher matcher = HEX.matcher(raw == null ? "" : raw);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String digits = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : digits.toCharArray()) replacement.append('&').append(c);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static void send(CommandSender receiver, String text) {
        receiver.sendMessage(color(text));
    }

    public static String plain(String raw) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(color(raw));
    }
}
