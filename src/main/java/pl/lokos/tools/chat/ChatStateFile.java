package pl.lokos.tools.chat;

import java.util.*;

/**
 * ChatState.json jest trwałym stanem moderacji. Zmiana Chat.json go nie nadpisuje.
 * Wyciszenia identyfikujemy po UUID, nazwę przechowujemy tylko do wyświetlania.
 */
public final class ChatStateFile {
    public record Mute(String name, long untilMillis, String reason) {
        public boolean active(long now) {return untilMillis == 0 || untilMillis > now;}
    }
    private boolean enabled = true;
    private boolean announcementsEnabled = true;
    private String minimumRank;
    private Map<String,Mute> muted = new LinkedHashMap<>();

    public boolean enabled(){return enabled;}
    public boolean announcementsEnabled(){return announcementsEnabled;}
    public String minimumRank(){return minimumRank;}
    public Map<String,Mute> muted(){return Map.copyOf(muted);}

    public ChatStateFile withEnabled(boolean value) {
        return copy(value,announcementsEnabled,minimumRank,muted);
    }
    public ChatStateFile withAnnouncements(boolean value) {
        return copy(enabled,value,minimumRank,muted);
    }
    public ChatStateFile withRank(String name) {
        return copy(enabled,announcementsEnabled,name,muted);
    }
    public ChatStateFile withMute(UUID uuid,Mute mute) {
        var next=new LinkedHashMap<>(muted);
        next.put(uuid.toString(),mute);
        return copy(enabled,announcementsEnabled,minimumRank,next);
    }
    public ChatStateFile withoutMute(UUID uuid) {
        var next=new LinkedHashMap<>(muted);
        next.remove(uuid.toString());
        return copy(enabled,announcementsEnabled,minimumRank,next);
    }
    public ChatStateFile withoutExpired(long now) {
        var next=new LinkedHashMap<>(muted);
        next.entrySet().removeIf(e->!e.getValue().active(now));
        return copy(enabled,announcementsEnabled,minimumRank,next);
    }
    private static ChatStateFile copy(boolean enabled,boolean announcements,String rank,Map<String,Mute> muted) {
        ChatStateFile file=new ChatStateFile();
        file.enabled=enabled;file.announcementsEnabled=announcements;file.minimumRank=rank;
        file.muted=new LinkedHashMap<>(muted);
        file.validate();
        return file;
    }

    public void validate() {
        if(minimumRank!=null && !minimumRank.matches("[\\p{L}0-9_-]{1,24}"))
            throw new IllegalArgumentException("ChatState.json: błędna ranga czatu.");
        if(muted==null || muted.size()>100000)throw new IllegalArgumentException("ChatState.json: błędne wyciszenia.");
        for(var entry:muted.entrySet()) {
            try { UUID.fromString(entry.getKey()); }
            catch(RuntimeException ex){throw new IllegalArgumentException("ChatState.json: błędne UUID wyciszenia.",ex);}
            Mute mute=entry.getValue();
            if(mute==null || mute.name()==null || !mute.name().matches("[A-Za-z0-9_]{1,16}")
                    || mute.reason()==null || mute.reason().length()>200 || mute.untilMillis()<0)
                throw new IllegalArgumentException("ChatState.json: błędny wpis wyciszenia.");
        }
    }
}
