package pl.lokos.tools.border;

import java.util.*;

/** Trwały stan ekspansji, godziny aktywności zapisane per UUID i dzień. */
public record BorderState(String world,double centerX,double centerZ,int diameter,
                          long cycleStarted,long lastExpansion,boolean paused,Map<String,Integer> dailyMinutes) {
    public BorderState(){this(null,0,0,0,0,0,false,Map.of());}
    public BorderState{
        dailyMinutes=dailyMinutes==null?Map.of():Map.copyOf(dailyMinutes);
    }
    public BorderState started(UUID uid,double x,double z,int size,long now){
        return new BorderState(uid.toString(),x,z,size,now,now,false,Map.of());
    }
    public BorderState increment(String playerDay,int cap){
        int count=dailyMinutes.getOrDefault(playerDay,0);
        if(count>=cap)return this;
        Map<String,Integer> copy=new HashMap<>(dailyMinutes);
        copy.put(playerDay,count+1);
        return new BorderState(world,centerX,centerZ,diameter,cycleStarted,lastExpansion,paused,copy);
    }
    /** Jeden snapshot i zapis na minutę zamiast jednego zapisu na każdego gracza. */
    public BorderState countActivity(Collection<String> playerDays,int dailyCap){
        if(playerDays.isEmpty())return this;
        Map<String,Integer> copy=new HashMap<>(dailyMinutes);
        boolean changed=false;
        for(String id:playerDays){
            int old=copy.getOrDefault(id,0);
            if(old<dailyCap){copy.put(id,old+1);changed=true;}
        }
        return changed?new BorderState(world,centerX,centerZ,diameter,cycleStarted,lastExpansion,paused,copy):this;
    }
    public int totalMinutes(){return dailyMinutes.values().stream().mapToInt(Integer::intValue).sum();}
    public BorderState expanded(int newDiameter,long now){
        return new BorderState(world,centerX,centerZ,newDiameter,now,now,paused,Map.of());
    }
    public BorderState withPause(boolean next){return new BorderState(world,centerX,centerZ,diameter,cycleStarted,lastExpansion,next,dailyMinutes);}
    public void validate(){
        if(diameter<0||diameter>30000000||cycleStarted<0||lastExpansion<0||
                !Double.isFinite(centerX)||!Double.isFinite(centerZ)||dailyMinutes.size()>100000)
            throw new IllegalArgumentException("Błędny WorldBorderState.json.");
        if(world!=null)try{UUID.fromString(world);}
        catch(IllegalArgumentException ex){throw new IllegalArgumentException("Błędny świat.",ex);}
        for(int count:dailyMinutes.values())if(count<0||count>1440)
            throw new IllegalArgumentException("Błędna aktywność.");
    }
}
