package pl.lokos.tools.chat;

import java.util.*;

/** Przesuwne okno; dokładnie N zaakceptowanych wiadomości w ostatnich S sekundach. */
public final class SlowModeLimiter {
    private final Map<UUID,ArrayDeque<Long>> messages=new HashMap<>();
    public synchronized boolean allow(UUID uuid,long now,int windowSeconds,int maxMessages){
        ArrayDeque<Long> times=messages.computeIfAbsent(uuid,id->new ArrayDeque<>());
        long threshold=now-windowSeconds*1000L;
        while(!times.isEmpty() && times.peekFirst()<=threshold)times.removeFirst();
        if(times.size()>=maxMessages)return false;
        times.addLast(now);
        return true;
    }
    public synchronized void remove(UUID uuid){messages.remove(uuid);}
    public synchronized void cleanup(long now) {
        messages.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(time->time<now-60000L);
            return entry.getValue().isEmpty();
        });
    }
}
