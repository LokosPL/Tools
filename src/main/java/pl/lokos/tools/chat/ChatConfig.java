package pl.lokos.tools.chat;

import java.util.List;

/** Wartości domyślne generowanego Chat.json; ustawienia są oddzielne od stanu moderacji. */
public final class ChatConfig {
    private int slowWindowSeconds = 3;
    private int slowMaxMessages = 2;
    private boolean announcementsEnabled = true;
    private int announcementIntervalSeconds = 300;
    private String announcementPrefix = "&#FFD166✦ &7";
    private List<String> announcements = List.of(
            "Testowa automatyczna wiadomość 1",
            "Testowa automatyczna wiadomość 2");
    private String chatDisabledMessage = "Czat jest obecnie wyłączony.";
    private String rankRestrictedMessage = "Twoja ranga nie może teraz pisać na czacie.";
    private String mutedMessage = "Masz wyciszony czat. &8» &7{reason}";
    private String slowMessage = "Piszesz zbyt szybko. &7Limit: &#FFD166{count} &7wiadomości na &#FFD166{seconds} s.";

    public int slowWindowSeconds(){return slowWindowSeconds;}
    public int slowMaxMessages(){return slowMaxMessages;}
    public boolean announcementsEnabled(){return announcementsEnabled;}
    public int announcementIntervalSeconds(){return announcementIntervalSeconds;}
    public String announcementPrefix(){return announcementPrefix;}
    public List<String> announcements(){return List.copyOf(announcements);}
    public String chatDisabledMessage(){return chatDisabledMessage;}
    public String rankRestrictedMessage(){return rankRestrictedMessage;}
    public String mutedMessage(){return mutedMessage;}
    public String slowMessage(){return slowMessage;}

    public void validate() {
        if (slowWindowSeconds < 1 || slowWindowSeconds > 60 || slowMaxMessages < 1 || slowMaxMessages > 20)
            throw new IllegalArgumentException("Chat.json: limit wiadomości musi być w zakresie 1-20 / 1-60 s.");
        if (announcementIntervalSeconds < 20 || announcementIntervalSeconds > 86400)
            throw new IllegalArgumentException("Chat.json: odstęp ogłoszeń musi wynosić 20-86400 s.");
        if (announcements == null || announcements.size() > 50)
            throw new IllegalArgumentException("Chat.json: za dużo ogłoszeń.");
        for (String line : announcements)
            if (line == null || line.isBlank() || line.length() > 512)
                throw new IllegalArgumentException("Chat.json: błędna automatyczna wiadomość.");
        if (announcementPrefix == null || announcementPrefix.length() > 128
                || chatDisabledMessage == null || chatDisabledMessage.length() > 350
                || rankRestrictedMessage == null || rankRestrictedMessage.length() > 350
                || mutedMessage == null || mutedMessage.length() > 350
                || slowMessage == null || slowMessage.length() > 350)
            throw new IllegalArgumentException("Chat.json: nieprawidłowy tekst.");
    }
}
