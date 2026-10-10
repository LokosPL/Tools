package pl.lokos.tools.events;

import java.util.*;

/** Stan przechowywany po restarcie, tylko jeden aktywny typ eventu. */
public record EventState(String type,long startedAt,long endsAt,Map<String,Integer> progress) {
    public EventState(){this(null,0,0,Map.of());}
    public EventState {
        progress=progress==null?Map.of():Map.copyOf(progress);
    }
    public EventType active(long now){
        EventType kind=EventType.parse(type);
        return kind!=null && endsAt>now && startedAt>0?kind:null;
    }
    public EventState started(EventType next,long start,long finish){
        return new EventState(next.id(),start,finish,Map.of());
    }
    public EventState ended(){return new EventState();}
    public EventState progress(UUID uuid,int count){
        var copy=new HashMap<>(progress);
        if(count<=0)copy.remove(uuid.toString());
        else copy.put(uuid.toString(),count);
        return new EventState(type,startedAt,endsAt,copy);
    }
    public int progress(UUID uuid){return progress.getOrDefault(uuid.toString(),0);}
    public void validate(){
        if(type!=null&&EventType.parse(type)==null)throw new IllegalArgumentException("Nieznany event.");
        if(startedAt<0||endsAt<0||progress.size()>100000)
            throw new IllegalArgumentException("Błędne dane eventu.");
        if(type!=null&&(startedAt==0||endsAt<=startedAt))
            throw new IllegalArgumentException("Błędny termin eventu.");
        for(var entry:progress.entrySet()){
            try{UUID.fromString(entry.getKey());}
            catch(IllegalArgumentException ex){throw new IllegalArgumentException("Błędny UUID.",ex);}
            if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>1000000)
                throw new IllegalArgumentException("Błędny postęp.");
        }
    }
}
