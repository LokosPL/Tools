package pl.lokos.tools.staff;

import java.util.Set;
import java.util.UUID;

/** Trwałe, niemodyfikowalne dane: vanish i ostatni aktywny komunikat bossbar. */
public record StaffState(Set<String> vanished, Broadcast activeBroadcast) {
    public record Broadcast(String message,long startedAt,long untilMillis){
        public void validate() {
            if(message==null || message.isBlank() || message.length()>500
                    || message.contains("\n") || message.contains("\r")
                    || startedAt<0 || untilMillis<=startedAt)
                throw new IllegalArgumentException("Niepoprawny komunikat bossbar.");
        }
        public boolean active(long now){return now<untilMillis;}
    }
    public StaffState(){this(Set.of(),null);}
    public StaffState{
        vanished=vanished==null?Set.of():Set.copyOf(vanished);
    }
    public boolean vanished(UUID player){return vanished.contains(player.toString());}
    public StaffState withVanish(UUID id,boolean enable){
        var changed=new java.util.HashSet<>(vanished);
        if(enable)changed.add(id.toString());else changed.remove(id.toString());
        return new StaffState(changed,activeBroadcast);
    }
    public StaffState withBroadcast(Broadcast broadcast){return new StaffState(vanished,broadcast);}
    public void validate(){
        if(vanished.size()>100000)throw new IllegalArgumentException("Zbyt wiele danych vanish.");
        for(String id:vanished) {
            try{UUID.fromString(id);}
            catch(IllegalArgumentException e){throw new IllegalArgumentException("Nieprawidłowe UUID vanish.",e);}
        }
        if(activeBroadcast!=null)activeBroadcast.validate();
    }
}
