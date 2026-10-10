package pl.lokos.tools.staff;

import org.bukkit.GameMode;
import java.util.Locale;

/** Funkcje bez efektów ubocznych – testowane bez serwera. */
public final class StaffParsers {
    private StaffParsers(){}
    /** Kolejność oczekiwana przez Tools: 1 survival, 2 creative, 3 adventure, 4 spectator. */
    public static GameMode gameMode(String text){
        return switch(text.toLowerCase(Locale.ROOT)){
            case "1","survival","s" -> GameMode.SURVIVAL;
            case "2","creative","c" -> GameMode.CREATIVE;
            case "3","adventure","a" -> GameMode.ADVENTURE;
            case "4","spectator","sp" -> GameMode.SPECTATOR;
            default -> throw new IllegalArgumentException("Tryb: 1 survival, 2 creative, 3 adventure, 4 spectator.");
        };
    }
    public static double coordinate(String value,double base,int bound) {
        try{
            double result=value.startsWith("~")?
                    base+(value.length()==1?0:Double.parseDouble(value.substring(1))):
                    Double.parseDouble(value);
            if(!Double.isFinite(result)||Math.abs(result)>bound)
                throw new IllegalArgumentException("Koordynaty są poza dozwolonym zakresem.");
            return result;
        }catch(NumberFormatException error){throw new IllegalArgumentException("Niepoprawna koordynata: "+value);}
    }
    public static int duration(String input,int maxSeconds){
        if(input==null||!input.matches("[1-9][0-9]{0,6}[smhd]"))
            throw new IllegalArgumentException("Czas: np. 30s, 5m, 2h lub 1d.");
        int value=Integer.parseInt(input.substring(0,input.length()-1));
        long seconds=(long)value*switch(input.charAt(input.length()-1)){
            case 's'->1L;case 'm'->60L;case 'h'->3600L;default->86400L;
        };
        if(seconds>maxSeconds)throw new IllegalArgumentException("Przekroczono maksymalny czas ogłoszenia.");
        return (int)seconds;
    }
    public static float speed(String value){
        try{
            int level=Integer.parseInt(value);
            if(level<1||level>10)throw new IllegalArgumentException("Prędkość: od 1 do 10.");
            return level/10f;
        }catch(NumberFormatException error){throw new IllegalArgumentException("Prędkość: od 1 do 10.");}
    }
}
