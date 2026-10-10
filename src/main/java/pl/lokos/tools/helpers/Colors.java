package pl.lokos.tools.helpers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import pl.lokos.tools.config.VisualsConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Kolory: &a, &l, &r i HEX w formacie &#RRGGBB. */
public final class Colors {
    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern LEGACY_PALETTE = Pattern.compile("(?i)&([6789abce])");
    private static volatile VisualsConfig palette;

    /** Aktywowane po załadowaniu Visuals.json; nie ingeruje w jawne kody HEX. */
    public static void configurePalette(VisualsConfig visual){
        if(visual!=null)visual.validate();
        palette=visual;
    }
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private Colors() {}

    public static Component color(String raw) {
        return SERIALIZER.deserialize(normalize(raw == null ? "" : raw));
    }

    public static String normalize(String raw) {
        String source=raw==null?"":raw;
        VisualsConfig active=palette;
        if(active!=null){
            Matcher legacy=LEGACY_PALETTE.matcher(source);
            StringBuilder replaced=new StringBuilder();
            while(legacy.find()){
                String replacement=switch(legacy.group(1).toLowerCase(java.util.Locale.ROOT)){
                    case "6","e"->active.gold();
                    case "a"->active.success();
                    case "9","b"->active.cyan();
                    case "c"->active.red();
                    case "7"->active.gray();
                    case "8"->active.muted();
                    default->null;
                };
                legacy.appendReplacement(replaced,
                        Matcher.quoteReplacement(replacement==null?legacy.group():"&"+replacement));
            }
            legacy.appendTail(replaced);
            source=replaced.toString();
        }
        Matcher matcher = HEX.matcher(source);
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
