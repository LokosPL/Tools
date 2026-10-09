## Poprawka 1.6.2 — prywatne uprawnienia Tools i widoczność komend

- GUI \`/ranga\` pokazuje wyłącznie zdefiniowane funkcje Tools z czytelnymi polskimi nazwami i opisami; nie zbiera już uprawnień Bukkit, vanilla ani innych pluginów. Ręcznie wpisane uprawnienia innych pluginów w Ranks.json pozostają nienaruszone, ale nie są wyświetlane w panelu ani automatycznie podpowiadane w \`/ranga dodaj\`.
- \`tools.lokalizacje\` **nie jest już domyślnym uprawnieniem wszystkich graczy**. Aby zwykły gracz otworzył \`/lokalizacje\`, nadaj tę funkcję jego randze w GUI. Dla \`gracz\` kliknij \`/ranga\` → \`gracz\` → \`Uprawnienia\` → \`Menu lokalizacji\`.
- \`/tools\`, \`/ranga\`, \`/region\`, \`/lokalizacje\` i aliasy podpowiadają się wyłącznie osobom mającym dostęp. Ukryte są też przy ręcznym wpisaniu: \`BŁĄD -> Nieznana komenda.\`
- Po zmianie uprawnień klient natychmiast otrzymuje odświeżoną listę komend (Paper/Brigadier).
- Domyślny prefix wiadomości jest pusty. Istniejący fabryczny \`&8[&a{server}&8] \` zostanie automatycznie usunięty z Commands.json przy starcie; własne, zmienione ręcznie prefixy pozostaną bez zmian.
- Naprawiono niezgodność: \`/ranga stworz\`, \`/ranga dodaj\`, \`/ranga pozycja\`, \`/ranga wejscie\`, \`/ranga edytuj\`, \`/ranga usun\` zapisują definicje w **Ranks.json**, nie w starej tabeli SQL. Nadania graczom pozostają w MySQL.

**Ważne przy aktualizacji:** dla nowych serwerów zwykły gracz bez uprawnienia \`tools.lokalizacje\` NIE otworzy GUI lokalizacji. Jeśli wcześniej każdy mógł używać tej komendy, nadaj tę funkcję randze \`gracz\`. OP oraz ranga z \`*\` zachowują pełny dostęp. System uprawnień innych pluginów nadal działa niezależnie; Tools ukrywa tylko własne niedostępne komendy.

---

## Poprawka 1.6.1 — GUI regionów i gęste granice

- **Naprawiono NullPointerException** w `RegionMenuListener.click` przy zmianie ustawienia na „dziedziczenie”. Flagi przełączają się kolejno: **dziedziczenie → dozwolone → zabronione → dziedziczenie** bez próby rozpakowania `null` do `boolean`.
- **Gęstszy podgląd granic**: cząsteczki koloru miętowo-zielonego co **1,5 bloku**, w **trzech warstwach wysokości**. Pokazywane tylko graczowi uruchamiającemu podgląd, przez **10 sekund**, w odległości **36 bloków**. Punktów jest maksymalnie **140** na cykl, więc koszt nie zależy od rozmiaru regionu.
- Jeśli granica leży dalej niż 36 bloków od gracza, pojawia się wskazówka, aby podejść bliżej — zamiast pozornie pustego podglądu.
- Dodano testy przełączania trzech stanów i wyliczania gęstych punktów na granicy.

Po podmianie JAR-a wykonaj pełny restart serwera. Zmiana nie wymaga kasowania żadnych plików JSON ani danych MySQL.

---

# Tools 1.6.0 — panele GUI, granularne flagi regionów, płynne etykiety

## Najważniejsze zmiany

- **\`/region edytuj <nazwa>\`** otwiera panel. Dostępne są podstrony **Zabezpieczenia** (wiele stron flag) i **Dostęp rang** (wybór bez komend); kliknięcie zmienia stan i odświeża ekran po atomowym zapisie Regions.json. W GUI wyświetlany jest zarówno stan lokalny, jak i efekt dziedziczenia. Domyślnie nowy region jest chroniony.
- **Nowe flagi**: skrzynie i beczki (\`skrzynie\`), stoły rzemieślnicze (\`crafting\`), piece, kowadła, zaklinanie, alchemia, drzwi, przyciski, dźwignie, płytki naciskowe, leje, ramki, stojaki, pojazdy, portale, perły, podnoszenie i wyrzucanie przedmiotów. Stara flaga \`interakcje\` pozostaje ustawieniem domyślnym dla szczegółowych czynności, o ile nie ustawiono ich oddzielnie.
- **\`/ranga\` oraz \`/ranga menu\`** wyświetlają GUI. Wybierz rangę, potem uprawnienia. Lista pochodzi z serwera Bukkit i uwzględnia Tools oraz uprawnienia już użyte w rangach. Przełączanie odbywa się atomowo w Ranks.json. Można nadal dopisać niestandardowe uprawnienie komendą \`/ranga dodaj\`.
- Ranga **\`gracz\`** jest obecna na liście i nie można jej usunąć lub zmienić jej nazwy. Jej uprawnienia obowiązują graczy bez aktywnego przypisania.
- **Różdżka** po pomyślnym utworzeniu podregionu znika z ekwipunku, a zaznaczenie jest czyszczone. W razie błędu zapisu nie znika.
- **Granice regionu**: podgląd cząsteczek w ekranie regionu przez 10 sekund, wysyłany tylko do moderatora i tylko w odległości 48 bloków. Nie tworzy barier ani fake bloków, nie działa w tle.
- **Actionbar**: \`Lokalizacja » spawn → afk\`, a dla buforu \`spawn → strefa ochronna\`.
- **Płynność własnego nicku w F5**: interpolacja teleportacji TextDisplay i aktualizacja co 2 ticki; etykiety innych graczy korzystają nadal ze scoreboard.
- **Commands.json**: \`serverName\` (np. \`"MójSerwer"\`) i \`messagePrefix\` (np. \`"&8[&a{server}&8] "\`). Prefiks można też ustawić na pusty ciąg.

## Konfiguracja nazwy serwera

\`\`\`json
{
  "serverName": "MojSerwer",
  "messagePrefix": "&8[&a{server}&8] "
}
\`\`\`

W Commands.json pozostają również pola \`tools\`, \`ranga\`, \`region\`, \`lokalizacje\` z \`enabled\`, \`description\`, \`aliases\` i \`permission\`. Nie zastępuj całego pliku powyższym fragmentem; zmień tylko pola \`serverName\` i \`messagePrefix\`.

Wszystkie definicje rang i regionów są przechowywane w Ranks.json oraz Regions.json; przypisania graczy nadal w MySQL. Po aktualizacji wykonaj pełny restart serwera. Najpierw zrób kopię zapasową JSON i bazy danych.

## Ograniczenia i testy

Przełączniki pokrywają podstawowe zdarzenia Bukkit/Paper, a nie każdą niestandardową mechanikę z innych pluginów. Zdarzenia redstone, kontenery modyfikowane przez inne pluginy i PvP wymagają dodatkowego sprawdzenia na żywym serwerze. Nazwa nad głową w F5 jest wciąż odrębnym TextDisplay i należy ocenić jej płynność na kliencie; interpolacja zmniejsza skoki, ale nie zapewnia pełnego podczepienia modelu.

Zbuduj \`mvn clean verify\`, testy SQL na GitHub Actions uruchamiają MariaDB.

---

## Informacje o wersji 1.5.0

Paper 26.3, Java 25, MySQL/MariaDB (HikariCP), komendy rejestrowane w Java.

## Pliki

Po pierwszym starcie w katalogu plugins/Tools zostaną utworzone:

| Plik | Zawartość |
| --- | --- |
| MySql.json | Host, port, dane logowania, pula połączeń, autosaveSeconds |
| Commands.json | Osobne settings \`enabled\`, \`description\`, \`aliases\`, \`permission\` dla komend \`tools\`, \`ranga\`, \`region\`, \`lokalizacje\` |
| Ranks.json | Ustawienia TAB-u i pełne definicje rang: nazwy, prefixy, suffixy, pozycje, wiadomości wejścia, uprawnienia |
| Regions.json | Ustawienia ochrony, lista regionów, podregionów, flag i punktów teleportacji oraz \`mainSpawn\` |

**Definicje to JSON, dane graczy to SQL.** Zapisy do Ranks.json i Regions.json są atomowe (plik tymczasowy + rename) i nie wykonują zapytań bazodanowych podczas ticków. MySQL przechowuje przypisania rang do UUID, terminy ważności, sesje i statystyki. Usuwamy stary klucz obcy wiążący SQL przypisań z definicjami, nie kasując danych graczy.

### Migracja z wcześniejszych wersji

Przy pierwszym uruchomieniu plugin odczyta stary \`config.json\` i rozdzieli ustawienia pomiędzy cztery pliki. Stary plik zostanie zachowany jako \`config.json.legacy-backup\`. Istniejące rangi i regiony przeniesie **jednorazowo** z MySQL do odpowiednich JSON. Nie usuwa historycznych tabel, aby można było wykonać rollback po przywróceniu wcześniejszej wersji pluginu. Zrób kopię katalogu Tools oraz bazy przed aktualizacją.

## Ochrona spawn

\`\`\`text
/region stworz spawn 100
/region spawn
\`\`\`

Region 100 oznacza 100 bloków w każdą stronę (łącznie 201 × 201 bloków), na całej wysokości. \`Regions.json\` zawiera \`settings.spawnProtectionOutside: 50\`, które automatycznie chroni pas **na zewnątrz** granic głównego spawnu: jego chroniony obszar obejmuje dodatkowo 50 bloków na każdy bok. Pas nie jest nowym regionem, nie ma własnej nazwy i nie pojawia się w GUI ani na liście regionów. Nie istnieje już komenda \`/region ochrona\`. Dawne, błędnie utworzone podregiony \`spawn_ochrona\` nie będą importowane podczas migracji.

Wewnątrz spawnu i jego zewnętrznego bufora blokowane są: niszczenie, budowanie, płyny, PvP, obrażenia, eksplozje, tłoki, ogień, interakcje i moby (chyba że administrator przyzna odpowiednią flagę wewnątrz regionu). Moby wchodzące do strefy z zewnątrz są usuwane. Gdy spawn mobów nie jest dozwolony, próby naturalnego przywołania są anulowane. Wyjątkiem mogą być moby przywołane przez administratora.

## Podregiony

\`\`\`text
/region rozdzka
/region podregion spawn arena
/region edytuj arena flaga pvp tak
/region edytuj arena wejscie premium
/region edytuj spawn
/region lista
/region usun arena
\`\`\`

Zaznacz dwa narożniki różdżką. Podregiony dziedziczą flagi regionu nadrzędnego, ale jawne wartości nadpisują wybrane zasady. Lista regionów i zasady są odczytywane z Regions.json. Własna lokalizacja regionu jest dostępna w GUI \`/lokalizacje\` dopiero po ustawieniu punktu teleportacji poleceniem \`/region spawn\` stojąc w danym regionie.

## Teleport

- Gracze z **aktywną nadaną rangą**, OP i osoby z \`tools.lokalizacje.instant\` teleportują się bez odliczania.
- Gracze bez nadanej rangi mają odliczanie (domyślnie 5 sekund; zmiana w Regions.json), przerywane ruchem lub obrażeniami.
- Papierowe \`teleportAsync\` ładuje chunk bez blokowania głównego wątku.
- Uprawnienia wejścia są sprawdzane ponownie przy wykonywaniu akcji.

## Testy i budowanie

\`\`\`bash
mvn clean verify
\`\`\`

GitHub Actions uruchamia rzeczywistą MariaDB oraz testy Java. Konieczny jest również test na serwerze Paper z mobami, teleportacją i innymi pluginami modyfikującymi zdarzenia. Kopię danych wykonaj przed migracją.
