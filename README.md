# Tools 1.3.1 — klasyczny TAB i poprawione rangi

Modułowy plugin **Paper 26.3 / Java 25** z HikariCP, MariaDB/MySQL, rangami i konfiguracją JSON generowaną z klas Java.

## Zmiany w 1.3.1

- **TAB** wrócił do standardowej listy prawdziwych graczy, bez sztucznych kolumn i poszerzania. Sortowanie według pozycji rang pozostało. U góry jest krótki tytuł i liczba graczy (czcionka `ᴀᴋᴛᴜᴀʟɴɪᴇ ɢʀᴀᴄᴢʏ ɴᴀ ᴛʀʏʙɪᴇ`), na dole ranga, ping, statystyki i TPS.
- **Jednolite kolory:** czerwony `&c` dla błędów, zielony `&a` dla sukcesów i wyróżnień, szary `&7` dla treści, ciemny `&8` dla separatorów. Nie ma znaków ✓ / ✖ ani prefiksu `[Tools]`. Przykłady stylu:
  - `&c&lBŁĄD &8» &cNie znaleziono rangi.`
  - `&a&lSUKCES &8» &7Nadano rangę &awłaściciel&7.`
  - `&a&lTIP &8» &7Użyj /ranga lista`
- **Prefix i suffix:** plugin sam dokłada brakującą spację między prefixem, szarym nickiem a sufiksem, w TAB-ie, czacie i nazwach nad głową. Nie zmienia przy tym wartości zapisanych w MySQL.
- **Ważność:** `/ranga sprawdz <nick>` pokazuje `na zawsze`, `wygasła` lub pozostały czas oraz datę zakończenia w lokalnej strefie serwera. Potwierdzenie `/ranga nadaj ... *` wyświetla `na zawsze`, zamiast surowej gwiazdki.
- **Tworzenie:** suffix jest opcjonalny: `/ranga stworz <nazwa> <prefix> [sufix]`. Przy nieustawionym suffixie może pozostać tylko prefix i nick.
- **Konfiguracja w Javie:** domyślne wartości `tabHeader` i `tabFooter` są aktualizowane dla starszych, **niezmienionych** presetów przy starcie. Twoje własne zmiany w JSON pozostają zachowane.

## Pierwszy start

1. Uruchom MySQL/MariaDB i serwer Paper **26.3** na Java **25**.
2. Wgraj plik `Tools.jar` do folderu `plugins` i uruchom serwer.
3. Plugin wygeneruje `plugins/Tools/config.json` na podstawie domyślnych ustawień w `ToolsConfig.java` i przygotuje tabele MySQL.
4. Użyj `/tools status`, `/tools ping`, a następnie komend rang.

Domyślny lokalny preset MySQL: `127.0.0.1:3306`, baza `tools`, użytkownik `root`, puste hasło. To wyłącznie konfiguracja do testów z Laragonem na tym samym komputerze. **Nie używaj root bez hasła w produkcji**; utwórz dedykowanego użytkownika.

## Komendy

```text
/ranga stworz właściciel &c&lWłaściciel
/ranga pozycja właściciel 1
/ranga dodaj właściciel essentials.fly
/ranga nadaj LokosPL właściciel *
/ranga sprawdz LokosPL
/ranga lista
/ranga info właściciel
/ranga edytuj właściciel sufix &7★
/ranga wejscie właściciel &a • &7Gracz&8: &a{player} &7dołączył do serwera!
```

Uwaga: wiadomość `wejscie` jest opcjonalna, a pusta nie wyświetla niczego. Dostępne placeholdery: `{player}`, `{nick}`, `{ranga}`. Podkreślenie `_` w prefixie/suffixie przy `stworz` oznacza spację.

- `/ranga stworz` tworzy rangę, której jeszcze nie można nadać; `/ranga pozycja` aktywuje możliwość nadania.
- `/ranga dodaj <ranga> *` ustawia wszystkie uprawnienia (OP); dawnego statusu OP nie traci się po odebraniu tego uprawnienia.
- `/ranga nadaj <nick> <ranga> <1h|7d|30d|*|na_zawsze>` zapisuje przypisanie w MySQL. Dla graczy offline wymagane jest wcześniejsze wejście na serwer.
- `/ranga sprawdz <nick>` pokazuje wynik z MySQL i bieżący stan rangi online.
- `/ranga usun <nazwa>` usuwa rangę i jej przypisania.

Nicki są **szare** na czacie, nad głową i w TAB-ie, a prefixy i suffixy mogą być kolorowane `&a`, `&c`, `&#RRGGBB`. Tekst gracza na czacie również pozostaje szary. Nazwa nad głową w F5 korzysta z oddzielnego `TextDisplay` i wymaga testu wizualnego.

## Struktura i konfiguracja

```text
src/main/java/pl/lokos/tools/
  basic/       ToolsPlugin
  commands/    RankCommand, ToolsCommand
  config/      ToolsConfig, JsonConfigManager, ToolsConfigMigration
  database/    DatabaseManager, RankRepository, PlayerRepository
  helpers/     Colors, Messages, RankFormatting, RankValidity
  listeners/   RankListener, PlayerConnectionListener
  manager/     RankManager, RankVisualManager, RankSnapshot, TabPanel
  registry/    ConfigRegistry, CommandRegistry
  ...
```

Domyślne wartości konfiguracji znajdują się **wyłącznie w Javie**. Pliki JSON generowane są przy starcie; późniejsze edycje administratora zostają zachowane. `plugin.yml` zawiera wyłącznie metadane; komendy są rejestrowane w Javie (Paper BasicCommand). Zmiany konfiguracji wymagają pełnego restartu.

## Kompilacja i testy

```bash
mvn clean verify
```

[GitHub Actions](https://github.com/LokosPL/Tools/actions/workflows/build.yml) uruchamia Maven z testami JUnit oraz integracyjnym testem MariaDB tworzenia, nadawania, odczytu i wygasania rang. Artefakt `Tools.jar` jest do pobrania po udanej kompilacji. Testy nie zastępują weryfikacji GUI i uprawnień na uruchomionym serwerze Paper.
