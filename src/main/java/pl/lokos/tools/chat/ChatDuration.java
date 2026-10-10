package pl.lokos.tools.chat;

import java.util.Locale;

/** Czas wyciszenia: 30s, 5m, 2h, 1d lub * (na stałe). */
public final class ChatDuration {
    private ChatDuration(){}
    public static long until(String value,long now) {
        if("*".equals(value))return 0;
        if(value==null || !value.toLowerCase(Locale.ROOT).matches("[1-9][0-9]{0,5}[smhd]"))
            throw new IllegalArgumentException("Podaj czas: 30s, 5m, 2h, 1d lub *.");
        char unit=Character.toLowerCase(value.charAt(value.length()-1));
        long count=Long.parseLong(value.substring(0,value.length()-1));
        long multiplier=switch(unit){case 's'->1000L;case 'm'->60000L;
            case 'h'->3600000L;default->86400000L;};
        long duration=count*multiplier;
        if(duration>365L*86400000L)throw new IllegalArgumentException("Maksymalny czas wyciszenia: 365 dni.");
        return now+duration;
    }
}
