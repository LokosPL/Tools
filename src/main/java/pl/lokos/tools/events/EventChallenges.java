package pl.lokos.tools.events;

import java.util.List;

/** Trzy unikalne wyzwania na każdy event, resetowane przy nowej edycji. */
public final class EventChallenges {
    private EventChallenges(){}
    public static int[] goals(EventType type){
        return switch(type){
            case ZIMA -> new int[]{12,36,80};
            case HALLOWEEN -> new int[]{5,20,50};
            case WIELKANOC -> new int[]{8,24,60};
            case LATO -> new int[]{3,10,25};
            case ZABOJSTWA -> new int[]{3,8,18};
            case METEORY -> new int[]{1,3,6};
            case ZNIWA -> new int[]{16,48,96};
            case WEDKOWANIE -> new int[]{5,15,40};
        };
    }
    public static String action(EventType type){
        return switch(type){
            case ZIMA -> "kopanie naturalnych bloków";
            case HALLOWEEN -> "pokonywanie wrogich potworów";
            case WIELKANOC -> "zbieranie dzikich kwiatów";
            case LATO -> "udane połowy";
            case ZABOJSTWA -> "uczciwe eliminacje PvP";
            case METEORY -> "odnalezione meteoryty";
            case ZNIWA -> "zebrane dojrzałe uprawy";
            case WEDKOWANIE -> "złowione ryby";
        };
    }
    public static List<String> names(EventType type){
        return switch(type){
            case ZIMA -> List.of("Śnieżny zwiadowca","Łowca lodowych skarbów","Legenda zimy");
            case HALLOWEEN -> List.of("Pogromca cieni","Nocny łowca","Król mroku");
            case WIELKANOC -> List.of("Poszukiwacz pisanek","Kwiatowy odkrywca","Mistrz wiosny");
            case LATO -> List.of("Wędkarz lata","Morski podróżnik","Słoneczny mistrz");
            case ZABOJSTWA -> List.of("Początkujący łowca","Weteran areny","Mistrz pojedynków");
            case METEORY -> List.of("Odkrywca kraterów","Łowca gwiezdnego pyłu","Pogromca meteorytów");
            case ZNIWA -> List.of("Pomocnik rolnika","Mistrz plonów","Złoty żniwiarz");
            case WEDKOWANIE -> List.of("Łowca fal","Władca głębin","Legendarny wędkarz");
        };
    }
}
