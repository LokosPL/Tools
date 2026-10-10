package pl.lokos.tools.msg;

import java.util.*;

/** Snapshot ustawień graczy: UUID to klucz, nick wyłącznie do wyświetlania listy. */
public final class PrivateMessageState {
    private Set<String> disabled = new LinkedHashSet<>();
    private Map<String,Map<String,String>> ignored = new LinkedHashMap<>();

    public Set<String> disabled(){return Collections.unmodifiableSet(disabled);}
    public Map<String,Map<String,String>> ignored(){return Collections.unmodifiableMap(ignored);}
    public boolean disabled(UUID player){return disabled.contains(player.toString());}
    public boolean ignores(UUID owner,UUID sender){
        return ignored.getOrDefault(owner.toString(),Map.of()).containsKey(sender.toString());
    }
    public Map<String,String> ignoredBy(UUID owner){
        return Collections.unmodifiableMap(ignored.getOrDefault(owner.toString(),Map.of()));
    }
    public PrivateMessageState withDisabled(UUID owner,boolean value){
        var off=new LinkedHashSet<>(disabled);
        if(value)off.add(owner.toString());else off.remove(owner.toString());
        return copy(off,ignored);
    }
    public PrivateMessageState withIgnore(UUID owner,UUID target,String name,boolean value){
        var copy=new LinkedHashMap<>(ignored);
        var users=new LinkedHashMap<>(copy.getOrDefault(owner.toString(),Map.of()));
        if(value)users.put(target.toString(),name);else users.remove(target.toString());
        if(users.isEmpty())copy.remove(owner.toString());
        else copy.put(owner.toString(),users);
        return copy(disabled,copy);
    }
    private static PrivateMessageState copy(Set<String> disabled,Map<String,Map<String,String>> ignored){
        PrivateMessageState next=new PrivateMessageState();
        next.disabled=new LinkedHashSet<>(disabled);
        next.ignored=new LinkedHashMap<>(ignored);
        next.validate();
        return next;
    }
    public void validate(){
        if(disabled==null || ignored==null || disabled.size()>100000 || ignored.size()>100000)
            throw new IllegalArgumentException("PrivateMessagesState.json: nieprawidłowy stan.");
        for(String uuid:disabled)validUuid(uuid);
        for(var entry:ignored.entrySet()){
            validUuid(entry.getKey());
            if(entry.getValue()==null || entry.getValue().size()>10000)
                throw new IllegalArgumentException("PrivateMessagesState.json: zbyt długa lista ignorowanych.");
            for(var subject:entry.getValue().entrySet()){
                validUuid(subject.getKey());
                if(subject.getValue()==null || !subject.getValue().matches("[A-Za-z0-9_]{1,16}"))
                    throw new IllegalArgumentException("PrivateMessagesState.json: błędna nazwa gracza.");
            }
        }
    }
    private static void validUuid(String value){
        try {UUID.fromString(value);}
        catch(RuntimeException ex){throw new IllegalArgumentException(
                "PrivateMessagesState.json: nieprawidłowy UUID.",ex);}
    }
}
