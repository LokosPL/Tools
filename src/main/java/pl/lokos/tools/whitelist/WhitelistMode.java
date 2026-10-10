package pl.lokos.tools.whitelist;

import java.util.List;
import java.util.Locale;

public enum WhitelistMode {
    PRACE_TECHNICZNE("Prace techniczne"),
    CHWILOWA_PRZERWA("Chwilowa przerwa"),
    NOWA_EDYCJA("Nowa edycja"),
    AKTUALIZACJA("Aktualizacja");

    private final String title;
    WhitelistMode(String title) { this.title = title; }
    public String title() { return title; }
    public static WhitelistMode parse(String name) {
        try { return valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException error) {
            throw new IllegalArgumentException("Wybierz: prace_techniczne, chwilowa_przerwa, nowa_edycja, aktualizacja.");
        }
    }
    public static boolean valid(String name) {
        try { parse(name); return true; } catch (IllegalArgumentException error) { return false; }
    }
    public static List<String> names() {
        return java.util.Arrays.stream(values()).map(Enum::name).toList();
    }
}
