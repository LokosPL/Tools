package pl.lokos.tools.crates;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Odliczanie pobytu w regionie AFK, liczone wyłącznie w sekundowych tickach
 * głównego wątku Paper. Wylogowanie, wyjście z regionu i restart zerują serię.
 * Zapisany licznik pełnych minut do klucza pozostaje w CratesState.
 */
public final class AfkStayTracker {
    private final Map<UUID,Long> seconds=new HashMap<>();

    /** Zwraca true na zakończenie każdej minuty ciągłego pobytu. */
    public boolean tick(UUID uuid){
        long next=seconds.getOrDefault(uuid,0L)+1;
        seconds.put(uuid,next);
        return next%60==0;
    }

    public long seconds(UUID uuid){return seconds.getOrDefault(uuid,0L);}
    public void leave(UUID uuid){seconds.remove(uuid);}
    public void clear(){seconds.clear();}

    /** Bonus zaczyna się od drugiego pełnego okresu; limit jest konfigurowalny. */
    public static int rewardKeys(long staySeconds,int intervalMinutes,
                                 int bonusEveryMinutes,int maxKeys){
        if(staySeconds<0||intervalMinutes<1||bonusEveryMinutes<1||maxKeys<1)
            throw new IllegalArgumentException("Niepoprawne ustawienia nagród AFK.");
        long stayMinutes=staySeconds/60;
        long extra=Math.max(0,stayMinutes-intervalMinutes)/bonusEveryMinutes;
        return (int)Math.min(maxKeys,1L+extra);
    }

    public static int remainingSeconds(int progressMinutes,long staySeconds,int intervalMinutes){
        if(progressMinutes<0||staySeconds<0||intervalMinutes<1)
            throw new IllegalArgumentException("Niepoprawny licznik AFK.");
        // Jeśli administrator skróci interwał w JSON, istniejący postęp
        // nie może wyświetlać dłuższego czasu niż realnie pozostały.
        int effective=Math.min(progressMinutes,intervalMinutes-1);
        long left=(long)(intervalMinutes-effective)*60-(staySeconds%60);
        return (int)Math.max(1,left);
    }

    public static String countdown(int seconds){
        if(seconds<0)throw new IllegalArgumentException("Ujemny czas AFK.");
        int hours=seconds/3600,minutes=(seconds%3600)/60,remaining=seconds%60;
        return hours>0
                ?String.format(Locale.ROOT,"%d:%02d:%02d",hours,minutes,remaining)
                :String.format(Locale.ROOT,"%02d:%02d",minutes,remaining);
    }
}
