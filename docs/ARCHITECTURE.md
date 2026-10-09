# Architektura i audyt bezpieczeństwa — Tools 1.7.0

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

## 2. Komendy i uprawnienia

- Wszystkie komendy są rejestrowane w kodzie przez \`Paper BasicCommand\` (bazuje na Brigadier). Istnieją walidatory zakresów, dat/czasów, UUID, nazw i podpowiedzi. Nie jest to jednak pełna migracja do biblioteki Cloud z deklaratywnymi typami argumentów — obecna implementacja zachowuje zgodność komend.
- \`LuckPermsBridge\` jest **opcjonalnym** adapterem do LuckPerms 5.5. Dostępny tylko przy aktywnym LuckPerms, nie instaluje go i nie zmienia automatycznie wszystkich rang Tools w grupy LuckPerms.
- \`/tools lp nadaj <nick> <node> <czas|*> [świat]\` nadaje własną permisję z czasem i opcjonalnym kontekstem \`world\`.
- \`/tools lp dziedzicz <nick> <grupa> <czas|*> [świat]\` nadaje węzeł dziedziczenia grupy LP (grupa musi być wcześniej poprawnie skonfigurowana w LuckPerms).
- \`/tools lp sprawdz <nick> <node> [świat]\` bada wynik uprawnienia z API LP (uwzględnia dziedziczenie i konteksty).
- Pozostaje autorski system rang i grantów Tools; grupy LP nie są automatycznie importowane/eksportowane. Przy LuckPerms odświeżanie uprawnień odbywa się także po zmianie świata. Polecenia LP są administracyjne i dostępne tylko z uprawnieniem \`tools.admin\`.

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
5. Opcjonalnie zainstaluj LuckPerms i sprawdź grupę kontekstową w dwóch światach; brak LP nie może blokować startu Tools.
