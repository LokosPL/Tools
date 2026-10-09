package pl.lokos.tools.helpers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Czytelny czas trwania rangi; NULL w bazie oznacza na zawsze. */
public final class RankValidity {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm z", Locale.forLanguageTag("pl-PL"));

    private RankValidity() {}

    public static String durationLabel(String input) {
        String value = input == null ? "" : input.strip().toLowerCase(Locale.ROOT);
        if (value.equals("*") || value.equals("na_zawsze") || value.equals("na zawsze")
                || value.equals("zawsze")) return "na zawsze";
        if (!value.matches("[1-9][0-9]*[mhdw]")) return input;
        long n = Long.parseLong(value.substring(0, value.length() - 1));
        String unit = switch (value.charAt(value.length() - 1)) {
            case 'm' -> plural(n, "minuta", "minuty", "minut");
            case 'h' -> plural(n, "godzina", "godziny", "godzin");
            case 'd' -> n == 1 ? "dzień" : "dni";
            case 'w' -> plural(n, "tydzień", "tygodnie", "tygodni");
            default -> "";
        };
        return n + " " + unit;
    }

    private static String plural(long value, String one, String few, String many) {
        if (value == 1) return one;
        long last = value % 10;
        long lastTwo = value % 100;
        return last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14) ? few : many;
    }

    public static String remaining(Long expiresAt, long now) {
        if (expiresAt == null) return "na zawsze";
        long delta = Math.max(0, expiresAt - now);
        if (delta == 0) return "wygasła";
        long minutes = (delta + 59_999L) / 60_000L;
        long days = minutes / 1440;
        long hours = (minutes % 1440) / 60;
        long mins = minutes % 60;
        if (days > 0) return days + (days == 1 ? " dzień, " : " dni, ") + hours + " godz.";
        if (hours > 0) return hours + " godz., " + mins + " min";
        return mins + " min";
    }

    public static String expirationDate(Long expiresAt, ZoneId zone) {
        return expiresAt == null ? "bez daty zakończenia"
                : DATE.format(Instant.ofEpochMilli(expiresAt).atZone(zone));
    }
}
