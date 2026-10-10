# Granica świata Tools 1.13.0-SNAPSHOT

**Status: zaimplementowana, domyślnie aktywna** poprzez `WorldBorder.json`
i `WorldBorderState.json`. Nie jest to już wyłącznie projekt.

- Start: 1500×1500 bloków, **środek na głównym spawnie regionów** (jeśli
  nie został ustawiony, na spawnie Overworld).
- Bez pomniejszania istniejących działek: podczas pierwszego uruchomienia
  Tools liczy zakres wszystkich zapisanych regionów w tym świecie i
  automatycznie powiększa granicę, gdy byłoby to konieczne.
- Nie nadpisuje istniejącej, niestandardowo większej granicy,
  ale zastępuje zwykłe domyślne 60 mln bloków.
- Automatyczny krok +1000 średnicy po 7 dniach i 120 godzinach
  aktywności graczy; po 14 dniach awaryjnie +500 średnicy.
- AFK po pięciu minutach bez ruchu, dzienny limit 4 godzin na UUID,
  stan zapisany także po restarcie. Maksymalny rozmiar 15000×15000.
- Nowe regiony za granicą są odrzucane. Ochrona regionów zachowuje
  priorytety i flagi.
- Bossbar pokazuje postęp i termin, za aktywnym eventem,
  walką i ogłoszeniami administracyjnymi.

## Administracja

`/granica status`, `/granica pauza`, `/granica wznow`,
`/granica rozbuduj 500`. Perk `tools.granica.admin`.

**Ograniczenia:** automatyczna pregeneracja chunków i blokowanie
nadmiernego zwiedzania nowych chunków wymaga kolejnej iteracji.
Zmiany przetestowano przez JUnit i CI, nie na realnym świecie Paper.
Przed pierwszym włączeniem wykonaj kopię mapy i sprawdź, czy wszystkie
obecne regiony znajdują się wewnątrz nowej granicy.
