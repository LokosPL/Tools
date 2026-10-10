package pl.lokos.tools.whitelist;

import java.util.*;

/** Jedyne źródło domyślnych wartości — Java. Plik Whitelist.json powstaje przy starcie. */
public final class WhitelistConfig {
    private boolean enabled = false;
    private String mode = "PRACE_TECHNICZNE";
    private Set<String> players = new LinkedHashSet<>();
    private String defaultKick = "&cSerwer jest aktualnie wyłączony dla graczy.\n&7Przepraszamy za niedogodności.";
    private Map<String,String> messages = new LinkedHashMap<>(Map.of(
        "PRACE_TECHNICZNE", "&#FF6B6B&lPRACE TECHNICZNE\n&7Serwer jest aktualnie niedostępny.\n&7Przepraszamy za niedogodności.",
        "CHWILOWA_PRZERWA", "&#FFB65C&lCHWILOWA PRZERWA\n&7Wrócimy za krótką chwilę.",
        "NOWA_EDYCJA", "&#65C7FF&lNOWA EDYCJA\n&7Przygotowujemy nową przygodę. Do zobaczenia!",
        "AKTUALIZACJA", "&#8DABFF&lAKTUALIZACJA SERWERA\n&7Instalujemy nowe funkcje. Wróć później."
    ));
    private String shutdownMessage = "&#FF7777&lSERWER WYŁĄCZONY\n&7Przepraszamy, serwer został wyłączony.\n&7Wrócimy za krótką chwilę…";
    private String reloadMessage = "&#FFB55D&lPRZEŁADOWANIE SERWERA\n&7Aktualnie trwa przeładowanie serwera.\n&7Wrócimy za krótką chwilę.";

    public boolean enabled() { return enabled; }
    public String mode() { return mode; }
    public Set<String> players() { return Set.copyOf(players); }
    public String defaultKick() { return defaultKick; }
    public String shutdownMessage() { return shutdownMessage; }
    public String reloadMessage() { return reloadMessage; }
    public String kickText() { return messages.getOrDefault(mode, defaultKick); }
    public Map<String,String> messages() { return Map.copyOf(messages); }

    public void validate() {
        if (mode == null || !WhitelistMode.valid(mode)) throw new IllegalArgumentException("Niepoprawny tryb whitelisty.");
        if (players == null || messages == null || defaultKick == null || shutdownMessage == null || reloadMessage == null)
            throw new IllegalArgumentException("Brakuje ustawień whitelisty.");
        if (players.size() > 10000) throw new IllegalArgumentException("Zbyt długa lista whitelisty.");
        for (String name : players)
            if (!name.equals(normalize(name))) throw new IllegalArgumentException("Błędny nick: " + name);
        for (String key : WhitelistMode.names())
            if (!messages.containsKey(key) || messages.get(key) == null || messages.get(key).length() > 2048)
                throw new IllegalArgumentException("Brak komunikatu whitelisty: " + key);
        if (shutdownMessage.length() > 2048 || reloadMessage.length() > 2048 || defaultKick.length() > 2048)
            throw new IllegalArgumentException("Za długi komunikat whitelisty.");
    }
    public static String normalize(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{3,16}"))
            throw new IllegalArgumentException("Nick powinien mieć 3–16 znaków: litery, cyfry lub '_'.");
        return name.toLowerCase(Locale.ROOT);
    }
    public WhitelistConfig with(boolean newEnabled, String newMode, Set<String> names) {
        WhitelistConfig copy = new WhitelistConfig();
        copy.enabled = newEnabled;
        copy.mode = newMode;
        copy.players = new LinkedHashSet<>(names);
        copy.messages = new LinkedHashMap<>(messages);
        copy.defaultKick = defaultKick;
        copy.shutdownMessage = shutdownMessage;
        copy.reloadMessage = reloadMessage;
        copy.validate();
        return copy;
    }
}
