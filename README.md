# Tools 1.1.0 — system rang Paper 26.3

Modułowy plugin Java 25 / Paper 26.3 z MySQL (HikariCP), konfiguracją definiowaną w Java i zapisywaną do JSON dopiero przy uruchomieniu.

## Instalacja

1. Uruchom lokalny Laragon MySQL (domyślnie 127.0.0.1:3306, użytkownik root, puste hasło **tylko do testów lokalnych**).
2. Pobierz `Tools.jar` z [GitHub Actions](https://github.com/LokosPL/Tools/actions/workflows/build.yml) i przenieś do `plugins/` serwera Paper 26.3 + Java 25.
3. Uruchom serwer. Plugin automatycznie utworzy bazę `tools` i potrzebne tabele, jeżeli użytkownik ma do tego uprawnienia.
4. Jeśli masz własne ustawienia połączenia MySQL, zostaną zachowane. Zmodyfikuj `plugins/Tools/config.json` tylko gdy to potrzebne.
5. Jako operator sprawdź `/tools status`, `/tools ping`, `/ranga info`.

**Nie używaj root bez hasła na publicznym serwerze.** Serwer MySQL i Paper muszą działać na tym samym komputerze, by adres 127.0.0.1 był poprawny.

## Kolorowanie

We wszystkich wiadomościach Tools, prefixach, suffixach, komunikatach rang oraz nagłówku i stopce TAB-u używaj kodów **`&`**:

- `&a` — zielony, `&b` — błękitny, `&l` — pogrubienie, `&r` — reset.
- `&#FF9900` — kolor HEX RGB (6 cyfr).
- `&x&F&F&9&9&0&0` — równoważny rozszerzony zapis HEX.

Kolory interpretuje centralnie `helpers/Colors.java` z użyciem Adventure Component. Przykład: `&#FFBB00&l[PREMIUM] &f`. Nie używamy kodu sekcji w plikach konfiguracyjnych.

## Rangi / komendy

| Komenda | Znaczenie |
| --- | --- |
| `/ranga stworz premium &#FFBB00[PREMIUM]_ &7` | Tworzy rangę (początkowo nie można jej nadać) |
| `/ranga dodaj premium essentials.fly` | Dopisuje uprawnienie |
| `/ranga dodaj admin *` | Nadaje wszystkie zarejestrowane uprawnienia i status OP na czas działania rangi |
| `/ranga pozycja premium 2` | Ustala priorytet i odblokowuje nadawanie |
| `/ranga wejscie premium &a{nick}_dolaczyl!` | Ustawia komunikat powitalny |
| `/ranga wejscie premium brak` | Wyłącza komunikat |
| `/ranga nadaj LokosPL premium 7d` | Nadaje rangę na siedem dni |
| `/ranga nadaj LokosPL premium na_zawsze` | Nadaje bez końca |
| `/ranga edytuj premium prefix &#55AAFF[VIP]_` | Edytuje prefix |
| `/ranga edytuj premium sufix &7*` | Edytuje suffix |
| `/ranga edytuj premium nazwa vip` | Zmienia nazwę |
| `/ranga info [premium]` | Lista rang lub szczegóły i liczba graczy |
| `/ranga usun premium` | Usuwa rangę, jej uprawnienia i przypisania |

Podkreślenie w parametrach prefix/suffix dla `stworz` służy jako spacja. W poleceniach `edytuj` i `wejscie` możesz podać tekst z odstępami. Nazwy rang: 1–24 znaków (a-z, 0-9, `_`, `-`).

Dostęp do komend: `tools.ranga.admin` (domyślnie OP). Komendy rejestrowane są w **Javie**, bez wpisów w `plugin.yml`. Podpowiedzi w grze obejmują nazwy rang, online nicki, uprawnienia zarejestrowane na serwerze oraz przykładowe czasy. Nadawanie graczom offline działa dla tych, którzy już kiedyś weszli na serwer (UUID z bazy `tools_players`).

**Uwaga bezpieczeństwa:** `*` zmienia uprawnienia oraz status OP gracza; oryginalny stan OP jest zapisywany do MySQL i przywracany przy wyjściu, odebraniu rangi i zamknięciu pluginu. Daj dostęp do `/ranga` wyłącznie zaufanym operatorom. Nadawanie rangi bez ustawionej pozycji jest zablokowane.

## Wyświetlanie

- **TAB:** kolory prefixu + nick + suffix. Gracze są sortowani według pola `position` (mniejsza liczba oznacza wyższą rangę); rangi bez przypisania są niżej. Nagłówek i stopka mają HEX.
- **Czat:** `[PREFIX] NICK » WIADOMOSC` w kolorach Adventure. Zawartość czatu gracza nie jest interpretowana jako kody kolorów.
- **Wejście:** komunikat wyświetla się wyłącznie, gdy w randze jest ustawiony tekst; użyj `{nick}` i `{ranga}`. W przeciwnym wypadku nie ma komunikatu wejścia.
- **Nick nad głową:** system teamów scoreboard dla innych graczy; **własny nick w F5** przez osobny `TextDisplay`, widoczny tylko dla siebie. W pliku `config.json` można wyłączyć `ranks.selfNameTag`.

Obecna implementacja używa dedykowanego scoreboard do teamów/nicków; może kolidować z innymi pluginami, które zmieniają scoreboard, chat lub TAB. Własna etykieta F5 jest rozwiązaniem niestandardowym i wymaga testu wizualnego na kliencie 26.3.

## Struktura

```text
basic/ToolsPlugin.java
commands/ToolsCommand.java, RankCommand.java
config/ToolsConfig.java, JsonConfigManager.java, ToolsConfigMigration.java
database/DatabaseManager.java, PlayerRepository.java, RankRepository.java
manager/PlayerDataManager.java, RankManager.java, RankSnapshot.java, RankVisualManager.java
listeners/PlayerConnectionListener.java, RankListener.java
registry/ConfigRegistry.java, CommandRegistry.java
helpers/Colors.java
...```

MySQL: `tools_players`, `tools_sessions`, `tools_ranks`, `tools_rank_permissions`, `tools_player_ranks`, `tools_rank_op_restore`. Asynchroniczna obsługa SQL; cache rang widoczny dla chatu jest niemutowalny. Przypisania z czasem wygasają, a odświeżenie odbywa się co 10 sekund. Wszystkie opcje konfiguracyjne mają domyślne wartości w klasach Java, a JSON powstaje dopiero podczas uruchomienia pluginu.

## Budowanie i testy

`mvn clean verify` (Java 25) buduje `target/Tools.jar` i uruchamia testy JUnit. [GitHub Actions](https://github.com/LokosPL/Tools/actions/workflows/build.yml) automatycznie publikuje wynik do pobrania.

Sprawdź plugin na testowym serwerze Paper z lokalną bazą przed użyciem na produkcji. Nie ma jeszcze gwarancji bezstratnej pracy bez dostępnego MySQL. Nie używaj komendy serwera `/reload`: restartuj cały proces.
