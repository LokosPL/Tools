# Tools 1.13.0-SNAPSHOT — meteoryty, Tytani i wyzwania eventowe

## Nowa rozgrywka — sezonowe wyzwania i ekspedycje

- **Dziewięć wydarzeń**, w tym nowe **Łowy na Tytanów** (`/lowy`).
  Administracja: `/event wlacz lowy 1h` lub `/event wlacz meteory 1h`.
- `/eventy` otwiera panel eventów. `/eventy wyzwania` otwiera trzy zadania
  bieżącego eventu (nazwy i progi są inne dla różnych typów). Każdy próg daje
  automatycznie konfigurowalną liczbę kluczy eventowych (domyślnie 1, 2, 3).
  Postępy i odblokowane progi są trwałe w `EventState.json` i zerują się
  dopiero przy rozpoczęciu następnego eventu. Niewypłacone klucze z kolejki
  przechodzą przez zmianę eventu i ponowne uruchomienie.
- **Meteoryty**: podczas eventu co 180 sekund próba utworzenia meteorytu przy
  graczach Survival w załadowanych chunkach. Obiekt z hologramem i efektem
  cząsteczek można zebrać prawym przyciskiem. Zwykły daje 1 klucz; złoty
  (5% szansy) daje 3 klucze i specjalny kilof z `SpecialItems.json`.
  `/meteory gdzie` podaje współrzędne; `/meteory namierz` kieruje kompas
  na najbliższy meteoryt we własnym świecie. Meteoryty nie niszczą bloków,
  nie powodują wybuchów i nie wymuszają ładowania chunków.
- **Łowy**: Tytani (nazwane, wzmocnione Huski) pojawiają się co 180 sekund
  podczas aktywnego `/lowy`. Pokonanie daje klucz eventowy i punkt wyzwania.
  Nie mogą niszczyć drzwi ani podnosić cudzych przedmiotów; mają ograniczony
  czas życia, limit 2 sztuk oraz automatyczne sprzątanie po evencie.
- Meteoryty i Tytani powstają **wyłącznie w dziczy**: nigdy wewnątrz
  regionów Tools, w buforze spawnu ani poza WorldBorder. Wymagane są
  załadowane chunki, płaskie bezpieczne podłoże oraz gracz Survival.
  Jeżeli nie ma odpowiedniej lokacji, spawn zostaje pominięty zamiast
  wymuszać generację świata.
- W `Events.json`: `meteorEnabled`, `meteorSpawnIntervalSeconds`,
  `meteorLifetimeSeconds`, `meteorMaxActive`, `meteorRareChance`,
  `meteorRareKeys`, `eliteEnabled`, `eliteSpawnIntervalSeconds`,
  `eliteLifetimeSeconds`, `eliteMaxActive`, `eliteKeysPerKill`,
  `challengesEnabled`, `challengeKeyRewards`, `challengeFlushSeconds`
  oraz przełączniki efektów i ogłoszeń. Wszystkie wartości domyślne są
  w Javie i dopisywane do istniejącego JSON bez usuwania starych ustawień.
- **AFK**: bonus za ciągły pobyt wyłącznie w regionie `afk` (w tym
  podregionach), bossbar z odliczaniem, 1–4 klucze za kolejne pełne okresy.
  Samo stanie w miejscu wystarcza, chyba że ustawiono
  `afkRequireActivity=true`. Wyjście ze strefy zeruje serię, ale
  zachowuje zapisany licznik pełnych minut.

**Bezpieczeństwo aktualizacji:** wykonaj kopię świata oraz `plugins/Tools`,
następnie wgraj nowy JAR i wykonaj pełny restart Paper 26.3 / Java 25.
Obiekty meteorów i Tytanów są efemeryczne (znikają po restarcie),
ale postępy wyzwań i kolejka nagród są trwałe. Testy JUnit/MariaDB nie
zastępują testu rozgrywki z graczami. Odbiór fizycznych kluczy i zapis
ekwipunku Minecraft nie tworzą wspólnej transakcji — przy awaryjnym
zaniku zasilania możliwy jest przypadek ponowienia wypłaty; wymagany jest
dodatkowy test odporności na crash przed wdrożeniem na duży serwer.

---

# Tools 1.13.0-SNAPSHOT — eventy, skrzynie i rozrost mapy

## Uruchamianie eventów

- `/event lista`, `/event status`, `/event wlacz zima 2h`,
  `/event zakoncz` — administracja z `tools.event.admin`.
- **Maksymalnie jeden event naraz**, także po restarcie. Stan, czas końca
  i postęp graczy w `EventState.json`; konfiguracja szans w `Events.json`.
- Publiczne informatory z GUI: `/zima`, `/halloween`, `/wielkanoc`,
  `/lato`, `/zabojstwa`, `/meteory`, `/zniwa`, `/wedkowanie`, `/lowy`.
- Standardowa szansa zdobycia pamiątki eventowej wynosi **2%** od
  odpowiedniej aktywności: zima — kopanie, Halloween — wrogie moby,
  Wielkanoc — kwiaty/trawa, lato i wędkowanie — połowy, PvP — zabójstwa,
  meteory — rudy, żniwa — uprawy. Powtórne zabijanie tego samego gracza
  ma pięciominutowe ograniczenie.
- Zima generuje efekt opadania płatków śniegu wokół graczy w Overworld,
  także w ciepłych biomach, **bez zmiany biome, flag regionów lub bloków**.
  Nie jest to fizyczna zamiana całego świata w śnieżny biom.
- Zbieranie pamiątek daje po 10 sztukach jeden klucz eventowy.
  Skrzynia eventowa losuje przedmiot aktualnego wydarzenia.
- Każde wydarzenie ma własny przedmiot w `SpecialItems.json` (łącznie
  osiem nowych przedmiotów, plus istniejące Buty Szybkości).

## Pięć skrzyń

- `/skrzynia` lub `/skrzynia gui`: panel administracyjny.
  Kliknij skrzynię, otrzymasz blok z unikatową metką, a następnie
  postaw go **w granicach głównego regionu Spawn**. Wymaga permisji
  `tools.skrzynia.admin` i możliwości stawiania bloków w regionie.
- `/skrzynia klucz eventowa Steve 1` wydaje klucze;
  `/skrzynia usun` usuwa skrzynię, na którą patrzysz;
  `/skrzynia lista` pokazuje liczbę skrzyń.
- Zwykła, Premium, AFK, Eventowa i Specjalna mają oddzielne tabele
  nagród w `Crates.json`. Stan bloków i minut AFK w `CratesState.json`.
- Klucze zwykłe/specjalne wypadają czasem z mobów; klucz AFK
  za pobyt w regionie `afk`; klucz eventowy za pamiątki; premium
  obecnie przez admina.
- W regionach można kliknąć **wyłącznie zarejestrowaną skrzynię Tools**
  nawet przy fladze zakazującej otwierania pojemników. Nie zmienia to
  ochrony pozostałych skrzyń, budowania, kopania i interakcji.
- Ochrona przed zniszczeniem, TNT, tłokami, hopperami, podwójnymi chestami
  i przenoszeniem przedmiotów przez zwykły ekwipunek. Loot losowany
  jest po sprawdzeniu pasującego klucza i miejsca w ekwipunku.
- Hologramy TextDisplay i lekkie particle istnieją tylko przy wczytanych chunkach.
  Dane skrzyń pozostają zapisane po wyłączeniu serwera.

## Granica świata

- `WorldBorder.json`: domyślnie **1500 × 1500** wokół głównego spawnu,
  automatyczne powiększanie co 7 dni po zebraniu 120 graczogodzin.
  Awaryjnie po 14 dniach mały krok. Aktywność AFK nie jest naliczana
  po pięciu minutach bez ruchu; limit 240 min/UUID/dobę.
- Rozmiar i dzienny postęp przechowuje `WorldBorderState.json`.
  Działki/regiony istniejące poza nowym limitem **nie są obcinane**:
  pierwszy rozmiar jest zwiększany tak, by objąć wszystkie istniejące
  regiony; niestandardowo większej granicy też nie zmniejszamy.
- Nowe regiony Tools poza aktywnym WorldBorder nie mogą być tworzone.
  Inne światy, np. Nether, nie podlegają tej jednej granicy Overworld.
- `/granica status`, `/granica pauza`, `/granica wznow`,
  `/granica rozbuduj 500` — uprawnienie `tools.granica.admin`.
- **Pregeneracja nowych chunków nie jest jeszcze wykonywana przez Tools**.
  Zmiana WorldBorder może przyciągnąć graczy do nowych chunków, więc
  przed dużym eventem zalecana jest pregeneracja kontrolowanym narzędziem
  lub test wydajności na kopii świata.
- Jeden `BossBarHub` wyświetla paski we właściwej kolejności:
  **event → combat-log → broadcast → rozrost świata → AFK**. Nie usuwa
  bossbarów innych pluginów.

## Poprawki istniejących komend

- `/fly Steve wlacz` i `/fly wlacz Steve` włączają latanie innej osobie
  tylko z `tools.fly.others`. Tak samo `/vanish Steve wlacz`
  i `/vanish wlacz Steve`, z `tools.vanish.others`.
- Autouzupełnianie argumentów nie ujawnia vanisha ani operacji,
  których nie wolno wykonać. Nowe komendy także mają oddzielne
  pliki `plugins/Tools/commands/*.json` generowane z Javy.

## Zasady aktualizacji

Najpierw **wykonaj kopię świata i całego folderu `plugins/Tools`**,
wgraj JAR i wykonaj pełny restart. Konfiguracje są generowane z klas
Java, stare wpisy są zachowywane. Nie używamy LuckPerms, nie przenosimy
SQL na główny wątek ani nie rejestrujemy komend w `plugin.yml`.

Testy Maven/JUnit potwierdzają m.in. walidację plików, granice regionów,
serię uprawnień, stan jedynego eventu, zapisy skrzyń i losowanie.
**Nie zastępują próby na serwerze Paper z graczami**.
Konieczne testy: stawianie skrzyń na chronionym spawnie, wybuchy,
relog podczas eventu, zużywanie kluczy, warunki PvP, opady w różnych biomach
i zmiana WorldBorder przy istniejących działkach.

---

# Tools 1.12.0-SNAPSHOT — ochrona, walka i przedmioty

## Antycheat i ochrona wydajności
- `/antycheat` lub `/ac`, `/ac status`, `/ac wlacz`, `/ac wylacz`:
  przełączanie **detekcji ruchu, nieprawidłowego zasięgu i serii stawiania bloków**
  dla wszystkich graczy, także OP. Wymaga `tools.antycheat.admin`.
- `/ac powiadomienia wlacz`, `/ac powiadomienia wylacz`:
  indywidualne powiadomienia moderatora z `tools.antycheat.alerts`.
  Konfiguracja w `AntiCheat.json`, stan i wyciszone UUID w `AntiCheatState.json`.
- Wszystkie detekcje są **ostrożne**: brak automatycznych banów i wyrzucania.
  Potwierdzony długotrwały anomalny ruch może zostać skorygowany.
  Elytra, latanie, pojazdy, efekty mikstur, teleportacja, knockback
  i spadki TPS są uwzględniane przy kontroli szybkości.
- Mechanizmy przeciążające serwer: progi dla aktualizacji redstone,
  przenoszenia itemów przez hoppery, cykli tłoków, TNT i spawnów mobów
  na chunk. **Zabezpieczenia wydajności działają również po `/ac wylacz`.**
- Nie ma gwarancji wyłapania wszystkich cheatów ani ochrony przed atakami
  pakietowymi/proxy — ten system działa w Paper API. Monitoruj na żywym
  serwerze oraz skonfiguruj limity pakietów i chunków bezpośrednio w Paper.

## Combat-log i respawn
- `Combat.json`: domyślnie 18 sekund walki, osobne opcje walki z graczem
  i mobami, wylogowania i spawnów. Bossbar pokazuje odliczanie.
- Combat-tag powstaje wyłącznie przy nieanulowanym, dodatnim obrażeniu
  od innego żywego przeciwnika (lub jego pocisku). Samouszkodzenie,
  lawa, upadek, cancelowane hity i śmierć środowiskowa nie wyzwalają taga.
- Wylogowanie w walce powoduje śmierć postaci (normalne zachowanie dropów),
  z osobnym komunikatem. Standardowe komunikaty śmierci zastępowane są
  czytelnymi polskimi tekstami dopasowanymi do przyczyny.
- Po śmierci gracz wraca na główny Spawn regionów Tools; jeśli spawn nie jest
  skonfigurowany, na spawn głównego świata Overworld.
- Vanilla ogłoszenia zdobycia osiągnięć zostały wyłączone we wszystkich
  światach przez gamerule `announceAdvancements=false`, także w światach
  wczytywanych później. **Zdobywanie advancementów i toast klienta
  nie są wyłączone** — do pełnej zmiany tej mechaniki potrzebne są
  oddzielne zasoby/datapack i testy zgodności z rozgrywką.

## GUI przedmiotów
- `/przedmiot gui` otwiera panel dla siebie;
  `/przedmiot gui <nick>` otwiera panel rozdawania wybranemu
  graczowi online. Lewy klik daje 1, prawy 4 sztuki.
- GUI jest zabezpieczone InventoryHolder, anulowaniem przesuwania itemów,
  kontrolą UUID odbiorcy, dostępności miejsca i ponownym sprawdzaniem
  `tools.przedmiot.admin` przy każdym kliknięciu.
- Testowe `buty_szybkosci` pozostają w `SpecialItems.json`, bez
  ingerowania w istniejące dane itemów i regionów.

**Uruchomienie:** pełny restart Paper 26.3 z Java 25.
Nowe pliki JSON powstają z domyślnych definicji Java bez usuwania danych.
Przetestuj próbne PvP i walkę z mobami, wylogowanie podczas walki,
śmierć i respawn, a także mechanizmy redstone na kopii świata.
CI/Maven nie zastępuje testów na serwerze z graczami.

---

## Świat survival, przedmioty eventowe i TAB (1.11.0-SNAPSHOT)

- [Koncepcja rozrastającej się mapy](docs/granica-swiata-survival.md) opiera się na cyklach aktywności,
  z ograniczeniem AFK. **To projekt, a nie uruchomiona granica świata.**
- SpecialItems.json: testowa definicja buty_szybkosci, z ukrytym Unbreaking III,
  efektami Speed II i Jump Boost I, opisem eventu oraz opcjonalnym modelem przedmiotu.
- Administracyjne /przedmiot lista, /przedmiot info buty_szybkosci,
  /przedmiot daj <nick> buty_szybkosci [1-16], permisja tools.przedmiot.admin.
- Visuals.json: konfigurowalna paleta HEX komunikatów; jawne HEX w innych
  JSON-ach pozostają nienaruszone.
- Wieloargumentowy TAB działa również po /inventoryopen eq, /msg wycisz,
  /gamemode <nick> i /chat slow, zgodnie z rangami i widocznością vanisha.

Po instalacji wymagany pełny restart i sprawdzenie na Paper. Przedmiot jest
wydawany tylko administratorowi przez komendę; definicja nie modyfikuje
przedmiotów już posiadanych przez graczy. Efekty po zdjęciu butów mogą
utrzymać się maksymalnie ok. 3 sekundy.

---

# Tools 1.10.0-SNAPSHOT — komendy administracyjne

Nowe komendy są rejestrowane w Java przez Paper API, **nie** w `plugin.yml`.
Domyślne konfiguracje: `plugins/Tools/Commands.json`,
`plugins/Tools/StaffTools.json`, `plugins/Tools/StaffState.json` i osobne
`plugins/Tools/commands/{tp,vanish,helpop,gamemode,fly,broadcast,inventoryopen,speed}.json`.
Istniejące pliki JSON i stan graczy są zachowywane — loader dopisuje tylko
brakujące pola.

## Teleportacja administracyjna

- `/tp <nick>`: teleportuje wykonującego do gracza.
- `/tp <x> <y> <z>`: własne współrzędne; obsługiwane też `~` i `~10`.
- `/tp <nick> <x> <y> <z>`: teleportuje gracza na koordynaty.
- `/tp *`: teleportuje wszystkich do wykonującego.
- `/tp * <x> <y> <z>`: teleportuje wszystkich w świat wykonującego i na współrzędne.
- Z konsoli podaj konkretnego gracza z koordynatami; przed rozpoczęciem
  teleportacji grupowej walidowane są wszystkie cele.
- Permisje: `tools.tp`, dodatkowo `tools.tp.others`, `tools.tp.all`.

## Vanish i nadzór

- `/vanish [wlacz|wylacz]`, `/vanish <nick> [wlacz|wylacz]`.
- Stan jest trwały dla UUID w `StaffState.json`; po ponownym wejściu
  zachowana jest niewidzialność dla zwykłych graczy i brak wpisu w TAB.
- Ukrytych graczy widzą osoby z `tools.vanish.use` albo `tools.vanish.see`,
  także ich złoto-czerwoną etykietę `✦ VANISH` nad nazwą postaci.
- Moby nie mogą ich targetować standardowym zdarzeniem Paper; nie podnoszą itemów.
  Wiadomości publiczne na vanish są kierowane wyłącznie do widzących vanish.
- `tools.vanish.monitor` otrzymuje powiadomienia na chacie o włączeniu i wyłączeniu.
  `tools.vanish.others` pozwala zmieniać vanish innych.

## Helpop, gamemode i fly

- `/helpop <tekst>`: osobista wiadomość do administracji z
  `tools.helpop.receive`; limit 30 s (zmieniany w StaffTools.json).
  `tools.helpop.bypass.cooldown` omija limit.
- `/gamemode <1|2|3|4>` oraz `/gm <nick> <tryb>`:
  w Tools liczby oznaczają **1 survival, 2 creative, 3 adventure, 4 spectator**.
  Nazwy angielskie także są obsługiwane.
  Permisje: `tools.gamemode`, `tools.gamemode.others`.
- `/fly [wlacz|wylacz]` i `/fly <nick> [wlacz|wylacz]`.
  Monitorowanie przełączania: `tools.fly.monitor`.
  Zmiana innych: `tools.fly.others`.

## Bossbar, inwentarz i speed

- `/broadcast 30s <tekst>`, `/broadcast 5m <tekst>`,
  `/broadcast 2h <tekst>`, `/broadcast 1d <tekst>` itd.
  `/broadcast wylacz` kończy ogłoszenie przed czasem.
  Komunikat jest widoczny na bossbarze i trwa do zapisanego końca,
  także po ponownym uruchomieniu serwera. Jednocześnie aktywny jest jeden
  komunikat (kolejny zastępuje poprzedni).
- `/inventoryopen eq <nick>` lub `/inventoryopen enderchest <nick>`:
  dostęp do **gracza online**. Kolejne kliknięcia ponownie sprawdzają
  permisje, także po zmianie rangi.
  Permisje: `tools.inventoryopen`, `tools.inventoryopen.enderchest`.
- `/speed <1-10>`, `/speed <walk|fly> <1-10> [nick]`,
  `/speed <nick> <1-10>`; osobne `tools.speed.others`.
- Wszystkie uprawnienia specjalne nadawane są przez wbudowany system rang Tools
  (bez LuckPerms). OP zachowuje bezpośredni dostęp.

**Testy i ograniczenia:** Maven/JUnit w GitHub Actions obejmuje parsery,
stan JSON, bezpieczeństwo wiadomości i autoryzację komend. Pełna weryfikacja
na dwóch klientach Minecraft oraz zachowania wszystkich mobów, GUI i TAB
wymaga uruchomienia rzeczywistego serwera Paper 26.3. Zalecana jest kopia
danych i pełny restart po aktualizacji.

---

# Tools — poprawa idempotencji poleceń (aktualna)

W komendach modyfikujących konfigurację rozróżniamy **zmianę**, **brak zmiany** i **błąd**.
Ponowne wykonanie `/msg wlacz`, `/msg wylacz`, `/msg wycisz`,
`/chat wlacz`, `/chat wylacz`, `/chat ogloszenia wlacz/wylacz`,
`/chat slow`, `/chat ogloszenia interwal`,
`/whitelist wlacz/wylacz/dodaj/usun`, ustawianie istniejących flag regionów,
pozycji rang, uprawnień rangi i tego samego przypisania rangi nie powoduje
niepotrzebnego zapisu ani komunikatu sukcesu.

Neutralny komunikat typu **„Prywatne wiadomości są już włączone”** ma własną
kolorystykę i jest ograniczany czasowo, aby powtarzanie polecenia nie zalewało czatu.
Zdarzenia GUI regionów i whitelisty także rozróżniają brak zmiany od błędu.

Część poleceń jest tylko odczytem (`/chat status`, `/region info`, `/tools ping`)
albo akcją wykonywaną na żądanie (`/chat wyczysc`). Nie są one blokowane
jako „już zrobione”, ponieważ ich ponowne wykonanie ma sens.

Testy regresyjne obejmują wielokrotne przełączanie MSG/czatu, ponowne wyciszanie
i ignorowanie oraz niewykonywanie identycznej zmiany pliku JSON. Kompilacja i testy
CI nie zastępują próby na działającym serwerze Paper.

---

# Tools 1.9.2 — prywatne wiadomości i ustawienia czatu (aktualne)

## Czat — ustawienia z gry i JSON

- `/chat slow <liczba> <sekundy>` — np. `/chat slow 2 3` ogranicza gracza do 2 wiadomości w oknie 3 s.
- `/chat ogloszenia interwal <sekundy>` — np. `/chat ogloszenia interwal 60` wysyła kolejne ogłoszenia co minutę (minimum 20 s, maksimum 86400 s).
- `/chat ogloszenia lista` — lista treści i interwału. `/chat ogloszenia wlacz` lub `wylacz` ustawia ich stan.
- Konfiguracja jest w `plugins/Tools/Chat.json`. Modyfikacje tych parametrów z komendy są atomowe i zachowują nieznane pola w pliku, w tym ręcznie dodane komentarze jako pola JSON (JSON nie wspiera komentarzy tekstowych).
- `/chat przeladuj` pozwala ponownie wczytać ręcznie edytowany `Chat.json` bez restartu.
- Stan czatu i wyciszenia nadal są zachowywane w `ChatState.json`, bez zmian w SQL.

## MSG — prywatne wiadomości

- `/msg <nick> <tekst>`, `/tell`, `/w` — wiadomość prywatna.
- `/reply`, `/r`, `/replay` — odpowiedź ostatniemu dostępnemu rozmówcy.
- `/msg wylacz` — wyłącza własną możliwość pisania i odbierania wiadomości; `/msg wlacz` ponownie włącza.
- `/msg wycisz <nick>` — ignoruje przychodzące wiadomości od wskazanego gracza online.
  `/msg odcisz <nick>` odblokowuje również po jego wyjściu, a `/msg wyciszeni` pokazuje listę.
- `tools.msg.staff` — jedyne uprawnienie pozwalające wysyłać MSG do chronionej administracji; bez tej permisji zwykli gracze są odrzucani.
- `tools.msg.protected` — dodatkowo chroni skrzynkę wiadomości wybranej rangi. Rangi administracyjne Tools są chronione także automatycznie.
- `tools.msg.bypass.cooldown` — omija ograniczenie szybkości wysyłania prywatnych wiadomości, ale **nie** omija ignorowania, wyłączenia ani wyciszenia na czacie.
- `tools.msg.use` — publiczne uprawnienie rejestracji komend MSG (Paper `PermissionDefault.TRUE`), natomiast specjalne uprawnienia MSG są zarządzane przez rangi Tools.
- `plugins/Tools/PrivateMessages.json` — odstęp między wiadomościami w ms, limit znaków, ochrona administracji i format wiadomości; administrator czatu może wczytać zmiany poprzez `/msg przeladuj`.
- `plugins/Tools/PrivateMessagesState.json` — wyłączenia oraz ignorowani gracze, zapisywane według UUID, bez kasowania po restarcie.
- Nie można pisać do siebie; tekst wiadomości jest zwykłym tekstem (nie interpretuje `&` ani MiniMessage), aby uniemożliwić fałszowanie kolorów systemowych.
- Globalne wyciszenie na czacie blokuje również wysyłanie MSG, z wyjątkiem posiadaczy jawnego `tools.chat.bypass.mute`.

**Ważne:** `/msg wycisz` to prywatne ignorowanie użytkownika, a `/chat wycisz` jest wyciszeniem nadawania na poziomie serwera. Pamiętaj o pełnym restarcie po wymianie JAR i wykonaniu kopii istniejących JSON-ów.

**Testy:** `mvn clean verify` w GitHub Actions (Java 25, MariaDB, SQLite, JUnit). Działanie interfejsu komend i rzeczywistej wysyłki należy jeszcze potwierdzić z dwoma klientami na działającym serwerze Paper.

---

# Tools 1.9.2 — korekta uprawnień teleportacji i ochrony

## Co zostało poprawione

- **Teleportacja natychmiastowa:** sama nadana ranga (VIP, Premium, Moderator)
  nie przyspiesza /spawn ani /lokalizacje. Wymagana jest wyraźna permisja
  `tools.lokalizacje.instant`, prawidłowe `*` lub rzeczywisty OP.
- **Ochrona regionów:** osoba z rzeczywistym uprawnieniem do zarządzania
  regionami (w Commands.json domyślnie `tools.region.admin`) może budować,
  niszczyć, otwierać skrzynie, korzystać z interakcji, zbierać przedmioty
  oraz wejść na obszary z ograniczeniem rang.
  **Nie przywrócono** komendy `/region bypass`.
- **Zwykli gracze:** nadal podlegają flagom regionów. Samo posiadanie
  `tools.lokalizacje` nie daje zarządzania obszarami.
- **Środowisko:** wybuchy, hoppery, tłoki, rozlewanie płynów i inne automatyczne
  efekty nadal podlegają flagom regionu — uprawnienie danego gracza nie odblokowuje ich globalnie.
- **Action bar:** lokalizacja, poziom i postęp są stale widoczne;
  krótkie ostrzeżenie o ochronie trafia na koniec, ma cooldown 0,8 s
  i nie zastępuje pozostałych danych. Ponowny /reload wysyła informację
  na czat zamiast nadpisywać action bar.
- **Zgodność:** stary tekst `protectedAction` w wygenerowanym JSON
  otrzyma automatycznie krótki wygląd. Własne edytowane komunikaty pozostają zachowane.

### Przykładowy test

Utwórz rangę `vip` bez `tools.lokalizacje.instant`, ustaw pozycję i nadaj ją
graczowi bez OP. Po `/spawn` ma wystąpić odliczanie.
Następnie nadaj dokładnie `tools.lokalizacje.instant` i sprawdź teleportację
natychmiastową. Gdy odbierzesz tę permisję w GUI rang i ponownie wywołasz
`/spawn`, odliczanie musi powrócić.

Na chronionym terenie zaloguj gracza bez OP z rangą zwykłą i spróbuj postawić
blok. Operacja powinna być zablokowana. Następnie nadaj **oddzielnej randze
moderatora** `tools.region.admin`: można budować i niszczyć, ale automatyczne
tłoki i wybuchy nadal są kontrolowane flagami.

### Bezpieczeństwo

Testy jednostkowe sprawdzają brak podwyższenia uprawnień przez samą nazwę
rangi, prawidłowe uprawnienie natychmiastowego teleportowania, wygasłe nadania,
wildcard oraz prawa administratora regionów. Przed podmianą pluginu na
publicznym serwerze wykonaj kopię bazy i plików JSON oraz przetestuj na
serwerze Paper. Offline-mode wymaga dodatkowego uwierzytelniania kont.

---

# Tools 1.9.1 — podpowiedzi uprawnień i stabilny action bar

### Bezpieczeństwo
- `/tools`, `/ranga`, `/region`, `/whitelist` i aliasy są ukrywane dla nieuprawnionych graczy.
- `/minecraft:whitelist`, `/bukkit:about`, `/bukkit:plugins`, `/bukkit:help` oraz pozostałe techniczne komendy z prefiksami `bukkit:`, `minecraft:` i `paper:` nie są wyświetlane w podpowiedziach użytkownikom bez dostępu administratora.
- Techniczne komendy administracyjne (`plugins`, `version`, `reload` itd.) są również blokowane przy ręcznym wpisaniu; wyświetla się krótki komunikat **Nieznana komenda.**
- Komendy nadal sprawdzają realne rangi Tools na etapie wykonania i sugestii, a nie tylko `sender.hasPermission`. `/spawn` i `/lokalizacje` pozostają publiczne.
- Uprawnienia nie należą do LuckPerms ani innego pluginu.

### Wspólny action bar
- Pasek statusu jest aktualizowany co 5 ticków z jednym źródłem (bez SQL w ticku): lokalizacja, aktualny poziom Minecraft i procent postępu XP.
- Ostrzeżenie **Obszar chroniony** jest dołączane po prawej stronie przez około 2,35 s. Krótkie komunikaty teleportacji pojawiają się w tym samym obszarze, bez usuwania lokalizacji.
- `/reload` wyświetla rezultat na czacie administratora. Nie zasłania paska wszystkich graczy i nie wymusza restartu Paper.
- Zmiany obsługiwanych komunikatów ochrony można edytować w `commands/region.json`, np. `messages.protectedAction`.

**Uwaga:** interfejs action bara współdzieli jeden kanał Minecrafta. Jeśli **inny plugin** stale wysyła swoje action bary, może on nadal nadpisywać wiadomości Tools. Ten patch synchronizuje nadawców należących do Tools.

Weryfikacja: `mvn clean verify` Java 25, testy MariaDB/SQLite, `CommandVisibilityPolicyTest` i `PlayerStatusBarTest`. Ręcznie sprawdź wejście bez OP na rzeczywistym Paper oraz zaktualizuj serwer pełnym restartem po podmianie pliku JAR.

---

# Tools 1.9.0 — naprawa uprawnień, regiony i jednolite GUI

**Bezpieczna aktualizacja:** wykonaj kopię `Ranks.json`, `Regions.json` i MySQL przed aktualizacją. Aktualizacja nie usuwa rang ani przypisań graczy.

## Uprawnienia

- Komendy administracyjne `/tools`, `/region`, `/ranga` i `/whitelist` oraz ich GUI korzystają z jednego weryfikatora `ToolsAccess`.
- Ranga `gracz` nie może nigdy automatycznie otrzymać `*`, uprawnień administratora, node'ów Bukkit/vanilla lub uprawnień obcych komend; nawet przypadkowy wpis w JSON jest pomijany. Nadal może używać publicznych `tools.spawn` i `tools.lokalizacje`.
- `*` zachowuje dawną semantykę pełnych uprawnień + OP, **ale tylko w jawnie nadanej randze innej niż podstawowa Gracz**. Przy cofnięciu rangi przywracamy wcześniejszy OP przed ponownym założeniem uprawnień.
- Zwykłe `Player.hasPermission()` nie stanowi samodzielnej autoryzacji komend administracyjnych — sprawdzany jest rzeczywisty stan rangi, a nie tylko node Paper.
- `/region bypass` **zostało całkowicie usunięte**. OP i ranki `*` podlegają flagom ochrony regionów tak samo jak pozostali. Regiony nadal konfiguruje się przez `/region edytuj`.
- `/region stan` wyświetla liczbę regionów i ich flagi, bez możliwości ominięcia ich.

## Wiadomości, dźwięki i GUI

- Powodzenie komendy: miętowy znacznik ✔ i cichy, wysoki dźwięk doświadczenia.
- Błąd/składnia: czerwony znacznik ✘ i cichy, niski dźwięk wieśniaka.
- Dźwięki mają ograniczenie częstotliwości i emitowane są wyłącznie na wątku Paper.
- GUI własnego Tools (rangi, regiony, lokalizacje, whitelist) mają ciemne obramowania, złote nagłówki, turkusowe nawigacje, szare opisy i jasne komunikaty akcji.
- Edycja tekstów pozostaje w `plugins/Tools/commands/*.json`; kolorystyka obsługuje `&#RRGGBB` i `&`.

**Ważne:** Tools stylizuje wyłącznie swoje GUI; nie zmienia GUI i itemów utworzonych przez inne wtyczki ani tekstur klienta Minecraft. W offline-mode sam nick nie jest wiarygodną tożsamością, nawet po włączeniu poprawnych uprawnień.

### Kontrola po wdrożeniu

1. Zaloguj zwykłego gracza bez OP, bez jawnej rangi i upewnij się, że `/region`, `/ranga`, `/tools`, `/whitelist` nie działają.
2. Nadaj uprawnienie administratora **osobnej** randze testowej, przypisz ją drugiemu kontu i potwierdź dostęp.
3. Wyłącz OP drugiemu kontu, odbierz rangę i sprawdź, że komendy znów są zablokowane.
4. Spróbuj postawić/zniszczyć blok na chronionym obszarze jako zwykły gracz i jako OP — obaj podlegają tym samym flagom.
5. Wpisz `/region stan`; potem `/ranga menu` i sprawdź nowe kolory oraz dźwięki.

---

# Tools 1.8.1 — naprawa ochrony i teksty komend

**Najważniejsza poprawka:** operatorzy (OP), ranga z `*` i gracze z
`tools.region.bypass` NIE omijają już zabezpieczeń regionu automatycznie.
Administrator włącza/wyłącza świadomie `/region bypass`. Tryb zeruje się po
wyjściu z serwera. `/region stan` pokazuje status załadowania, liczbę regionów,
aktywny region, omijanie oraz ustawienia budowania i niszczenia.

`/ranga` pokazuje teraz tekstową pomoc. Panel jest dostępny osobno pod
`/ranga menu`.

## Oddzielne JSON-y dla każdej komendy

Po pierwszym uruchomieniu powstaje:

```text
plugins/Tools/
├── Commands.json       (włączanie komend, aliasy i uprawnienia)
└── commands/
    ├── region.json
    ├── ranga.json
    ├── tools.json
    ├── lokalizacje.json
    ├── spawn.json
    └── whitelist.json
```

**Wszystkie wartości domyślne zdefiniowane są w Javie**, nie w plugin.yml
ani w `src/main/resources`. Każdy plik ma:

- `title`: tytuł pomocy;
- `help`: kolejne wiersze pomocy; można zmienić kolor, tekst, kolejność i układ;
- `messages`: nazwane szablony (np. `notReady`, `protectedAction`);
- `replacements`: stałe fragmenty wszystkich komunikatów wysyłanych przez
  odpowiednią klasę komendy i jej listenery, również przy wynikach async.
  Klucz to oryginalny fragment, wartość to zastępujący go tekst.

W tekstach dostępne są kolory Minecraft `&a`, `&l` i HEX `&#79D6C1`.
Dla przykładu fragment w `commands/region.json`:

```json
{
  "messages": {
    "protectedAction": "&#FF7285Ten teren jest chroniony!"
  },
  "replacements": {
    "Nie masz dostępu do edycji regionu.": "&#FF7285Nie możesz edytować tego regionu."
  }
}
```

**To przykład dwóch pól, nie cała zawartość pliku.** Edytuj plik wygenerowany
przez plugin, nie zastępuj go tym przykładem. Pozostawiaj nazwy kluczy, modyfikuj
wartości. Jeśli komunikat zawiera zmienne (np. nick lub nazwę rangi), możesz
zmienić jego stałe fragmenty — dane dynamiczne zostają bez zmian.

Po edycji wpisz `/reload` lub `/tools przeladuj`. Nie następuje restart
Paper ani wszystkich wtyczek. Nie wolno zmieniać połączenia MySQL ani
definicji regionów przez hot-reload.

### Kontrola ochrony regionów

1. Zrób kopię zapasową `plugins/Tools/Regions.json`.
2. Stań w chronionym regionie i wpisz `/region stan`.
3. Sprawdź, czy region jest rozpoznawany i `Omijanie ochrony: WYŁĄCZONE`.
4. Spróbuj zniszczyć i postawić blok również jako OP. Domyślnie będzie to
   zablokowane (chyba że świadomie włączyłeś odpowiednią flagę).
5. Jeżeli trzeba przetestować bez blokad, wpisz `/region bypass`.
   Wpisz ponownie, aby przywrócić ochronę.
6. Jeśli `/region stan` pokazuje zero regionów lub obszar poza regionem,
   sprawdź `Regions.json`, właściwy świat oraz log rozruchu.

Stare `Regions.json`, dane MySQL i istniejące rangi pozostają zachowane.
Przed testowaniem zrób kopię zapasową bazy i konfiguracji.

---

# Tools 1.8.0 — whitelist, spawn, skórki i zabezpieczenia

**Wszystkie uprawnienia, rangi i ich zapis obsługuje sam Tools.** Wtyczka nie używa LuckPerms ani innych pluginów jako zależności.

## Whitelist (autorska)

Domyślnie wyłączona; konfiguracja `plugins/Tools/Whitelist.json` jest tworzona podczas startu z klasy Java. Dozwolone nicki przechowywane są małymi literami, niezależnie od wielkości znaków w komendzie. Własne wiadomości każdego trybu można edytować w JSON. Każda zmiana z komendy/GUI jest zapisywana **atomowo i poza głównym wątkiem**.

| Polecenie | Działanie |
| --- | --- |
| `/whitelist` | Minimalistyczny panel |
| `/whitelist włącz prace_techniczne` | Prace techniczne |
| `/whitelist włącz chwilowa_przerwa` | Krótka przerwa |
| `/whitelist włącz nowa_edycja` | Start nowej edycji |
| `/whitelist włącz aktualizacja` | Aktualizacja |
| `/whitelist wyłącz` | Wyłącza whitelistę |
| `/whitelist dodaj LokosPL` | Dodaje nick |
| `/whitelist usuń LokosPL` | Usuwa nick |
| `/whitelist lista` | Główki graczy w GUI; kliknięcie usuwa wpis |

Komenda `/whitelist` przechwytuje też wariant wbudowany vanilla. Administracja wymaga `tools.whitelist.admin` (OP domyślnie; można nadać w GUI rang Tools). Przy włączeniu whitelisty klikający administrator lub gracz uruchamiający komendę jest automatycznie dopisywany, żeby sam siebie nie wyrzucił. Wszyscy pozostali nieuprawnieni gracze otrzymują kick z tekstem trybu; nowe połączenia są odrzucane w `AsyncPlayerPreLoginEvent`.

**Bezpieczeństwo offline-mode:** sama nazwa konta nie zapewnia uwierzytelnienia! Każdy może podać nick innego użytkownika, także administratora. Whitelist po nicku oraz pobieranie skinów to NIE zabezpieczenie przed podszyciem. Przed publicznym otwarciem offline-mode potrzebne jest własne uwierzytelnianie graczy (i zabezpieczenie panelu admina). Nie dodawaj uprzywilejowanych nicków na whitelistę i nie polegaj tylko na OP, jeśli połączenia są nieautoryzowane.

## Lokalizacje, /spawn i interfejs

- `/spawn`: teleportacja do głównej lokalizacji serwera ustawionej w `/region spawn`, z tym samym odliczaniem, kontrolą rang i `teleportAsync` co lokalizacje.
- `/lokalizacje`: na pierwszym ekranie tylko główne lokalizacje. Kliknięcie głównej lokalizacji otwiera drugi ekran z głównym teleportem i podlokalizacjami. Nie pokazujemy rozmiaru regionu ani nazwy świata.
- Komunikaty używają `&` i `&#RRGGBB`, szare nicki i minimalne opisy.

## Skórki premium i antybot

W `plugins/Tools/Security.json` generowanym z Java są przełączniki `premiumSkins` i `antiBot`. Oba są domyślnie włączone.

Skórki kont premium o takiej samej nazwie pobierane są **asynchronicznie** z usług profili Minecraft, z cache 6 godzin, timeoutem 4 sekundy i ograniczeniem jednoczesnych odpytań. Przy niedostępności usług gracz zostaje z obecną skórką. Skórki są jedynie kosmetyczne; nie zmieniają UUID ani autoryzacji. Skórki w główkach GUI mogą pozostać domyślne, jeśli nie są jeszcze dostępne w profilach serwera.

Lekki antybot ogranicza liczbę prób logowania z jednego IP, nie wykorzystuje SQL/HTTP w zdarzeniu prelogin i przepuszcza wcześniej znane pary nick/adres IP oraz nicki na whiteliście. **Nie chroni przed atakami DDoS ani profesjonalnym botnetem** — ograniczanie ruchu na proxy/firewallu pozostaje konieczne.

## Reload i zamknięcie

- `/reload` przechwytuje komendę Paper/vanilla i wykonuje `/tools przeladuj` + ponowny odczyt whitelisty. Nie wywołuje niebezpiecznego globalnego restartu pluginów.
- Żeby nie uszkodzić sesji, zmiana połączenia SQL, aliasów i definicji regionów nadal wymaga restartu całego serwera. Zmiany stylów TAB/chatu i komunikatów są bezpiecznie przeładowywane.
- W `Whitelist.json` można zmienić komunikat `shutdownMessage` i `reloadMessage`. Przy planowym wyłączeniu pluginu gracze otrzymują powód wyłączenia. Przy bezpiecznym hot-reloadzie gracze nie są wyrzucani.

**Testowanie:** `mvn clean verify` (Java 25). Po instalacji sprawdź `/whitelist`, `/spawn`, `/reload`, `/tools zdrowie` i zachowanie skórek na serwerze testowym.

---

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
