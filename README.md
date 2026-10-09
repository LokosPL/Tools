# Tools 1.2.0 — rozbudowany TAB i system rang Paper 26.3

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

## Wygląd czatu, TAB-u i nazwy nad głową

Nick gracza jest **zawsze szary** (`&7`) — w TAB-ie, na czacie i w nicku nad głową (również przy etykiecie F5). Treść wiadomości czatu jest zawsze szara; kolory wpisane przez gracza nie są interpretowane. Kolorowy prefix rangi można nadal ustawić przez `&` albo `&#RRGGBB`; po nim nick automatycznie wraca do szarości. Prefix bez ustawionego koloru domyślnie będzie szary.

Komunikaty pluginu **nie zawierają powtarzanego prefiksu [Tools]**. Potwierdzenie pojawia się na zielono, błędy na czerwono wraz z podkreślonym błędnym argumentem lub poprawną składnią.

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
| `/ranga nadaj LokosPL premium *` | Skrót oznaczający rangę na zawsze (to samo co `na_zawsze`) |
| `/ranga edytuj premium prefix &#55AAFF[VIP]_` | Edytuje prefix |
| `/ranga edytuj premium sufix &7*` | Edytuje suffix |
| `/ranga edytuj premium nazwa vip` | Zmienia nazwę |
| `/ranga info [premium]` | Lista rang lub szczegóły i liczba graczy |
| `/ranga lista` | Wyświetla wszystkie rangi według pozycji, z informacją o możliwości nadania |
| `/ranga usun premium` | Usuwa rangę, jej uprawnienia i przypisania |

Podkreślenie w parametrach prefix/suffix dla `stworz` służy jako spacja. W poleceniach `edytuj` i `wejscie` możesz podać tekst z odstępami. Nazwy rang: 1–24 znaków — także polskie litery (`Właściciel`, `Zażółć`), cyfry, `_`, `-`. Nazwy są zapisywane małymi literami, by komendy nie rozróżniały wielkości znaków.

Dostęp do komend: `tools.ranga.admin` (domyślnie OP). Komendy rejestrowane są w **Javie**, bez wpisów w `plugin.yml`. Podpowiedzi w grze obejmują nazwy rang, online nicki, uprawnienia zarejestrowane na serwerze oraz przykładowe czasy. Nadawanie graczom offline działa dla tych, którzy już kiedyś weszli na serwer (UUID z bazy `tools_players`).

**Uwaga bezpieczeństwa:** `*` zmienia uprawnienia oraz status OP gracza; oryginalny stan OP jest zapisywany do MySQL i przywracany przy wyjściu, odebraniu rangi i zamknięciu pluginu. Daj dostęp do `/ranga` wyłącznie zaufanym operatorom. Nadawanie rangi bez ustawionej pozycji jest zablokowane.

## Rozbudowana tablista — styl inspirowany podanym zrzutem

Panel TAB używa wbudowanych możliwości Paper, bez wymogu ProtocolLib ani fałszywych kont graczy. Rozbudowany, kolorowy nagłówek i stopka obejmują:

- **Serwer:** liczba graczy online / maksymalna pojemność, bieżący TPS (średnia 1 min).
- **Twój profil:** twoja ranga i bieżący ping w milisekundach.
- **Twoje statystyki:** zabójstwa, śmierci oraz czas gry (z wbudowanych statystyk Minecraft).
- **TOP ZABÓJSTW:** maksymalnie trzy najwyższe wyniki **spośród aktualnie grających** (nie globalny ranking MySQL).
- **Lista graczy:** istniejące rangi i ich sortowanie (pozycja 1 najwyżej), szare nicki, kolorowe prefixy.

Tablista automatycznie aktualizuje widoczne statystyki co **3 sekundy** (60 ticków). Odświeżenie korzysta z danych Bukkit na głównym wątku, bez zapytań do MySQL. Zmiana nicków, grup i sortowanie odbywa się po zmianie rangi / dołączeniu gracza, a nie przy każdym odświeżeniu statystyk.

Ustawienia w `config/ToolsConfig.java` (domyślne wartości w Java, do `plugins/Tools/config.json` są dopisywane przy starcie):
- `ranks.tabStatsEnabled` — wyświetlanie profilu i statystyk.
- `ranks.tabTopKillsEnabled` — ranking zabójstw online.
- `ranks.tabTopLimit` — ile osób w rankingu (od 1 do 5).
- `ranks.tabRefreshTicks` — częstotliwość odświeżania (od 20 do 1200 ticków; 60 to ok. 3 sekundy).
- `ranks.tabHeader`, `ranks.tabFooter` — linie nagłówka i stopki z kodami kolorów `&` / `&#RRGGBB`.

Własne linie obsługują znaczniki: `{online}`, `{max_online}`, `{nick}`, `{ranga}`, `{ping}`, `{zabojstwa}`, `{smierci}`, `{czas_gry}` i `{tps}`.

**Ograniczenie Minecraft:** dokładne boczne kolumny i sztuczne wpisy jak na zdjęciu nie są natywnie obsługiwane przez nagłówek/stopkę Paper. Tutaj statystyki trafiają w czytelne sekcje nad i pod prawdziwą listą graczy. Do odwzorowania wszystkich bocznych bloków 1:1 potrzebny byłby osobny system wirtualnych wpisów i pakietów.

- **Czat:** kolorowy prefix rangi, szary nick i szara wiadomość; kody wpisywane przez zwykłych graczy są traktowane jak tekst.
- **Wejście:** komunikat rangi jest opcjonalny; szablon pozwala użyć `{nick}` i `{ranga}`.
- **Nick nad głową:** scoreboard team dla innych graczy; w trybie F5 własny `TextDisplay`, widoczny tylko dla właściciela.

Uwaga: scoreboard może kolidować z innymi pluginami zarządzającymi TAB-em; F5 i TAB wymagają weryfikacji wizualnej na kliencie Minecraft.

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

MySQL: `tools_players`, `tools_sessions`, `tools_ranks`, `tools_rank_permissions`, `tools_player_ranks`, `tools_rank_op_restore`. Asynchroniczna obsługa SQL; cache rang widoczny dla chatu jest niemutowalny. Przypisania z czasem wygasają, a wygasłe przypisania są sprawdzane co sekundę. Wszystkie opcje konfiguracyjne mają domyślne wartości w klasach Java, a JSON powstaje dopiero podczas uruchomienia pluginu.

## Budowanie i testy

`mvn clean verify` (Java 25) buduje `target/Tools.jar` i uruchamia testy JUnit. [GitHub Actions](https://github.com/LokosPL/Tools/actions/workflows/build.yml) automatycznie publikuje wynik do pobrania.

Sprawdź plugin na testowym serwerze Paper z lokalną bazą przed użyciem na produkcji. Nie ma jeszcze gwarancji bezstratnej pracy bez dostępnego MySQL. Nie używaj komendy serwera `/reload`: restartuj cały proces.
