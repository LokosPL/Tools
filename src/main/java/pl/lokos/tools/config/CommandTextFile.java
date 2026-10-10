package pl.lokos.tools.config;

import java.util.*;

/**
 * Każda komenda ma własne teksty z domyślnymi wartościami w Javie.
 * JSON przechowuje WYŁĄCZNIE wygląd i teksty; komendy są rejestrowane w Java.
 */
public final class CommandTextFile {
    private String title;
    private List<String> help;
    private Map<String,String> messages;
    private Map<String,String> replacements;
    private CommandTextFile(String title,List<String> help,Map<String,String> messages){
        this.title=title;this.help=new ArrayList<>(help);this.messages=new LinkedHashMap<>(messages);
        this.replacements=new LinkedHashMap<>();
    }
    public String title(){return title;}
    public List<String> help(){return List.copyOf(help);}
    public Map<String,String> messages(){return Map.copyOf(messages);}
    public String message(String key){return messages.get(key);}
    public Map<String,String> replacements(){return Map.copyOf(replacements);}
    public void validate(){
        if(title==null||title.length()>200 || help==null || messages==null || replacements==null)
            throw new IllegalArgumentException("Niekompletna konfiguracja tekstów komendy.");
        if(help.size()>80)throw new IllegalArgumentException("Maksymalnie 80 linii pomocy.");
        for(String line:help)if(line==null||line.length()>500)
            throw new IllegalArgumentException("Linia pomocy nie może być pusta/null ani przekraczać 500 znaków.");
        if(replacements.size()>750) throw new IllegalArgumentException("Maksymalnie 750 zamian tekstu.");
        for(var e:replacements.entrySet()) {
            if(e.getKey()==null||e.getKey().isEmpty()||e.getKey().length()>500
                    ||e.getValue()==null||e.getValue().length()>1000)
                throw new IllegalArgumentException("Błędna zamiana tekstu.");
        }
        if(messages.size()>250)throw new IllegalArgumentException("Maksymalnie 250 szablonów.");
        for(var e:messages.entrySet()){
            if(e.getKey()==null||!e.getKey().matches("[a-zA-Z0-9_.-]{1,64}")
                    ||e.getValue()==null||e.getValue().length()>1200)
                throw new IllegalArgumentException("Niepoprawny szablon: "+e.getKey());
        }
    }
    public static List<String> names(){return List.of("region","ranga","tools","lokalizacje","spawn","whitelist","chat","msg","reply","tp","vanish","helpop","gamemode","fly","broadcast","inventoryopen","speed","przedmiot","antycheat");}

    public static CommandTextFile defaults(String name){
        CommandTextFile file=switch(name){
            case "region" -> new CommandTextFile("ZARZĄDZANIE REGIONAMI",List.of(
                    "&8&m                               ",
                    "&#73D6C1/region stworz &7<nazwa> <promień>",
                    "&#73D6C1/region rozdzka &8• &#73D6C1/region podregion &7<rodzic> <nazwa>",
                    "&#73D6C1/region ochrona &7<region> <promień>",
                    "&#73D6C1/region edytuj &7<nazwa> &8• &#73D6C1/region spawn",
                    "&#73D6C1/region stan &8• &7Sprawdź ochronę",
                    "&#73D6C1/region usun &7<nazwa> &8• &#73D6C1/region lista",
                    "&8&m                               "),
                    Map.of(
                            "notReady","&cRegiony nie zostały poprawnie wczytane. Sprawdź Regions.json i konsolę.",
                            "protectedAction","&#FF727F⚠ &7Obszar chroniony",
                            "noEntry","&cBrak dostępu do tego regionu. &7Wymagana wyższa ranga.",
                            "noRegion","&cNie znaleziono regionu {region}.",
                            "saved","&aZapisano zmianę regionu.",
                            "saveError","&cNie udało się zapisać ustawień regionu."
                    ));
            case "ranga" -> new CommandTextFile("ZARZĄDZANIE RANGAMI",List.of(
                    "&7Tworzenie i konfiguracja",
                    "&#73D6C1/ranga stworz &7<nazwa> <prefix> [sufix]",
                    "&#73D6C1/ranga dodaj &7<ranga> <uprawnienie>",
                    "&#73D6C1/ranga pozycja &7<ranga> <1-9998>",
                    "&#73D6C1/ranga wejscie &7<ranga> <tekst|brak>",
                    "&7Nadawanie i administracja",
                    "&#73D6C1/ranga nadaj &7<nick> <ranga> <czas|*>",
                    "&#73D6C1/ranga edytuj &7<ranga> <prefix|sufix|nazwa> <wartość>",
                    "&#73D6C1/ranga info &7<ranga> &8• &#73D6C1/ranga lista",
                    "&#73D6C1/ranga menu &8— &7Otwiera GUI",
                    "&#73D6C1/ranga usun &7<ranga> &8• &#73D6C1/ranga sprawdz &7<nick>"),
                    Map.of(
                            "notFound","&cNie znaleziono rangi {ranga}.",
                            "alreadyExists","&cRanga {ranga} już istnieje.",
                            "noPermission","&cNie masz uprawnień do zarządzania rangami.",
                            "saved","&aZapisano ustawienia rangi."
                    ));
            case "tools" -> new CommandTextFile("NARZĘDZIA SERWERA",List.of(
                    "&#73D6C1/tools status &8• &7Stan bazy",
                    "&#73D6C1/tools zdrowie &8• &7Pamięć, TPS i wątki",
                    "&#73D6C1/tools diagnostyka &8• &7Czasy modułów",
                    "&#73D6C1/tools przeladuj &8• &7Bezpieczne odświeżenie tekstów",
                    "&#73D6C1/tools ping &8• &7Czas odpowiedzi bazy",
                    "&#73D6C1/tools stats &7<nick> &8• &7Statystyki gracza"),
                    Map.of("noDatabase","&cBaza danych jest wyłączona.",
                            "unknown","&cNieznana podkomenda: {argument}.",
                            "reloadSuccess","&aPrzeładowano teksty i obsługiwane ustawienia."));
            case "lokalizacje" -> new CommandTextFile("LOKALIZACJE",List.of(
                    "&#73D6C1/lokalizacje &8— &7Otwiera wybór teleportacji"),
                    Map.of("notReady","&cLokalizacje jeszcze się wczytują.",
                            "onlyPlayer","&cMenu lokalizacji jest dostępne tylko w grze."));
            case "spawn" -> new CommandTextFile("SPAWN",List.of(
                    "&#73D6C1/spawn &8— &7Teleportacja na główny spawn"),
                    Map.of("notSet","&cSpawn nie został jeszcze ustawiony.",
                            "notReady","&cLokalizacje nie zostały wczytane."));
            case "chat" -> new CommandTextFile("ZARZĄDZANIE CZATEM",List.of(
                    "&#70D6E8/chat status &8• &7Stan czatu i ograniczenia",
                    "&#70D6E8/chat wlacz &8• &#70D6E8/chat wylacz",
                    "&#70D6E8/chat wyczysc &8• &7Wyczyść czat graczom",
                    "&#70D6E8/chat ranga &7<wszyscy|ranga>",
                    "&#70D6E8/chat wycisz &7<nick> <30s|5m|2h|1d|*> [powód]",
                    "&#70D6E8/chat odcisz &7<nick>",
                    "&#70D6E8/chat wyciszeni &7[strona]",
                    "&#70D6E8/chat slow &7<liczba> <sekundy>",
                    "&#70D6E8/chat ogloszenia &7<wlacz|wylacz|lista>",
                    "&#70D6E8/chat ogloszenia interwal &7<sekundy> &8• &7Co ile wysłać ogłoszenie",
                    "&#70D6E8/chat przeladuj &8• &7Odśwież Chat.json"),
                    Map.of("saved","&7Zapisano ustawienia czatu.",
                            "offline","&7Gracz musi być online, aby go wyciszyć.",
                            "notMuted","&7Nie znaleziono wyciszenia tego gracza.",
                            "noRank","&7Nie znaleziono takiej rangi.",
                            "muted","&7Wyciszono &f{nick}&7.",
                            "unmuted","&7Cofnięto wyciszenie &f{nick}&7."));
            case "msg" -> new CommandTextFile("PRYWATNE WIADOMOŚCI",List.of(
                    "&#70D6E8/msg &7<nick> <wiadomość>",
                    "&#70D6E8/msg wlacz &8• &7Włącz własne wiadomości prywatne",
                    "&#70D6E8/msg wylacz &8• &7Wyłącz wysyłanie i odbieranie wiadomości",
                    "&#70D6E8/msg wycisz &7<nick> &8• &7Ignoruj wiadomości tej osoby",
                    "&#70D6E8/msg odcisz &7<nick> &8• &7Przestań ignorować",
                    "&#70D6E8/msg wyciszeni &8• &7Lista ignorowanych",
                    "&#70D6E8/r &7<wiadomość> &8• &7Odpowiedz rozmówcy"),
                    Map.of("self","Nie możesz pisać do siebie.",
                            "offline","Ten gracz nie jest online.",
                            "disabled","Odbiorca ma wyłączone prywatne wiadomości."));
            case "reply" -> new CommandTextFile("ODPOWIEDŹ",List.of(
                    "&#70D6E8/reply &7<wiadomość> &8• &7Odpowiedz ostatniemu rozmówcy",
                    "&#70D6E8/r &7<wiadomość> &8• &7Skrót do /reply"),
                    Map.of("noPartner","Brak dostępnego rozmówcy."));
            case "tp" -> new CommandTextFile("TELEPORTACJA",List.of(
                    "&#70D6E8/tp &7<nick> &8• &7Teleportacja do gracza",
                    "&#70D6E8/tp &7<x> <y> <z> &8• &7Teleportacja na współrzędne",
                    "&#70D6E8/tp &7<nick> <x> <y> <z>",
                    "&#70D6E8/tp * &8• &7Wszyscy do Ciebie",
                    "&#70D6E8/tp * &7<x> <y> <z> &8• &7Wszyscy na współrzędne"),
                    Map.of());
            case "vanish" -> new CommandTextFile("VANISH",List.of(
                    "&#70D6E8/vanish &8• &7Przełącz niewidzialność",
                    "&#70D6E8/vanish &7<wlacz|wylacz>",
                    "&#70D6E8/vanish &7<nick> [wlacz|wylacz] &8• &7Dla administracji"),
                    Map.of());
            case "helpop" -> new CommandTextFile("KONTAKT Z ADMINISTRACJĄ",List.of(
                    "&#70D6E8/helpop &7<treść zgłoszenia>"),Map.of());
            case "gamemode" -> new CommandTextFile("TRYB GRY",List.of(
                    "&#70D6E8/gamemode &7<1|2|3|4> &8• &71 survival, 2 creative, 3 adventure, 4 spectator",
                    "&#70D6E8/gm &7<nick> <1|2|3|4>"),Map.of());
            case "fly" -> new CommandTextFile("LATANIE",List.of(
                    "&#70D6E8/fly &8• &7Przełącz latanie",
                    "&#70D6E8/fly &7<wlacz|wylacz>",
                    "&#70D6E8/fly &7<nick> [wlacz|wylacz]"),Map.of());
            case "broadcast" -> new CommandTextFile("OGŁOSZENIA BOSSBAR",List.of(
                    "&#70D6E8/broadcast &7<30s|5m|2h|1d> <treść>",
                    "&#70D6E8/broadcast wylacz &8• &7Usuń aktywne ogłoszenie",
                    "&#A8A8B7Ogłoszenie pozostaje po restarcie do upływu terminu."),Map.of());
            case "inventoryopen" -> new CommandTextFile("EKWIPUNKI",List.of(
                    "&#70D6E8/inventoryopen eq &7<nick>",
                    "&#70D6E8/inventoryopen enderchest &7<nick>"),Map.of());
            case "speed" -> new CommandTextFile("PRĘDKOŚĆ",List.of(
                    "&#70D6E8/speed &7<1-10> &8• &7Twoja prędkość",
                    "&#70D6E8/speed &7<walk|fly> <1-10> [nick]",
                    "&#70D6E8/speed &7<nick> <1-10>"),Map.of());
            case "antycheat" -> new CommandTextFile("ANTYCHEAT",List.of(
                    "&#70D6E8/antycheat status &8• &7Stan zabezpieczeń",
                    "&#70D6E8/antycheat wlacz &8• &7Włącz kontrolę graczy",
                    "&#70D6E8/antycheat wylacz &8• &7Wyłącz kontrolę graczy",
                    "&#70D6E8/ac powiadomienia wlacz &8• &7Alerty dla Ciebie",
                    "&#70D6E8/ac powiadomienia wylacz &8• &7Ukryj własne alerty",
                    "&#A8A8B7Ochrona wydajności pozostaje aktywna."),Map.of());
            case "przedmiot" -> new CommandTextFile("PRZEDMIOTY EVENTOWE",List.of(
                    "&#70D6E8/przedmiot gui [nick] &#A8A8B7» &7Panel rozdawania",
                    "&#70D6E8/przedmiot lista &#A8A8B7» &7Dostępne definicje",
                    "&#70D6E8/przedmiot info <id> &#A8A8B7» &7Szczegóły przedmiotu",
                    "&#70D6E8/przedmiot daj <nick> <id> [liczba]",
                    "&#A8A8B7Zmieniaj wygląd i efekty w SpecialItems.json."),Map.of());
            case "whitelist" -> new CommandTextFile("WHITELIST",List.of(
                    "&#73D6C1/whitelist &8— &7Otwórz panel",
                    "&#73D6C1/whitelist włącz &7<prace_techniczne|chwilowa_przerwa|nowa_edycja|aktualizacja>",
                    "&#73D6C1/whitelist wyłącz",
                    "&#73D6C1/whitelist dodaj &7<nick>",
                    "&#73D6C1/whitelist usuń &7<nick>",
                    "&#73D6C1/whitelist lista &8— &7Lista główek graczy"),
                    Map.of("noPermission","&cNie możesz zarządzać whitelistą.",
                            "saved","&aZapisano whitelistę.",
                            "saveError","&cNie udało się zapisać whitelisty."));
            default -> throw new IllegalArgumentException("Nieznana komenda: "+name);
        };
        file.replacements.putAll(CommandDefaultFragments.fragments(name));
        file.validate();
        return file;
    }
}