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
        private Map<String,Integer> enchantments=new LinkedHashMap<>();
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
        public Definition(){}
        private Definition(String material,String name,String event,String description,
                           int speed,int jump){
            this.material=material;
            this.name=name;
            this.subtitle="&#FFD166✦ PRZEDMIOT LEGENDARNY";
            this.description=description;
            this.eventName=event;
            this.eventStart="2026-01-01";
            this.eventEnd="2028-12-31";
            this.speedLevel=speed;
            this.jumpLevel=jump;
            this.lore=new ArrayList<>(List.of(
                    "&#A8A8B7━━━━━━━━━━━━━━━━━━━━",
                    "&#FFD166✦ {subtitle}",
                    "&#A8A8B7{description}",
                    "",
                    "&#70D6E8» Wytrzymałość: III",
                    speed>0?"&#70D6E8» Szybkość: {speed}":"",
                    jump>0?"&#70D6E8» Wysokość skoku: {jump}":"",
                    "",
                    "&#FFD166✦ NAGRODA EVENTOWA",
                    "&#A8A8B7» Event: &#70D6E8{event}",
                    "&#A8A8B7━━━━━━━━━━━━━━━━━━━━"));
        }
        private Definition enchants(Map<String,Integer> values){
            enchantments=new LinkedHashMap<>(values);
            return this;
        }
        public Map<String,Integer> enchantments(){return Map.copyOf(enchantments);}
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
                    customModelData<0 || lore==null || lore.size()>35 ||
                    enchantments==null || enchantments.size()>16)
                throw new IllegalArgumentException("Nieprawidłowa definicja przedmiotu specjalnego.");
            try {
                LocalDate start=LocalDate.parse(eventStart),end=LocalDate.parse(eventEnd);
                if(end.isBefore(start))throw new IllegalArgumentException("Koniec eventu musi być po rozpoczęciu.");
            }catch(java.time.format.DateTimeParseException error){
                throw new IllegalArgumentException("Daty eventu: RRRR-MM-DD.",error);
            }
            for(var enchant:enchantments.entrySet())
                if(enchant.getKey()==null||!enchant.getKey().matches("[a-z_]{2,40}")||
                        enchant.getValue()==null||enchant.getValue()<1||enchant.getValue()>10)
                    throw new IllegalArgumentException("Niepoprawne zaklęcie przedmiotu.");
            for(String line:lore)
                if(line==null || line.length()>350)
                    throw new IllegalArgumentException("Błędna linia opisu przedmiotu.");
        }
    }

    private static Map<String,Definition> defaults(){
        Map<String,Definition> value=new LinkedHashMap<>();
        value.put("buty_szybkosci",new Definition());
        value.put("zimowe_ostrze",new Definition("DIAMOND_SWORD",
                "&#70D6E8&l✦ MIECZ ZAMIECI","Zimowy Festiwal",
                "Ostrze wykute z lodowego kryształu",0,0).enchants(Map.of("sharpness",4,"knockback",1)));
        value.put("miecz_duchow",new Definition("NETHERITE_SWORD",
                "&#FF727F&l✦ OSTRZE DUCHÓW","Noc Duchów",
                "Broń poszukiwacza nocnych zjaw",0,0).enchants(Map.of("sharpness",4,"looting",2)));
        value.put("buty_zajaczka",new Definition("DIAMOND_BOOTS",
                "&#70D6E8&l✦ BUTY ZAJĄCZKA","Zajączkowe Poszukiwania",
                "Lekkie buty skocznego podróżnika",1,3).enchants(Map.of("feather_falling",4)));
        value.put("wedka_sloneczna",new Definition("FISHING_ROD",
                "&#FFD166&l✦ SŁONECZNA WĘDKA","Letnie Łowy",
                "Pamiątka letniego połowu",0,0).enchants(Map.of("luck_of_the_sea",3,"lure",2)));
        value.put("ostrze_lowcy",new Definition("NETHERITE_AXE",
                "&#FF727F&l✦ TOPÓR ŁOWCY","Arena Łowców",
                "Trofeum uczciwych pojedynków",0,0).enchants(Map.of("sharpness",5)));
        value.put("kilof_meteorytu",new Definition("NETHERITE_PICKAXE",
                "&#FFD166&l✦ KILOF METEORYTU","Deszcz Meteorów",
                "Kilof z kosmicznego odłamka",0,0).enchants(Map.of("efficiency",5,"fortune",3)));
        value.put("sierp_urodzaju",new Definition("NETHERITE_HOE",
                "&#89E5B0&l✦ SIERP URODZAJU","Święto Plonów",
                "Narzędzie mistrza zbiorów",0,0).enchants(Map.of("efficiency",5)));
        value.put("wedka_oceanu",new Definition("FISHING_ROD",
                "&#70D6E8&l✦ WĘDKA OCEANU","Wielkie Wędkowanie",
                "Nagroda za wyjątkowy połów",0,0).enchants(Map.of("luck_of_the_sea",3,"lure",3)));
        return value;
    }
    private Map<String,Definition> items=defaults();
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
