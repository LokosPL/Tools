package pl.lokos.tools.crates;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ostatnie potwierdzone działanie gracza. Nie przechowuje lokacji ani Bukkit API,
 * dzięki czemu odczyt licznika i jego testy nie wymagają uruchomienia serwera.
 * Dostęp wyłącznie z głównego wątku Paper.
 */
public final class CrateActivityWindow {
    private static final long MINUTE_MILLIS=60_000L;
    private final Map<UUID,Long> lastActivity=new HashMap<>();

    public void record(UUID player,long timestamp){
        lastActivity.put(player,timestamp);
    }

    public boolean recentlyActive(UUID player,long now,int windowMinutes){
        if(windowMinutes<1||windowMinutes>60)
            throw new IllegalArgumentException("Okno aktywności musi mieć 1-60 minut.");
        Long last=lastActivity.get(player);
        return last!=null && now>=last && now-last<windowMinutes*MINUTE_MILLIS;
    }

    public void remove(UUID player){lastActivity.remove(player);}
    public void clear(){lastActivity.clear();}
}
