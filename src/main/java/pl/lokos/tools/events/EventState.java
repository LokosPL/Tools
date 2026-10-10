package pl.lokos.tools.events;

import java.util.*;

/**
 * Utrwalony stan eventu: postęp żetonów, antyfarm PvP i niezależne wyzwania
 * z kolejką niewypłaconych kluczy. Starsze EventState.json bez nowych map
 * pozostają zgodne z Gson/JsonConfigManager.
 */
public record EventState(String type,long startedAt,long endsAt,
                         Map<String,Integer> progress,Map<String,Long> pvpCooldowns,
                         Map<String,Integer> challengePoints,
                         Map<String,Integer> challengeAwarded,
                         Map<String,Integer> pendingKeys) {
    private static final int MAX_PVP_PAIRS=5000;
    private static final int MAX_PLAYERS=100000;
    private static final int MAX_POINTS=1000000;
    private static final int MAX_PENDING=10000;
    public EventState(){this(null,0,0,Map.of(),Map.of(),Map.of(),Map.of(),Map.of());}
    /** Zgodność ze starymi testami i zapisami pięciopolowymi. */
    public EventState(String type,long startedAt,long endsAt,Map<String,Integer> progress,
                      Map<String,Long> pvpCooldowns){
        this(type,startedAt,endsAt,progress,pvpCooldowns,Map.of(),Map.of(),Map.of());
    }
    public EventState(String type,long startedAt,long endsAt,Map<String,Integer> progress){
        this(type,startedAt,endsAt,progress,Map.of());
    }
    public EventState {
        progress=progress==null?Map.of():Map.copyOf(progress);
        pvpCooldowns=pvpCooldowns==null?Map.of():Map.copyOf(pvpCooldowns);
        challengePoints=challengePoints==null?Map.of():Map.copyOf(challengePoints);
        challengeAwarded=challengeAwarded==null?Map.of():Map.copyOf(challengeAwarded);
        pendingKeys=pendingKeys==null?Map.of():Map.copyOf(pendingKeys);
    }
    public EventType active(long now){
        EventType kind=EventType.parse(type);
        return kind!=null && endsAt>now && startedAt>0?kind:null;
    }
    public EventState started(EventType next,long start,long finish){
        // Nieprzyznane klucze z wcześniejszego eventu nie mogą zaginąć.
        return new EventState(next.id(),start,finish,Map.of(),Map.of(),
                Map.of(),Map.of(),pendingKeys);
    }
    public EventState ended(){
        return new EventState(null,0,0,Map.of(),Map.of(),Map.of(),Map.of(),pendingKeys);
    }
    public EventState progress(UUID uuid,int count){
        var copy=new HashMap<>(progress);
        if(count<=0)copy.remove(uuid.toString());
        else copy.put(uuid.toString(),count);
        return new EventState(type,startedAt,endsAt,copy,pvpCooldowns,
                challengePoints,challengeAwarded,pendingKeys);
    }
    public int progress(UUID uuid){return progress.getOrDefault(uuid.toString(),0);}
    public int challengePoints(UUID uuid){return challengePoints.getOrDefault(uuid.toString(),0);}
    public int challengeAwarded(UUID uuid){return challengeAwarded.getOrDefault(uuid.toString(),0);}
    public int pendingKeys(UUID uuid){return pendingKeys.getOrDefault(uuid.toString(),0);}

    /**
     * Jeden przyrost dla zatwierdzonej akcji. Progi przechodzą do bitmaski
     * ukończonych wyzwań i kolejki nagród w tym samym atomowym stanie.
     */
    public EventState challengeActions(UUID player,int amount,EventType expectedType,
                                       long expectedStart,int[] goals,List<Integer> rewards){
        if(amount<=0||!Objects.equals(type,expectedType.id())||startedAt!=expectedStart)
            return this;
        if(goals.length!=3||rewards.size()!=3)
            throw new IllegalArgumentException("Potrzebne są trzy progi wyzwań.");
        String id=player.toString();
        int before=challengePoints.getOrDefault(id,0);
        int after=(int)Math.min(MAX_POINTS,(long)before+amount);
        if(after==before)return this;
        int mask=challengeAwarded.getOrDefault(id,0),earned=0;
        for(int i=0;i<3;i++){
            if(after>=goals[i] && (mask&(1<<i))==0){
                mask|=1<<i;
                earned+=rewards.get(i);
            }
        }
        Map<String,Integer> counts=new HashMap<>(challengePoints);
        counts.put(id,after);
        Map<String,Integer> claimed=new HashMap<>(challengeAwarded);
        if(mask!=0)claimed.put(id,mask);
        Map<String,Integer> pending=new HashMap<>(pendingKeys);
        if(earned>0){
            int existing=pending.getOrDefault(id,0);
            if(existing>MAX_PENDING-earned)
                throw new IllegalArgumentException("Zbyt wiele oczekujących kluczy.");
            pending.put(id,existing+earned);
        }
        return new EventState(type,startedAt,endsAt,progress,pvpCooldowns,
                counts,claimed,pending);
    }
    public EventState keysDelivered(UUID player,int amount){
        if(amount<1)throw new IllegalArgumentException("Niepoprawna ilość kluczy.");
        String id=player.toString();
        int existing=pendingKeys.getOrDefault(id,0);
        if(existing<amount)throw new IllegalArgumentException("Klucze zostały już odebrane.");
        Map<String,Integer> next=new HashMap<>(pendingKeys);
        if(existing==amount)next.remove(id);
        else next.put(id,existing-amount);
        return new EventState(type,startedAt,endsAt,progress,pvpCooldowns,
                challengePoints,challengeAwarded,next);
    }

    private static String pair(UUID killer,UUID victim){return killer+":"+victim;}
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
        if(next.size()>=MAX_PVP_PAIRS){
            String oldest=next.entrySet().stream()
                    .min(Map.Entry.comparingByValue()).orElseThrow().getKey();
            next.remove(oldest);
        }
        next.put(pair(killer,victim),now);
        return new EventState(type,startedAt,endsAt,progress,next,
                challengePoints,challengeAwarded,pendingKeys);
    }
    public void validate(){
        if(type!=null&&EventType.parse(type)==null)throw new IllegalArgumentException("Nieznany event.");
        if(startedAt<0||endsAt<0||progress.size()>MAX_PLAYERS||
                pvpCooldowns.size()>MAX_PVP_PAIRS||challengePoints.size()>MAX_PLAYERS||
                challengeAwarded.size()>MAX_PLAYERS||pendingKeys.size()>MAX_PLAYERS)
            throw new IllegalArgumentException("Błędne dane eventu.");
        if(type!=null&&(startedAt==0||endsAt<=startedAt))
            throw new IllegalArgumentException("Błędny termin eventu.");
        for(var entry:progress.entrySet()){
            checkPlayerId(entry.getKey());
            if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>MAX_POINTS)
                throw new IllegalArgumentException("Błędny postęp.");
        }
        for(var entry:challengePoints.entrySet()){
            checkPlayerId(entry.getKey());
            if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>MAX_POINTS)
                throw new IllegalArgumentException("Błędny wynik wyzwania.");
        }
        for(var entry:challengeAwarded.entrySet()){
            checkPlayerId(entry.getKey());
            if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>7)
                throw new IllegalArgumentException("Błędne ukończenia wyzwań.");
        }
        for(var entry:pendingKeys.entrySet()){
            checkPlayerId(entry.getKey());
            if(entry.getValue()==null||entry.getValue()<1||entry.getValue()>MAX_PENDING)
                throw new IllegalArgumentException("Błędna ilość oczekujących kluczy.");
        }
        for(var entry:pvpCooldowns.entrySet()){
            if(entry.getKey()==null||entry.getValue()==null||entry.getValue()<0)
                throw new IllegalArgumentException("Błędna para PvP.");
            String[] ids=entry.getKey().split(":",-1);
            if(ids.length!=2)throw new IllegalArgumentException("Błędny format pary PvP.");
            checkPlayerId(ids[0]);checkPlayerId(ids[1]);
        }
    }
    private static void checkPlayerId(String value){
        try{UUID.fromString(value);}
        catch(RuntimeException ex){throw new IllegalArgumentException("Błędny UUID gracza.",ex);}
    }
}
