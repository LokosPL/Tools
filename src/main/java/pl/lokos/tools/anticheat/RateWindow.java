package pl.lokos.tools.anticheat;

import java.util.*;

/** Okno stałej długości, ograniczone mapy pamięci (ochrona przed DoS). */
public final class RateWindow<K> {
    private record Bucket(long start,int count) {}
    private final Map<K,Bucket> entries=new HashMap<>();
    private final long milliseconds;
    private final int maximumKeys;
    public RateWindow(long milliseconds,int maximumKeys){
        if(milliseconds<1||maximumKeys<1)throw new IllegalArgumentException();
        this.milliseconds=milliseconds;this.maximumKeys=maximumKeys;
    }
    public boolean exceeded(K key,int max,long now){
        Bucket old=entries.get(key);
        if(old==null||now-old.start()>=milliseconds||now<old.start()){
            if(entries.size()>=maximumKeys)cleanup(now);
            if(entries.size()>=maximumKeys)entries.clear(); // mała, ograniczona liczba kluczy
            entries.put(key,new Bucket(now,1));
            return false;
        }
        int next=Math.min(Integer.MAX_VALUE,old.count()+1);
        entries.put(key,new Bucket(old.start(),next));
        return next>max;
    }
    public void remove(K key){entries.remove(key);}
    public void cleanup(long now){entries.entrySet().removeIf(e->now<e.getValue().start()
            ||now-e.getValue().start()>=milliseconds);}
    public int size(){return entries.size();}
}
