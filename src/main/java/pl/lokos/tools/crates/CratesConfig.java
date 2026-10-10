package pl.lokos.tools.crates;

import java.util.*;

/** Ustawienia losowania; lista minecraft:ID lub specjalny identyfikator. */
public final class CratesConfig {
    private boolean enabled=true;
    private int maximumPlacedCrates=100;
    private int afkKeyMinutes=60;
    private double ordinaryKeyChanceFromHostileMob=0.02;
    private double specialKeyChanceFromHostileMob=0.0005;
    private boolean particles=true;
    private boolean holograms=true;
    private Map<String,Map<String,Integer>> pools=defaults();

    private static Map<String,Map<String,Integer>> defaults(){
        Map<String,Map<String,Integer>> map=new LinkedHashMap<>();
        map.put("zwykla",new LinkedHashMap<>(Map.of(
                "minecraft:iron_ingot",40,"minecraft:emerald",25,
                "minecraft:diamond",8,"buty_szybkosci",2)));
        map.put("premium",new LinkedHashMap<>(Map.of(
                "minecraft:diamond",40,"minecraft:netherite_scrap",12,
                "kilof_meteorytu",2,"miecz_duchow",3)));
        map.put("afk",new LinkedHashMap<>(Map.of(
                "minecraft:gold_ingot",25,"minecraft:experience_bottle",30,
                "minecraft:emerald",25,"wedka_oceanu",2)));
        map.put("eventowa",new LinkedHashMap<>(Map.of(
                "minecraft:diamond",15,"@event",20,"minecraft:emerald",40)));
        map.put("specjalna",new LinkedHashMap<>(Map.of(
                "minecraft:netherite_ingot",5,"ostrze_lowcy",5,
                "miecz_duchow",5,"minecraft:totem_of_undying",15)));
        return map;
    }
    public boolean enabled(){return enabled;}
    public int maximumPlacedCrates(){return maximumPlacedCrates;}
    public int afkKeyMinutes(){return afkKeyMinutes;}
    public double ordinaryKeyChanceFromHostileMob(){return ordinaryKeyChanceFromHostileMob;}
    public double specialKeyChanceFromHostileMob(){return specialKeyChanceFromHostileMob;}
    public boolean particles(){return particles;}
    public boolean holograms(){return holograms;}
    public Map<String,Integer> pool(CrateType type){return pools.getOrDefault(type.id(),Map.of());}
    public void validate(){
        if(maximumPlacedCrates<5||maximumPlacedCrates>500||
                afkKeyMinutes<10||afkKeyMinutes>1440||
                ordinaryKeyChanceFromHostileMob<0||ordinaryKeyChanceFromHostileMob>0.25||
                specialKeyChanceFromHostileMob<0||specialKeyChanceFromHostileMob>0.05||
                pools==null||pools.size()>20)
            throw new IllegalArgumentException("Niepoprawne Crates.json.");
        for(CrateType t:CrateType.values())if(!pools.containsKey(t.id()))
            throw new IllegalArgumentException("Brak tabeli dropu "+t.id());
        for(var p:pools.entrySet()){
            if(CrateType.parse(p.getKey())==null||p.getValue()==null||
                    p.getValue().isEmpty()||p.getValue().size()>50)
                throw new IllegalArgumentException("Niepoprawna tabela dropów.");
            for(var entry:p.getValue().entrySet())
                if(entry.getKey()==null||!entry.getKey().matches("[a-z0-9_:@-]{1,70}")||
                        entry.getValue()==null||entry.getValue()<1||entry.getValue()>10000)
                    throw new IllegalArgumentException("Niepoprawna szansa nagrody.");
        }
    }
    public static String choose(Map<String,Integer> pool,int index){
        int sum=pool.values().stream().mapToInt(Integer::intValue).sum();
        if(index<0||index>=sum)throw new IllegalArgumentException("Niepoprawny indeks losowania.");
        for(var entry:new TreeMap<>(pool).entrySet()){
            index-=entry.getValue();
            if(index<0)return entry.getKey();
        }
        throw new IllegalArgumentException("Pusta tabela dropu.");
    }
}
