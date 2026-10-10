package pl.lokos.tools.items;

import java.time.LocalDate;
import java.util.*;

/**
 * Definicje generowane do SpecialItems.json. UUID graczy i istniejące
 * ItemStacki nie są modyfikowane podczas zmiany konfiguracji.
 */
public final class SpecialItemsConfig {
    public static final class Definition {
        private boolean enabled=true;
        private String material="DIAMOND_BOOTS";
        private String name="&#70D6E8&lBUTY SZYBKOŚCI";
        private String subtitle="&#FFD166&l✦ LEGENDARNE BUTY";
        private String description="&#A8A8B7Unikatowy przedmiot eventowy";
        private String eventName="EVENT TESTOWY";
        private String eventStart="2026-10-10";
        private String eventEnd="2027-01-10";
        private int unbreaking=3;
        private int speedLevel=2;
        private int jumpLevel=1;
        private int customModelData=0;
        private List<String> lore=new ArrayList<>(List.of(
                "&#A8A8B7━━━━━━━━━━━━━━━━━━━━",
                "&#FFD166✦ {subtitle}",
                "&#A8A8B7{description}",
                "",
                "&#A8A8B7» &#FF727FOCHRONA: 3",
                "&#A8A8B7» &#70D6E8Szybkość: +{speed}",
                "&#A8A8B7» &#70D6E8Wysokość skoku: +{jump}",
                "",
                "&#FFD166✦ PRZEDMIOT EVENTOWY",
                "&#A8A8B7» Początek: &#FFD166{start}",
                "&#A8A8B7» Zakończenie: &#FFD166{end}",
                "&#A8A8B7» Event: &#70D6E8{event}",
                "&#A8A8B7━━━━━━━━━━━━━━━━━━━━"
        ));
        public boolean enabled(){return enabled;}
        public String material(){return material;}
        public String name(){return name;}
        public String subtitle(){return subtitle;}
        public String description(){return description;}
        public String eventName(){return eventName;}
        public String eventStart(){return eventStart;}
        public String eventEnd(){return eventEnd;}
        public int unbreaking(){return unbreaking;}
        public int speedLevel(){return speedLevel;}
        public int jumpLevel(){return jumpLevel;}
        public int customModelData(){return customModelData;}
        public List<String> lore(){return List.copyOf(lore);}
        public void validate(){
            if(material==null || !material.matches("[A-Z_]{3,50}")||
                    name==null || name.isBlank() || name.length()>150 ||
                    subtitle==null || subtitle.length()>150 ||
                    description==null || description.length()>250 ||
                    eventName==null || eventName.length()>120 ||
                    eventStart==null || eventEnd==null ||
                    unbreaking<0 || unbreaking>10 ||
                    speedLevel<0 || speedLevel>5 || jumpLevel<0 || jumpLevel>5 ||
                    customModelData<0 || lore==null || lore.size()>35)
                throw new IllegalArgumentException("Nieprawidłowa definicja przedmiotu specjalnego.");
            try {
                LocalDate start=LocalDate.parse(eventStart),end=LocalDate.parse(eventEnd);
                if(end.isBefore(start))throw new IllegalArgumentException("Koniec eventu musi być po rozpoczęciu.");
            }catch(java.time.format.DateTimeParseException error){
                throw new IllegalArgumentException("Daty eventu: RRRR-MM-DD.",error);
            }
            for(String line:lore)
                if(line==null || line.length()>350)
                    throw new IllegalArgumentException("Błędna linia opisu przedmiotu.");
        }
    }

    private Map<String,Definition> items=new LinkedHashMap<>(Map.of("buty_szybkosci",new Definition()));
    public Map<String,Definition> items(){return Collections.unmodifiableMap(items);}
    public Definition get(String key){return items.get(key);}
    public void validate(){
        if(items==null||items.isEmpty()||items.size()>100)
            throw new IllegalArgumentException("SpecialItems.json: 1-100 definicji.");
        for(var item:items.entrySet()){
            if(item.getKey()==null||!item.getKey().matches("[a-z0-9_-]{1,48}")||item.getValue()==null)
                throw new IllegalArgumentException("SpecialItems.json: błędny identyfikator.");
            item.getValue().validate();
        }
    }
    public static String format(String template,Definition d){
        return template.replace("{subtitle}",d.subtitle()).replace("{description}",d.description())
                .replace("{start}",d.eventStart()).replace("{end}",d.eventEnd())
                .replace("{event}",d.eventName())
                .replace("{speed}",String.valueOf(d.speedLevel()))
                .replace("{jump}",String.valueOf(d.jumpLevel()));
    }
}
