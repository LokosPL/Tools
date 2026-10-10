package pl.lokos.tools.events;

import java.util.*;

/** Jeden identyfikator eventu, komend, dropu i klucza eventowego. */
public enum EventType {
    ZIMA("zima","Zimowy Festiwal","Zbieraj śnieżki przy kopaniu bloków","SNOWBALL","zimowe_ostrze"),
    HALLOWEEN("halloween","Noc Duchów","Pokonuj wrogie moby i zdobywaj dyniowe żetony","CARVED_PUMPKIN","miecz_duchow"),
    WIELKANOC("wielkanoc","Zajączkowe Poszukiwania","Zbieraj kwiaty i poszukuj wielkanocnych jaj","EGG","buty_zajaczka"),
    LATO("lato","Letnie Łowy","Łów ryby i zdobywaj letnie muszle","NAUTILUS_SHELL","wedka_sloneczna"),
    ZABOJSTWA("zabojstwa","Arena Łowców","Wygrywaj uczciwe pojedynki PvP","RED_DYE","ostrze_lowcy"),
    METEORY("meteory","Deszcz Meteorów","Odkrywaj spadające meteoryty i wydobywaj rudy","AMETHYST_SHARD","kilof_meteorytu"),
    ZNIWA("zniwa","Święto Plonów","Zbieraj dojrzałe uprawy","WHEAT","sierp_urodzaju"),
    WEDKOWANIE("wedkowanie","Wielkie Wędkowanie","Łów ryby i zbieraj eventowe perły","PRISMARINE_CRYSTALS","wedka_oceanu"),
    LOWY("lowy","Łowy na Tytanów","Znajduj i pokonuj elitarne potwory na dzikich terenach","NETHER_STAR","ostrze_lowcy");

    private final String id,title,description,tokenMaterial,itemId;
    EventType(String id,String title,String description,String tokenMaterial,String itemId){
        this.id=id;this.title=title;this.description=description;
        this.tokenMaterial=tokenMaterial;this.itemId=itemId;
    }
    public String id(){return id;}
    public String title(){return title;}
    public String description(){return description;}
    public String tokenMaterial(){return tokenMaterial;}
    public String itemId(){return itemId;}
    public static EventType parse(String value){
        if(value==null)return null;
        String key=value.toLowerCase(Locale.ROOT);
        for(EventType type:values())if(type.id.equals(key))return type;
        return null;
    }
    public static List<String> names(){return Arrays.stream(values()).map(EventType::id).toList();}
}
