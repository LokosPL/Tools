# Architektura i audyt bezpieczeństwa — Tools 1.8.0

> Stan na 10 października 2026 r. Wersja projektu: Paper 26.3, Java 25.

## Zasady

- Konfiguracje początkowe są tworzone **w klasach Java** i generowane do JSON przy starcie. W zasobach są tylko migracje **SQL**; nie zawierają haseł ani domyślnych plików JSON.
- Rejestracja komend odbywa się w Java, a nie w plugin.yml.
- Nie używamy kodów koloru sekcji w ustawieniach. Komponenty Adventure obsługują \`&a\` i \`&#RRGGBB\`.
- JDBC, migracje Flyway, odczyt/zmiana plików JSON nie wykonują się na głównym wątku Paper. Elementy Bukkit API (GUI, scoreboard, Player) pozostają na głównym wątku.

## 1. Wydajność i architektura

**Zaimplementowano**:
- \`ServiceRegistry\` i \`PluginServiceFactory\`: jawne DI przez konstruktory, brak refleksji; klasy zależą od mniejszych kontraktów \`DatabaseExecutor\`.
- \`CompletableFuture\`, ograniczona kolejka SQL (4096 operacji), pula HikariCP oraz zapis sesji gracza z idempotentnym czasem gry. Połączenia i migracje są inicjowane w tle.
- \`MonitoringService\`: liczniki czasu własnych zadań pluginu, liczba powolnych wywołań, pamięć, wątki, TPS, liczba zadań, długość kolejki Hikari i SQL.

**Granice**: Nie każdy możliwy ciężki algorytm posiada osobną implementację asynchroniczną; dane Bukkit muszą pozostać na głównym wątku. Narzędzie nie gwarantuje zerowego TPS ani kompletnego profilowania wszystkich pluginów. Przy niedostępności bazy i nagłym przerwaniu procesu ostatnie niezapisane zdarzenia mogą zostać utracone.

## 2. Komendy i własny system uprawnień

- Narzędzia są rejestrowane w Java przez Paper BasicCommand/Brigadier.
- **Brak własny system rang i jakichkolwiek zależności od zewnętrznych pluginów rang.**
- Definicje rang i przypisanych uprawnień są w Ranks.json. Nadania i daty wygaśnięcia graczy w bazie MySQL/MariaDB/SQLite.
- Gwiazdka `*` oznacza pełne uprawnienia w obrębie istniejącego systemu Tools, wraz z istniejącym mechanizmem przywracania poprzedniego OP.
- GUI rang wyświetla wyłącznie własne uprawnienia Tools. Nie importuje uprawnień innych pluginów.

### 2.1. Whitelist i logowanie

- `Whitelist.json` jest generowany z wartości w Java, osobny writer zapisuje atomowo w tle.
- Tryby: `PRACE_TECHNICZNE`, `CHWILOWA_PRZERWA`, `NOWA_EDYCJA`, `AKTUALIZACJA`.
- `/whitelist`, `/whitelist włącz <tryb>`, `/whitelist wyłącz`, `/whitelist dodaj <nick>`, `/whitelist usuń <nick>`, `/whitelist lista`.
- GUI z główkami graczy, po kliknięciu można usunąć wpis. Zabezpieczone jest przez `tools.whitelist.admin`.
- `AsyncPlayerPreLoginEvent` odrzuca nieuprawnionych przed wejściem. Po aktywacji whitelisty już podłączeni gracze spoza listy są rozłączani.
- Przy planowym `onDisable` plugin wysyła zdefiniowany w Java komunikat wyłączenia.

**Krytyczne ograniczenie offline-mode:** lista sprawdza nick, ale sam nick nie dowodzi własności konta. Gracz może podszyć się pod inny nick, także nick administratora na whiteliście! Do publicznego serwera offline-mode należy osobno rozwiązać problem uwierzytelniania kont (np. własny bezpieczny system sesji i potwierdzania tożsamości). Samo pobranie skórki premium nie jest uwierzytelnieniem.

### 2.2. Premium skin i antybot

- Własny moduł pobiera skórki oficjalnych kont po nazwie na osobnym wątku i ustawia tylko tekstury, nigdy UUID ani uprawnienia. Obowiązuje cache 6h, timeout i limit jednoczesnych żądań. Nie działa bez dostępności usług profili Mojang/Minecraft.
- `Security.json` z Java-defaultami: `premiumSkins`, `antiBot`, oba włączone domyślnie.
- Antybot: limit prób na IP, krótki cache zaufanych par nick/IP po udanym wejściu, whitelistowani gracze pomijają ograniczenie. Bez DNS, SQL i HTTP podczas logowania.
- **Nie zapewnia pełnej ochrony przed botnetem/proxy ani atakiem DDoS**. Pakiety ataku należy ograniczać także na warstwie sieci i proxy.

### 2.3. Spawn i lokalizacje

- `/spawn` używa `RegionManager.mainSpawn()` i tej samej logiki `teleportAsync`/odliczania co `/lokalizacje`. Wymaga ustawienia głównego punktu `/region spawn`.
- Główne lokalizacje widoczne są w pierwszym menu. Kliknięcie otwiera główny teleport i listę podlokalizacji w oddzielnym ekranie. Menu nie pokazuje już niepotrzebnych wymiarów i nazwy świata.
- Własne nazwy, uprawnienia i opisy GUI nie korzystają z pluginów zewnętrznych.

## 3. Dane i migracje

\`MySql.json\` zachowuje format Java → JSON, z dodatkowymi polami:

\`\`\`json
{
  "type": "MYSQL",
  "sqliteFile": "tools.db"
}
\`\`\`

- \`type\` może przyjmować \`MYSQL\`, \`MARIADB\`, \`SQLITE\`; dla MariaDB używamy zgodnego protokołu mysql i Connector/J.
- \`SQLITE\` wykorzystuje lokalną bazę w \`plugins/Tools/tools.db\`, tryb WAL, \`foreign_keys=ON\` i pojedyncze połączenie Hikari, aby ograniczyć ryzyko konfliktów blokad.
- SQL ma dwie implementacje składni upsert: \`SqlDialect\` dla MySQL/MariaDB oraz SQLite; repozytoria danych graczy, rang i regionów używają odpowiedniego dialektu.
- **Flyway 11** ma oddzielne migracje \`db/migration/mysql\` i \`db/migration/sqlite\`. V1 zakłada schemat nowej instalacji, V2 dodaje tabelę audytu technicznego.
- **Zastana baza MySQL/MariaDB**: \`baselineOnMigrate(true)\` zapisuje historię migracji z wersją 1 bez modyfikacji dotychczasowych tabel, a następnie wykonuje V2. **Wymagany backup** przed pierwszym uruchomieniem.
- Zmiana typu bazy nie migruje zawartości z jednej bazy do drugiej. Potrzebne byłoby oddzielne narzędzie eksport/import.
- Testy obejmują SQLite + rzeczywistą MariaDB uruchamianą w kontenerze usługi CI.
- **MySQL na produkcji**: odrębny użytkownik, odpowiednie uprawnienia, TLS; domyślny \`root\` z pustym hasłem jest tylko lokalnym presetem Laragon.

## 4. Przeładowanie konfiguracji

\`/tools przeladuj\` odczytuje JSON-y przez pojedynczą, ograniczoną kolejkę IO, waliduje **wszystkie** pliki i dopiero potem w głównym wątku aktywuje bezpieczne ustawienia.
Zmiany mogą dotyczyć:
- \`Commands.json\`: \`serverName\` i \`messagePrefix\`;
- \`Ranks.json\`: **tylko** wizualne ustawienia \`settings\` (TAB, F5, chat).

Operacja jest odrzucana (z komunikatem) w razie zmian silnika/połączenia MySQL, komend/aliasów/uprawnień, definicji rang, regionów lub po błędnej walidacji. Przeładowanie nie pozostawia częściowo przełączonych modułów i nie resetuje działających połączeń SQL. To **świadomy hot-reload wybranych bezpiecznych ustawień**, nie dowolnych aspektów serwera. Definicje rang i regionów edytuj przez komendy pluginu. Nie używaj \`/reload\` serwera.

## 5. Diagnostyka i logowanie

- \`/tools zdrowie\`: TPS, używana i maksymalna pamięć, wątki JVM, zadania pluginu, typ/status bazy, pula i kolejka SQL.
- \`/tools diagnostyka\`: czas średni i najdłuższy fragmentów własnych zadań, liczba wywołań i powolnych wywołań (>=50 ms).
- Monitor co 10 sekund sprawdza TPS, pamięć i kolejkę SQL; przy przekroczeniu progu loguje ostrzeżenie nie częściej niż raz na minutę.
- SLF4J: oddzielne nazwy loggerów \`Tools.monitoring\`, \`Tools.SQL\`, \`Tools.Rangi\`, \`Tools.Regiony\`, \`Tools.Konfiguracja\`, \`Tools.Administracja\`. Dostępne poziomy DEBUG/INFO/WARN/ERROR konfiguruje system logowania Paper.
- **Granica przypisywania winy**: Spadek TPS nie może być automatycznie przypisany konkretnemu modułowi tylko z jednej próbki. Dla pełnej atrybucji stosuj profiler spark lub Paper timings, a nie automatyczny komunikat „winny moduł X”.

## Testy i wdrożenie

Uruchom \`mvn clean verify\` z Java 25. GitHub Actions buduje JAR i testuje rzeczywistą MariaDB. Testy jednostkowe nie zastępują testów rozruchu całego serwera z prawdziwymi graczami, siecią i uprawnieniami.

**Lista kontrolna po wdrożeniu:**
1. Wykonaj backup bazy MySQL/MariaDB i plików JSON.
2. Uruchom Tools, sprawdź log Flyway oraz \`/tools zdrowie\`.
3. Sprawdź \`/ranga lista\`, nadanie rangi \`*\`, region i teleportację na testowym serwerze.
4. Sprawdź \`/tools przeladuj\` po zmianie header/footer; upewnij się, że zmiana SQL zostaje odrzucona.