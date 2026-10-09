# Tools 1.4.0 — regiony, lokalizacje i rangi

Plugin Paper 26.3 / Java 25. Konfiguracja startuje z klas Java i generuje `plugins/Tools/config.json`. Dane regionów, rang i graczy zapisywane są w MySQL/MariaDB przez asynchroniczny silnik SQL.

## Regiony

| Komenda | Funkcja |
| --- | --- |
| `/region stworz spawn 100` | Region 201 × 201 bloków (100 w każdą stronę od gracza), cała wysokość świata |
| `/region rozdzka` | Daje różdżkę (lewy/prawy klik w blok: pierwszy/drugi narożnik) |
| `/region podregion spawn afk` | Tworzy wewnętrzny region AFK z zaznaczenia |
| `/region podregion spawn pvp` | Tworzy strefę PvP w zaznaczeniu |
| `/region edytuj pvp flaga pvp tak` | Odblokowuje PvP tylko w strefie pvp |
| `/region edytuj afk wejscie premium` | Wymaga rangi Premium lub wyższej według jej pozycji |
| `/region edytuj spawn` | GUI zarządzania flagami regionu |
| `/region ochrona spawn 50` | Tworzy chroniony podregion 101 × 101 wokół ustawionego punktu spawn |
| `/region spawn` | Stojąc w regionie ustawia jego punkt teleportacji i globalny spawn nowych graczy |
| `/region lista` | Wyświetla utworzone regiony |
| `/region info spawn` | Pokazuje parametry, dostęp i flagi |
| `/region usun spawn` | Usuwa region i wszystkie jego podregiony |
| `/lokalizacje` | GUI dostępnych dla danej rangi punktów teleportacji |

Domyślna ochrona nowego regionu zabrania: budowania, niszczenia, PvP, zadawania obrażeń (w tym upadku), spawnu mobów, eksplozji, ognia, tłoków, rozlewania płynów i interakcji z kontenerami/przedmiotami. Blokowane są również zdarzenia pochodzące z zewnątrz regionu (przepływ wody, wybuchy, ruch bloków tłokiem). Uprawnienie `tools.region.bypass` lub status OP/ranga zawierająca uprawnienie `*` omija restrykcje administracyjne.

### Zasady podregionów

Nowy podregion **dziedziczy wszystkie ustawienia rodzica**, chyba że wybrana flaga jest ustawiona jawnie. Przykład: strefa `pvp` z włączoną flagą `pvp` pozwala walczyć między graczami znajdującymi się w niej, pozostawiając budowanie i inne czynności zablokowane jak w spawn.

```text
/region edytuj pvp flaga pvp tak
/region edytuj pvp flaga budowanie nie
/region edytuj pvp flaga pvp dziedzicz
/region edytuj pvp wejscie moderator
/region edytuj pvp wejscie wszyscy
```

Pozycja rangi jest jej priorytetem: ranga z pozycją 1 ma dostęp do obszarów wymagających rangi z pozycją 2, ale nie odwrotnie. Ograniczenia dostępowe rodzica również obowiązują w podregionach. Gdy region jest aktywny, gracz widzi jego nazwę na actionbarze.

### Teleportacja

`/lokalizacje` wyświetla do 45 punktów na stronę; podregiony można również udostępnić jako lokalizacje po ustawieniu w nich `/region spawn`. GUI używa kontrolowanego InventoryHolder i PDC. Każdorazowo kontroluje uprawnienia. Teleport trwa **5 sekund** z odliczaniem, dźwiękami i cząsteczkami. Ruch lub obrażenia przerywają odliczanie. Docelowy chunk wczytywany jest asynchronicznie (Paper `teleportAsync`).

Nowy gracz na pierwszym wejściu trafia na główny spawn, jeśli został ustawiony. Zwykły respawn również korzysta ze spawnu, o ile gracz ma tam dostęp.

## Uprawnienia

- `tools.region.admin` — pełna administracja komendą `/region`, tylko operator.
- `tools.region.bypass` — omija restrykcje regionów, tylko operator.
- `tools.lokalizacje` — dostęp do GUI, domyślnie każdy gracz. Docelowy region może dodatkowo wymagać rangi.
- `tools.ranga.admin` — zarządzanie rangami, operator.

## Konfiguracja

Domyślne ustawienia znajdują się w `ToolsConfig.Regions`, NIE w szablonach JSON:

```json
"regions": {
  "enabled": true,
  "maxRadius": 2000,
  "teleportSeconds": 5,
  "cancelTeleportOnMove": true,
  "barTitle": "&aᴏʙꜱᴢᴀʀ &8» &7"
}
```

`JsonConfigManager` uzupełni te opcje w istniejącym `config.json` bez nadpisywania Twoich ustawień bazy i rang. Dane regionów przechowywane są w tabelach `tools_regions`, `tools_region_flags` i `tools_region_settings` (klucze obce i transakcje).

**Ważne:** ochrona regionów jest ładowana asynchronicznie z bazy; dopóki nie ma gotowego cache regionów, działania zmieniające świat są blokowane, aby nie dopuścić do obchodzenia zabezpieczeń przy starcie. Przed uruchomieniem produkcyjnym zapewnij stabilne połączenie MySQL. Przetestuj zabezpieczenia wraz z innymi pluginami, szczególnie TNT, redstone, teleport i PvP.

## Kompilacja i testy

```bash
mvn clean verify
```

GitHub Actions uruchamia testy JUnit i integracyjne testy MariaDB. Wynikowy `Tools.jar` znajduje się w artefaktach workflow. Nie używaj `/reload`; po aktualizacji JAR-a uruchom cały serwer ponownie. Zalecane wykonywanie kopii MySQL przed zmianami struktury danych. Konto MySQL `root` bez hasła jest dopuszczalne **wyłącznie na lokalnym środowisku deweloperskim**.
