# Tools 1.5.0 — niezależna konfiguracja i bezpieczny spawn

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
