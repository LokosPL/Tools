package pl.lokos.tools.events;

import java.util.*;

/** Stan eventu z trwałą pamięcią par PvP; zgodność ze starszym EventState.json. */
public record EventState(String type,long startedAt,long endsAt,
                         Map<String,Integer> progress,Map<String,Long> pvpCooldowns) {
    private static final int MAX_PVP_PAIRS=5000;

    public EventState(){this(null,0,0,Map.of(),Map.of());}
    /** Konstruktor zgodny ze starszym API czteropolowym. */
    public EventState(String type,long startedAt,long endsAt,Map<String,Integer> progress){
        this(type,startedAt,endsAt,progress,Map.of());
    }
    public EventState {
        progress=progress==null?Map.of():Map.copyOf(progress);
        pvpCooldowns=pvpCooldowns==null?Map.of():Map.copyOf(pvpCooldowns);
    }
    public EventType active(long now){
        EventType kind=EventType.parse(type);
        return kind!=null && endsAt>now && startedAt>0?kind:null;
    }
    public EventState started(EventType next,long start,long finish){
        return new EventState(next.id(),start,finish,Map.of(),Map.of());
    }
    public EventState ended(){return new EventState();}
    public EventState progress(UUID uuid,int count){
        var copy=new HashMap<>(progress);
        if(count<=0)copy.remove(uuid.toString());
        else copy.put(uuid.toString(),count);
        return new EventState(type,startedAt,endsAt,copy,pvpCooldowns);
    }
    public int progress(UUID uuid){return progress.getOrDefault(uuid.toString(),0);}

    private static String pair(UUID killer,UUID victim){
        return killer+":"+victim;
    }
    public boolean canRewardPvPKill(UUID killer,UUID victim,long now,long intervalMillis){
        if(killer==null||victim==null||killer.equals(victim)||intervalMillis<1)return false;
        Long previous=pvpCooldowns.get(pair(killer,victim));
        return previous==null || now>=previous && now-previous>=intervalMillis;
    }
    public EventState withPvPKill(UUID killer,UUID victim,long now,long intervalMillis){
        if(!canRewardPvPKill(killer,victim,now,intervalMillis))
            throw new IllegalArgumentException("Ponowne zabójstwo zablokowane przez antyfarm.");
        Map<String,Long> next=new HashMap<>(pvpCooldowns);
        next.entrySet().removeIf(entry->now>=entry.getValue()
                && now-entry.getValue()>=intervalMillis);
        // Limit pamięci/dysku nawet przy wielu unikatowych parach.
        if(next.size()>=MAX_PVP_PAIRS){
            String oldest=next.entrySet().stream()
                    .min(Map.Entry.comparingByValue()).orElseThrow().getKey();
            next.remove(oldest);
        }
        next.put(pair(killer,victim),now);
        return new EventState(type,startedAt,endsAt,progress,next);
    }
    public void validate(){
        if(type!=null&&EventType.parse(type)==null)throw new IllegalArgumentException("Nieznany event.");
        if(startedAt<0||endsAt<0||progress.size()>100000||pvpCooldowns.size()>MAX_PVP_PAIRS)
            throw new IllegalArgumentException("Błędne dane eventu.");
        if(type!=null&&(startedAt==0||endsAt<=startedAt))
            throw new IllegalArgumentException("Błędny termin eventu.");
        for(var entry:progress.entrySet()){
            try{UUID.fromString(entry.getKey());}
            catch(IllegalArgumentException ex){throw new IllegalArgumentException("Błędny UUID.",ex);}
            if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>1000000)
                throw new IllegalArgumentException("Błędny postęp.");
        }
        for(var entry:pvpCooldowns.entrySet()){
            if(entry.getKey()==null||entry.getValue()==null||entry.getValue()<0)
                throw new IllegalArgumentException("Błędna para PvP.");
            String[] ids=entry.getKey().split(":",-1);
            if(ids.length!=2)throw new IllegalArgumentException("Błędny format pary PvP.");
            try{UUID.fromString(ids[0]);UUID.fromString(ids[1]);}
            catch(IllegalArgumentException ex){throw new IllegalArgumentException("Błędny UUID pary PvP.",ex);}
        }
    }
}
