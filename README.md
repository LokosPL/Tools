# Tools

Modułowy plugin Minecraft Java **Paper 26.3 / Java 25**. Wszystkie domyślne konfiguracje i komendy definiowane są w kodzie Java. Plugin obsługuje MySQL asynchronicznie przez HikariCP.

## Laragon / HeidiSQL — domyślne połączenie

Wersja **1.0.3-SNAPSHOT** ma wpisane w `config/ToolsConfig.java` ustawienia lokalnego MySQL widoczne na zrzucie HeidiSQL:

```json
{
  "database": {
    "enabled": true,
    "createDatabaseIfMissing": true,
    "host": "127.0.0.1",
    "port": 3306,
    "database": "tools",
    "username": "root",
    "password": "",
    "sslMode": "PREFERRED",
    "poolSize": 6,
    "connectionTimeoutMs": 5000
  }
}
```

To preset **lokalnej bazy deweloperskiej** na tym samym komputerze, na którym działa Paper. **Nie wystawiaj konta root bez hasła do sieci!** W produkcji utwórz dedykowanego użytkownika z minimalnymi uprawnieniami i ustaw mocne hasło.

**Ważne:** puste pole hasła w HeidiSQL nie dowodzi, że konto root naprawdę działa bez hasła. Jeżeli MySQL wymaga hasła, wpisz je tylko w lokalnym `plugins/Tools/config.json` (nie publikuj w GitHub). Alternatywnie ustaw `"password": "${TOOLS_DB_PASSWORD}"` i zmienną środowiskową procesu serwera.

### Uruchomienie

1. Uruchom **Laragon → Start All**, żeby wystartować lokalny MySQL lub MariaDB.
2. Wgraj `Tools.jar` do `plugins/` i uruchom serwer **Paper 26.3** na **Java 25**.
3. Plugin wygeneruje `plugins/Tools/config.json` **z domyślnych wartości zapisanych w Javie**. Jeśli plik już istnieje ze starym, niezmienionym presetem MySQL 1.0.2, jego sekcja `database` zostanie jednorazowo zmigrowana. Jeśli zmieniałeś ustawienia bazy samodzielnie, nie będą nadpisywane.
4. Asynchronicznie zostanie wykonane `CREATE DATABASE IF NOT EXISTS tools` i przygotowanie tabel `tools_players` oraz `tools_sessions`. Użytkownik DB potrzebuje uprawnień CREATE DATABASE oraz CREATE TABLE (lub przygotuj bazę i tabele wcześniej).
5. Zobacz konsolę Paper i wpisz jako OP: `/tools status`, `/tools ping`, `/tools stats <nick>`. Status `STARTING` oznacza inicjalizację w tle, `READY` udane połączenie, a `FAILED` błąd konfiguracji lub serwera MySQL.

Jeśli serwer Paper działa na **innym komputerze** niż Laragon, adres `127.0.0.1` wskazuje host Minecraft, a nie komputer HeidiSQL — podaj w configu poprawny adres sieciowy bazy, skonfiguruj firewall i uprawnienia MySQL. Nie otwieraj MySQL bezpośrednio do publicznego Internetu.

## Konfiguracja i architektura

```text
src/main/java/pl/lokos/tools/
  basic/        ToolsPlugin (start)
  commands/     ToolsCommand (Paper BasicCommand)
  config/       ToolsConfig, ToolsConfigMigration, JsonConfigManager
  database/     DatabaseManager, PlayerRepository, PlayerSnapshot
  enums/        DatabaseStatus
  helpers/      PlayerDataHelper
  inventorys/   InventoryRegistry
  listeners/    PlayerConnectionListener
  manager/      PlayerDataManager
  registry/     ConfigRegistry, CommandRegistry
  tasks/        AutosaveTask
  utils/        ThreadChecks
  variables/    PluginConstants
```

- `plugin.yml` zawiera tylko metadane. Komendy rejestrowane są przez Paper w klasach Java, bez sekcji `commands` w YAML.
- Wartości domyślne konfiguracji określają **klasy Java**, nie pliki JSON w `resources`. `JsonConfigManager` tworzy/uzupełnia brakujące klucze, zachowuje ręcznie zmienione ustawienia i weryfikuje dane przed zapisem.
- `ConfigRegistry` wczytuje wszystko przy `onEnable()`, zanim zostaną zarejestrowane komendy i listenery.
- MySQL jest inicjalizowany na osobnym wątku; HikariCP utrzymuje pulę połączeń, a sesje są zapisywane co określoną liczbę sekund oraz przy wyjściu i zamknięciu serwera.
- Brak trwałego offline-spool: przy awarii bazy lub procesu serwera możliwa jest utrata ostatnich niezapisanych sekund.

## Komendy

| Komenda | Funkcja |
| --- | --- |
| `/tools help` | Pomoc |
| `/tools status` | Stan MySQL i liczba sesji |
| `/tools ping` | Pomiar czasu zapytania SQL |
| `/tools stats <nick>` | Wejścia i czas gry |

Domyślne uprawnienie `tools.admin` (OP).

## Maven / GitHub Actions

`mvn clean verify` (Java 25) uruchamia testy i generuje `target/Tools.jar`. Gotowe buildy: [GitHub Actions](https://github.com/LokosPL/Tools/actions/workflows/build.yml).

Testy jednostkowe obejmują generowanie JSON, migrację starych domyślnych ustawień i ich walidację. Kompilacja GitHub nie potwierdza dostępności lokalnej bazy Laragon: tę sprawdź komendą `/tools ping` po uruchomieniu serwera Minecraft.
