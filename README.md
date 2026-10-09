# Tools

Modułowy plugin **Minecraft Java 26.3 / Paper 26.3** z asynchronicznym zapisem sesji do MySQL. Wymaga **Java 25**. Projekt startowy do dalszej rozbudowy w IntelliJ IDEA.

## Instalacja

1. Pobierz **Tools.jar** z [GitHub Actions](https://github.com/LokosPL/Tools/actions/workflows/build.yml) → najnowsze udane uruchomienie → **Artifacts: Tools-Paper-26.3**. Alternatywnie skompiluj: `mvn clean verify` (Java 25), wynik: `target/Tools.jar`.
2. Umieść JAR w katalogu `plugins/` serwera **Paper 26.3**, uruchom serwer, aby utworzyć `plugins/Tools/config.json`.
3. Utwórz pustą bazę MySQL 8.0+ z kodowaniem `utf8mb4` (wymagane uprawnienia CREATE TABLE, INSERT, SELECT, UPDATE), np. `CREATE DATABASE tools CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`.
4. Ustaw w `config.json` host, port, bazę, użytkownika oraz `"enabled": true`. Hasło wpisz w `"password"` albo najlepiej zostaw `"${TOOLS_DB_PASSWORD}"` i ustaw zmienną środowiskową `TOOLS_DB_PASSWORD` dla procesu serwera.
5. Zrestartuj serwer. Sprawdź: `/tools status`, `/tools ping`, `/tools stats <nick>`.

**Uwaga:** początkowo `database.enabled=false`, by plugin mógł wystartować przed skonfigurowaniem dostępu do MySQL. Nie przesyłaj swojego prawdziwego `config.json` do GitHuba. MySQL musi być dostępny sieciowo z hosta serwera.

## Uruchamianie i rejestrowanie komend

- `plugin.yml` zawiera **wyłącznie metadane** wymagane przez Paper do załadowania głównej klasy `ToolsPlugin`. Nie zawiera `commands:` ani `permissions:`.
- Komendy rejestruje klasa `registry/CommandRegistry.java` poprzez **Paper `JavaPlugin#registerCommand` + `BasicCommand`**. Nie korzystamy z `getCommand()` ani starego Bukkit `CommandExecutor`.
- Podczas `onEnable()` ładowany i walidowany jest **jeden raz** `plugins/Tools/config.json`. Dopiero potem plugin uruchamia MySQL i rejestruje komendy, listenery oraz zadania.
- Zmiany konfiguracji (np. aliasu, hasła MySQL, liczby sekund autosave) wymagają **pełnego restartu serwera**. Nie czytamy ponownie plików w czasie działania, a `/reload` nie jest zalecany.
- Jeśli `config.json` powstał w wersji 1.0.0, nowa sekcja `commands` jest **opcjonalna**: Java użyje domyślnych ustawień; aby edytować aliasy/uprawnienia, dopisz ją ręcznie. Plugin **nie nadpisuje istniejącej konfiguracji**.
- Uprawnienia są deklarowane w Javie: domyślnie `tools.admin` ma dostęp dla OP. Można zmienić identyfikator permisji w JSON; rejestracja Paper uwzględnia ją przy wyświetlaniu i wykonywaniu komend.

### Przykładowy config.json

```json
{
  "autosaveSeconds": 30,
  "commands": {
    "tools": {
      "enabled": true,
      "description": "Informacje, status i statystyki Tools",
      "aliases": ["narzedzia"],
      "permission": "tools.admin"
    }
  },
  "database": {
    "enabled": true,
    "host": "127.0.0.1",
    "port": 3306,
    "database": "tools",
    "username": "tools",
    "password": "${TOOLS_DB_PASSWORD}",
    "sslMode": "PREFERRED",
    "poolSize": 6,
    "connectionTimeoutMs": 5000
  }
}
```

W środowisku produkcyjnym przy konfiguracji poprawnego certyfikatu MySQL zalecane `sslMode: VERIFY_IDENTITY`. Unikaj `DISABLED` dla połączeń zdalnych.

## Architektura

```
src/main/java/pl/lokos/tools/
  basic/        ToolsPlugin (cykl życia i uruchamianie konfiguracji)
  commands/     ToolsCommand (/tools, implementacja Paper BasicCommand)
  enums/        DatabaseStatus
  helpers/      PlayerDataHelper
  config/       JsonConfigManager, ToolsConfig
  utils/        ThreadChecks
  variables/    PluginConstants
  inventorys/   InventoryRegistry (rozszerzalne GUI)
  listeners/    PlayerConnectionListener
  registry/     CommandRegistry (rejestracja komend w Javie)
  tasks/        AutosaveTask
  manager/      PlayerDataManager
  database/     DatabaseManager, PlayerRepository, PlayerSnapshot
```

- **JDBC i MySQL poza głównym wątkiem**: dedykowana kolejka z ograniczoną pojemnością 4096 operacji, 1-2 pracowników oraz HikariCP z pulą do 6 połączeń (konfigurowalne).
- **Bezpieczne ponowienia czasu gry**: każda sesja posiada losowy UUID; w SQL zapisujemy największy dotychczasowy czas tej sesji (`GREATEST`), więc ponowny zapis tego samego snapshotu nie podwaja czasu.
- **Automatyczne tworzenie tabel**: `tools_players` (UUID, nazwa, wejścia, pierwszy/ostatni kontakt) i `tools_sessions` (UUID sesji, UUID gracza, czas).
- **Zapis danych**: wejście rejestrowane natychmiast asynchronicznie, sesja co 30 s (konfigurowalne), wyjście gracza oraz zamknięcie serwera. Nie czytamy obiektów Bukkit we wątkach JDBC.
- **Retry**: nieudane zapisy aktywnych/poprzednich sesji ponawiane w kolejnym autosave, dopóki proces serwera działa. Błąd połączenia przy tworzeniu tabel ujawnia status `FAILED` (wymaga naprawy DB i restartu pluginu).
- **Ograniczenie**: gdy serwer ulegnie awarii przed zapisem lub baza pozostaje nieosiągalna podczas zamykania, dane z ostatnich sekund mogą zostać utracone. To nie jest trwały lokalny WAL/offline-spool ani architektura wieloserwerowa.

## Komendy

| Komenda | Funkcja |
| --- | --- |
| `/tools help` | Pomoc |
| `/tools status` | Stan MySQL i liczba sesji online |
| `/tools ping` | Pomiar odpowiedzi SQL |
| `/tools stats <nick>` | Liczba wejść i zapisany czas gry |

Domyślne uprawnienie: `tools.admin` (OP).

## Rozwój i testy

```bash
mvn clean verify
```

Projekt zawiera testy JUnit i workflow GitHub Actions. W IntelliJ IDEA otwórz katalog jako Maven Project, ustaw SDK Java 25 i włącz import zależności. Integracyjny test połączenia wymaga osobnej działającej instancji MySQL, której nie dostarcza repozytorium.

Nie przechowuj danych dostępowych w kodzie; wykonuj backup MySQL przed migracjami.
