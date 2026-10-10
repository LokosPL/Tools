# Koncepcja: rosnąca granica świata Tools Survival

**Status: projekt mechaniki, NIEAKTYWNY w bieżącym JAR.** Nie zmieniamy jeszcze granicy ani działek.

## Reguły proponowanego sezonu

- Start mapy: 3000 × 3000 bloków, maksymalny rozmiar 15000 × 15000.
- Cykl trwa 7 dni od startu sezonu; serwer zapamiętuje cykle niezależnie od restartów.
- Do odblokowania kolejnego obszaru potrzeba 120 aktywnych graczogodzin w cyklu.
- Co 7 dni po spełnieniu celu granica zwiększa promień o 500 bloków.
- Osoby AFK ponad 5 minut nie naliczają aktywności; maksymalnie 4 godziny na UUID na dobę.
- Zabezpieczenie dla małego serwera: po 14 dniach bez ekspansji automatyczny krok o 250 bloków promienia.
- Żaden wzrost nie niszczy istniejących działek, regionów ani magazynów.
- Nowy pierścień należy bezpiecznie pregenerować z ograniczeniem obciążenia; nie na głównym wątku.

## Bossbar

Proponowany wzór:

    ✦ NOWE TERENY │ za 3 dni 14 godz. │ Aktywność 76%

Paleta: złoty #FFD166, turkus #70D6E8, szary #A8A8B7, czerwony #FF727F.
Obecny /broadcast używa już bossbara. Przed implementacją granicy konieczny jest
**jeden koordynator pasków**. Ogłoszenie administracji ma priorytet nad postępem granicy.
Po wygaśnięciu ogłoszenia wraca informacja o następnym otwarciu terenu, bez pustych aktualizacji.

## Docelowa konfiguracja

WorldBorder.json: start, maksimum, krok, cel aktywności, limit AFK, teksty i kolory.
WorldBorderState.json: id sezonu, ostatni krok, statystyki graczy dziennie i następny termin.
Komendy administracyjne: /granica status, /granica harmonogram, /granica rozbuduj, /granica pauza.

Zapis kroku musi być atomowy i idempotentny (restart nie może ponowić tej samej ekspansji).
Przed uruchomieniem na produkcji potrzebne są testy granicy, pregeneracji, teleportacji
i działek na kopii świata.
