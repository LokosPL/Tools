package pl.lokos.tools.anticheat;

import java.util.*;
/** Trwale ustawienia; powiadomienia wyciszane osobno dla każdego moderatora. */
public record AntiCheatState(boolean enabled, Set<String> silentViewers) {
    public AntiCheatState(){this(true,Set.of());}
    public AntiCheatState{
        silentViewers=silentViewers==null?Set.of():Set.copyOf(silentViewers);
    }
    public boolean receives(UUID player){return !silentViewers.contains(player.toString());}
    public AntiCheatState withEnabled(boolean value){return new AntiCheatState(value,silentViewers);}
    public AntiCheatState withAlerts(UUID uuid,boolean enabled){
        Set<String> copy=new HashSet<>(silentViewers);
        if(enabled)copy.remove(uuid.toString()); else copy.add(uuid.toString());
        return new AntiCheatState(this.enabled,copy);
    }
    public void validate(){
        if(silentViewers.size()>100000)throw new IllegalArgumentException("Zbyt duży stan powiadomień.");
        for(String value:silentViewers)try{UUID.fromString(value);}
        catch(IllegalArgumentException failure){throw new IllegalArgumentException("Błędne UUID.",failure);}
    }
}
