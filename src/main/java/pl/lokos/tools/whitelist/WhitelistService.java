package pl.lokos.tools.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.StateChanges;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Konfiguracja trwała, odczytywana atomowo z volatile snapshot.
 * Zapisy w jednym workerze; stan zmienia się dopiero PO udanym zapisie JSON.
 * Prawa loginu są sprawdzane także w AsyncPlayerPreLoginEvent.
 */
public final class WhitelistService implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final JavaPlugin plugin;
    private final Path file;
    private final ExecutorService writer;
    private volatile WhitelistConfig state;
    private volatile boolean closed;

    public WhitelistService(JavaPlugin plugin) throws IOException {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("Whitelist.json");
        this.state = new JsonConfigManager(plugin.getDataFolder().toPath()).load(
                "Whitelist.json", WhitelistConfig.class, WhitelistConfig::new, WhitelistConfig::validate);
        writer = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "Tools-Whitelist-IO");
            thread.setDaemon(true);
            return thread;
        });
    }

    public WhitelistConfig state() { return state; }
    public boolean allowed(String name) {
        return !state.enabled() || state.players().contains(name.toLowerCase(Locale.ROOT));
    }
    public String refusal() { return state.kickText(); }

    public CompletableFuture<Void> enable(WhitelistMode mode) {
        return change(previous -> {
            StateChanges.requireChange(previous.enabled() && previous.mode().equals(mode.name()),
                    "Whitelist jest już włączona w trybie "+mode.title()+".");
            return previous.with(true,mode.name(),previous.players());
        });
    }
    public CompletableFuture<Void> disable() {
        return change(previous -> {
            StateChanges.requireChange(!previous.enabled(),"Whitelist jest już wyłączona.");
            return previous.with(false,previous.mode(),previous.players());
        });
    }
    public CompletableFuture<Void> add(String player) {
        String name = WhitelistConfig.normalize(player);
        return change(previous -> {
            Set<String> next = new LinkedHashSet<>(previous.players());
            StateChanges.requireChange(!next.add(name),"Gracz "+name+" jest już na whiteliście.");
            return previous.with(previous.enabled(), previous.mode(), next);
        });
    }
    public CompletableFuture<Void> remove(String player) {
        String name = WhitelistConfig.normalize(player);
        return change(previous -> {
            Set<String> next = new LinkedHashSet<>(previous.players());
            StateChanges.requireChange(!next.remove(name),"Gracza "+name+" nie ma na whiteliście.");
            return previous.with(previous.enabled(), previous.mode(), next);
        });
    }

    /** Reload z dysku, serializowany z komendami administracyjnymi. */
    public CompletableFuture<Void> reload() {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Whitelist jest wyłączona."));
        CompletableFuture<Void> future=new CompletableFuture<>();
        try {
            writer.execute(()->{
                try {
                    WhitelistConfig loaded=new JsonConfigManager(plugin.getDataFolder().toPath())
                            .load("Whitelist.json",WhitelistConfig.class,WhitelistConfig::new,WhitelistConfig::validate);
                    state=loaded;
                    if(loaded.enabled() && plugin.isEnabled())
                        Bukkit.getScheduler().runTask(plugin,()->{
                            for(Player player:Bukkit.getOnlinePlayers())
                                if(!allowed(player.getName()))player.kick(Colors.color(refusal()));
                        });
                    future.complete(null);
                } catch (Exception error){future.completeExceptionally(error);}
            });
        } catch (RejectedExecutionException ex){future.completeExceptionally(ex);}
        return future;
    }

    private CompletableFuture<Void> change(Function<WhitelistConfig, WhitelistConfig> mutation) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Moduł został zamknięty."));
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            writer.execute(() -> {
                try {
                    WhitelistConfig next = mutation.apply(state);
                    next.validate();
                    Path temporary = Files.createTempFile(file.getParent(), ".whitelist-", ".tmp");
                    try {
                        Files.writeString(temporary, GSON.toJson(next) + "\n", StandardCharsets.UTF_8);
                        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                        catch (AtomicMoveNotSupportedException error) {
                            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } finally { Files.deleteIfExists(temporary); }
                    state = next;
                    if (next.enabled() && plugin.isEnabled()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!plugin.isEnabled()) return;
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (!allowed(player.getName()))
                                    player.kick(Colors.color(refusal()));
                            }
                        });
                    }
                    result.complete(null);
                } catch (Exception error) { result.completeExceptionally(error); }
            });
        } catch (RejectedExecutionException ex) { result.completeExceptionally(ex); }
        return result;
    }

    @Override public void close() {
        closed = true;
        writer.shutdown();
        try { if (!writer.awaitTermination(3, TimeUnit.SECONDS)) writer.shutdownNow(); }
        catch (InterruptedException ex) { writer.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
