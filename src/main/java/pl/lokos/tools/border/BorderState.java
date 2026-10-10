package pl.lokos.tools.border;

import java.util.*;

/** Trwały stan ekspansji, godziny aktywności zapisane per UUID i dzień. */
public record BorderState(String world,double centerX,double centerZ,int diameter,
                          long cycleStarted,long lastExpansion,Map<String,Integer> dailyMinutes) {
    public BorderState(){this(null,0,0,0,0,0,Map.of());}
    public BorderState{
        dailyMinutes=dailyMinutes==null?Map.of():Map.copyOf(dailyMinutes);
    }
    public BorderState started(UUID uid,double x,double z,int size,long now){
        return new BorderState(uid.toString(),x,z,size,now,now,Map.of());
    }
    public BorderState increment(String playerDay,int cap){
        int count=dailyMinutes.getOrDefault(playerDay,0);
        if(count>=cap)return this;
        Map<String,Integer> copy=new HashMap<>(dailyMinutes);
        copy.put(playerDay,count+1);
        return new BorderState(world,centerX,centerZ,diameter,cycleStarted,lastExpansion,copy);
    }
    public int totalMinutes(){return dailyMinutes.values().stream().mapToInt(Integer::intValue).sum();}
    public BorderState expanded(int newDiameter,long now){
        return new BorderState(world,centerX,centerZ,newDiameter,now,now,Map.of());
    }
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
