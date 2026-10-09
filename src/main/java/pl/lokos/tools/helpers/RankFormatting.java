package pl.lokos.tools.helpers;

/** Przejrzyste odstepy pomiedzy ranga, nickiem i sufiksem dla TAB, czatu i nametag. */
public final class RankFormatting {
    private RankFormatting() {}

    public static String prefix(String raw) {
        if (raw == null || Colors.plain(raw).isBlank()) return "";
        String visible = Colors.plain(raw);
        return visible.endsWith(" ") ? raw : raw + " ";
    }

    public static String suffix(String raw) {
        if (raw == null || Colors.plain(raw).isBlank()) return "";
        String visible = Colors.plain(raw);
        return Character.isWhitespace(visible.charAt(0)) ? raw : " " + raw;
    }
}
