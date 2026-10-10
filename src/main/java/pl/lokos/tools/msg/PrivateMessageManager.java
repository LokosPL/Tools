package pl.lokos.tools.msg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

/** Bez SQL i operacji plikowych na main thread po uruchomieniu serwera. */
public final class PrivateMessageManager implements AutoCloseable {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final JavaPlugin plugin;
    private final Path folder;
    private final ExecutorService writer=Executors.newSingleThreadExecutor(task->{
        Thread thread=new Thread(task,"Tools-PrywatneWiadomosci");
        thread.setDaemon(false);
        return thread;
    });
    private volatile PrivateMessageConfig config;
    private volatile PrivateMessageState state;
    private final Map<UUID,UUID> lastPartner=new HashMap<>();
    private final Map<UUID,Long> lastSent=new HashMap<>();
    private volatile boolean closing;

    public PrivateMessageManager(JavaPlugin plugin,Path folder) throws IOException {
        this.plugin=plugin;this.folder=folder;
        var loader=new JsonConfigManager(folder);
        try {
            config=loader.load("PrivateMessages.json",PrivateMessageConfig.class,
                    PrivateMessageConfig::new,PrivateMessageConfig::validate);
            state=loader.load("PrivateMessagesState.json",PrivateMessageState.class,
                    PrivateMessageState::new,PrivateMessageState::validate);
        } catch(IOException failure){writer.shutdown();throw failure;}
    }
    public PrivateMessageConfig config(){return config;}
    public PrivateMessageState state(){return state;}
    public UUID partner(UUID uuid){return lastPartner.get(uuid);}
    public long lastSent(UUID uuid){return lastSent.getOrDefault(uuid,0L);}
    public void delivered(UUID sender,UUID receiver,long at){
        lastPartner.put(sender,receiver);lastPartner.put(receiver,sender);
        lastSent.put(sender,at);
    }
    public void leave(UUID uuid){
        lastPartner.remove(uuid);
        lastPartner.entrySet().removeIf(e->e.getValue().equals(uuid));
        lastSent.remove(uuid);
    }
    public synchronized CompletableFuture<Void> disable(UUID uuid,boolean disable){
        return mutate(s->s.withDisabled(uuid,disable));
    }
    public synchronized CompletableFuture<Void> ignore(UUID owner,UUID target,boolean ignored){
        return mutate(s->s.withIgnore(owner,target,ignored));
    }
    private synchronized CompletableFuture<Void> mutate(UnaryOperator<PrivateMessageState> modify){
        if(closing)return CompletableFuture.failedFuture(new IllegalStateException("Moduł MSG został wyłączony."));
        PrivateMessageState next=modify.apply(state);
        state=next;
        return CompletableFuture.runAsync(()->writeState(next),writer);
    }
    private void writeState(PrivateMessageState next) {
        Path file=folder.resolve("PrivateMessagesState.json"),tmp=null;
        try {
            tmp=Files.createTempFile(folder,".tools-msg-",".tmp");
            Files.writeString(tmp,GSON.toJson(next)+"\n",StandardCharsets.UTF_8);
            try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored) {
                Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException failure) {
            plugin.getLogger().severe("Nie można zapisać PrivateMessagesState.json: "+failure.getMessage());
            throw new CompletionException(failure);
        } finally {
            if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}
        }
    }
    public CompletableFuture<Void> reloadConfig(){
        return CompletableFuture.supplyAsync(()->{
            try{return new JsonConfigManager(folder).load("PrivateMessages.json",
                    PrivateMessageConfig.class,PrivateMessageConfig::new,PrivateMessageConfig::validate);}
            catch(IOException e){throw new CompletionException(e);}
        },writer).thenAccept(fresh->config=fresh);
    }
    @Override public synchronized void close(){closing=true;writer.shutdown();}
}
