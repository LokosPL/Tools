package pl.lokos.tools.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;

/** Jeden moduł moderacji. Odczyt początkowy przy starcie, kolejne zapisy poza main thread. */
public final class ChatManager implements AutoCloseable {
    public record MuteEntry(UUID uuid,ChatStateFile.Mute mute) {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final Path folder;
    private final ExecutorService writer=Executors.newSingleThreadExecutor(task->{
        Thread thread=new Thread(task,"Tools-Chat-Pliki");
        thread.setDaemon(false);
        return thread;
    });
    private final SlowModeLimiter limiter=new SlowModeLimiter();
    private final Set<UUID> operators=ConcurrentHashMap.newKeySet();
    private volatile ChatConfig config;
    private volatile ChatStateFile state;
    private int nextAnnouncement;
    private volatile long nextAnnouncementAt;
    private long nextCleanupAt;

    public ChatManager(JavaPlugin plugin,RankManager ranks,Path folder) throws IOException {
        this.plugin=plugin;this.ranks=ranks;this.folder=folder;
        var json=new JsonConfigManager(folder);
        try {
            config=json.load("Chat.json",ChatConfig.class,ChatConfig::new,ChatConfig::validate);
            state=json.load("ChatState.json",ChatStateFile.class,ChatStateFile::new,ChatStateFile::validate);
        } catch(IOException error) {
            writer.shutdown();
            throw error;
        }
        nextAnnouncementAt=System.currentTimeMillis()+config.announcementIntervalSeconds()*1000L;
    }
    public ChatConfig config(){return config;}
    public ChatStateFile state(){return state;}

    public void refreshOperators(){
        operators.clear();
        for(Player player:Bukkit.getOnlinePlayers())if(player.isOp())operators.add(player.getUniqueId());
    }

    public ChatPolicy.Decision check(UUID uuid,long now){
        RankSnapshot snapshot=ranks==null?RankSnapshot.empty():ranks.snapshot();
        return ChatPolicy.evaluate(state,config,snapshot,uuid,operators.contains(uuid),now,limiter);
    }
    public ChatStateFile.Mute mute(UUID uuid){return state.muted().get(uuid.toString());}
    public void leave(UUID uuid){limiter.remove(uuid);operators.remove(uuid);}

    public synchronized CompletableFuture<Void> setEnabled(boolean value){
        return change(s->s.withEnabled(value));
    }
    public synchronized CompletableFuture<Void> setAnnouncements(boolean value){
        return change(s->s.withAnnouncements(value));
    }
    public synchronized CompletableFuture<Void> setRank(String rank){
        return change(s->s.withRank(rank));
    }
    public synchronized CompletableFuture<Void> silence(UUID id,String nick,long until,String reason){
        return change(s->s.withMute(id,new ChatStateFile.Mute(nick,until,reason)));
    }
    public synchronized CompletableFuture<Void> unsilence(UUID id){
        return change(s->s.withoutMute(id));
    }
    private synchronized CompletableFuture<Void> change(UnaryOperator<ChatStateFile> operation){
        var updated=operation.apply(state);
        // Natychmiast blokuje dalsze wypowiedzi; zapis odbywa się sekwencyjnie w IO.
        state=updated;
        return CompletableFuture.runAsync(()->save(updated),writer);
    }

    private void save(ChatStateFile updated){
        Path destination=folder.resolve("ChatState.json");
        Path temporary=null;
        try{
            temporary=Files.createTempFile(folder,".tools-chat-",".tmp");
            Files.writeString(temporary,GSON.toJson(updated)+"\n",StandardCharsets.UTF_8);
            try{Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){
                Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException ex){
            plugin.getLogger().severe("Nie zapisano ChatState.json: "+ex.getMessage());
            throw new CompletionException(ex);
        } finally {
            if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){}
        }
    }

    public List<MuteEntry> activeMutes(long now) {
        return state.muted().entrySet().stream().filter(e->e.getValue().active(now))
                .map(e->new MuteEntry(UUID.fromString(e.getKey()),e.getValue()))
                .sorted(Comparator.comparing(e->e.mute().name(),String.CASE_INSENSITIVE_ORDER)).toList();
    }
    public Optional<MuteEntry> findMute(String nick,long now){
        return activeMutes(now).stream().filter(e->e.mute().name().equalsIgnoreCase(nick)).findFirst();
    }

    public CompletableFuture<Void> reloadConfig(){
        return CompletableFuture.supplyAsync(()->{
            try{return new JsonConfigManager(folder).load("Chat.json",ChatConfig.class,
                    ChatConfig::new,ChatConfig::validate);}
            catch(IOException e){throw new CompletionException(e);}
        },writer).thenCompose(fresh->{
            var completion=new CompletableFuture<Void>();
            plugin.getServer().getScheduler().runTask(plugin,()->{
                config=fresh;
                nextAnnouncementAt=System.currentTimeMillis()+fresh.announcementIntervalSeconds()*1000L;
                completion.complete(null);
            });
            return completion;
        });
    }

    /** Wywoływane raz na sekundę na głównym wątku; ogłoszenia nie zużywają limitu graczy. */
    public void tick(){
        long now=System.currentTimeMillis();
        refreshOperators();
        if(now>=nextCleanupAt) {
            limiter.cleanup(now);
            nextCleanupAt=now+60000L;
            var before=state;
            if(before.muted().values().stream().anyMatch(m->!m.active(now)))
                change(s->s.withoutExpired(now));
        }
        ChatConfig cfg=config;
        if(now<nextAnnouncementAt)return;
        nextAnnouncementAt=now+cfg.announcementIntervalSeconds()*1000L;
        if(!cfg.announcementsEnabled() || !state.announcementsEnabled()
                || cfg.announcements().isEmpty())return;
        String announcement=cfg.announcements().get(nextAnnouncement++%cfg.announcements().size());
        for(Player player:Bukkit.getOnlinePlayers())
            player.sendMessage(Colors.color(cfg.announcementPrefix()+announcement));
    }

    @Override public void close(){writer.shutdown();}
}
