package pl.lokos.tools.crates;

import java.util.*;

/** Skrzynie są definiowane współrzędnymi bloków, nie inventarzami Bukkit. */
public record CratesState(List<Position> crates,Map<String,Integer> afkMinutes) {
    public record Position(String world,int x,int y,int z,String type){
        public CrateType kind(){return CrateType.parse(type);}
        public String key(){return world+":"+x+":"+y+":"+z;}
        public void validate(){
            if(world==null||kind()==null||Math.abs(x)>30000000||Math.abs(z)>30000000||
                    y < -2048||y>4096)
                throw new IllegalArgumentException("Nieprawidłowa pozycja skrzyni.");
            try{UUID.fromString(world);}
            catch(IllegalArgumentException e){throw new IllegalArgumentException("Nieznany UUID świata.",e);}
        }
    }
    public CratesState(){this(List.of(),Map.of());}
    public CratesState{
        crates=crates==null?List.of():List.copyOf(crates);
        afkMinutes=afkMinutes==null?Map.of():Map.copyOf(afkMinutes);
    }
    public Position get(String key){
        for(Position p:crates)if(p.key().equals(key))return p;
        return null;
    }
    public CratesState with(Position position){
        if(get(position.key())!=null)throw new IllegalArgumentException("Skrzynia już istnieje.");
        var next=new ArrayList<>(crates);next.add(position);
        return new CratesState(next,afkMinutes);
    }
    public CratesState without(String key){
        if(get(key)==null)throw new IllegalArgumentException("W tym miejscu nie ma skrzyni.");
        return new CratesState(crates.stream().filter(p->!p.key().equals(key)).toList(),afkMinutes);
    }
    public CratesState withAfk(Map<String,Integer> progress){return new CratesState(crates,progress);}
    public void validate(){
        if(afkMinutes.size()>100000)throw new IllegalArgumentException("Zbyt wiele liczników AFK.");
        for(var entry:afkMinutes.entrySet()){
            try{UUID.fromString(entry.getKey());}
            catch(IllegalArgumentException ex){throw new IllegalArgumentException("Błędny UUID AFK.",ex);}
            if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>2000)
                throw new IllegalArgumentException("Błędny czas AFK.");
        }
        if(crates.size()>500)throw new IllegalArgumentException("Zbyt wiele skrzyń.");
        var unique=new HashSet<String>();
        for(Position p:crates){p.validate();if(!unique.add(p.key()))throw new IllegalArgumentException("Duplikat skrzyni.");}
    }
}
