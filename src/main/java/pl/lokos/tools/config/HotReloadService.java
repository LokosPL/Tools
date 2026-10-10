package pl.lokos.tools.config;

import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Messages;
import java.util.Map;
import pl.lokos.tools.listeners.RankListener;
import pl.lokos.tools.manager.RankVisualManager;
import pl.lokos.tools.registry.ConfigRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transakcyjne "przeładuj" dla ustawień bezpiecznych do zmian na żywo.
 * Nie rejestruje ponownie komend ani połączeń DB. Błędy nie zmieniają
 * działającej konfiguracji. Odczyt plików jest poza wątkiem Paper.
 */
public final class HotReloadService implements AutoCloseable {
    private static final Gson GSON = new Gson();
    private final JavaPlugin plugin;
    private final ConfigRegistry active;
    private final ToolsConfig settings;
    private final RankVisualManager visuals;
    private final RankListener listener;
    private final CommandTextRegistry commandTexts;
    private final ThreadPoolExecutor io;
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean closed;

    public HotReloadService(JavaPlugin plugin, ConfigRegistry active, ToolsConfig settings,
                            RankVisualManager visuals, RankListener listener, CommandTextRegistry commandTexts) {
        this.plugin = plugin;
        this.active = active;
        this.settings = settings;
        this.visuals = visuals;
        this.listener = listener;
        this.commandTexts=commandTexts;
        this.io = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(8), task -> {
                    Thread thread = new Thread(task, "Tools-Konfiguracja");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<String> reload() {
        if (closed || !busy.compareAndSet(false, true))
            return CompletableFuture.failedFuture(new IllegalStateException(
                    closed ? "Moduł konfiguracji jest wyłączony." : "Przeładowanie już trwa."));

        CompletableFuture<String> result = new CompletableFuture<>();
        try {
            io.execute(() -> {
                try {
                    Path folder = plugin.getDataFolder().toPath();
                    JsonConfigManager json = new JsonConfigManager(folder);
                    ToolsConfig.Database db = json.load("MySql.json", ToolsConfig.Database.class,
                            ToolsConfig.Database::new, ToolsConfig.Database::validate);
                    CommandsFile commands = json.load("Commands.json", CommandsFile.class,
                            CommandsFile::new, CommandsFile::validate);
                    RanksFile ranks = json.load("Ranks.json", RanksFile.class,
                            RanksFile::new, RanksFile::validate);
                    RegionsFile regions = json.load("Regions.json", RegionsFile.class,
                            RegionsFile::new, RegionsFile::validate);
                    Map<String,CommandTextFile> nextTexts=commandTexts.loadSnapshot();

                    verifySafeToReload(settings.database(), db,
                            active.commands(), commands,
                            active.definitions().ranks(), ranks,
                            active.definitions().regions(), regions);
                    if (!plugin.isEnabled()) throw new IllegalStateException("Plugin został wyłączony.");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        try {
                            // Sprawdzamy ponownie, bo admin mógł edytować rangę podczas odczytu.
                            verifySafeToReload(settings.database(), db,
                                    active.commands(), commands,
                                    active.definitions().ranks(), ranks,
                                    active.definitions().regions(), regions);
                            // RanksFile ma te same definicje; zmieniamy tylko ustawienia wizualne.
                            settings.configure(settings.database(), ranks.settings(), settings.regions());
                            active.definitions().applyRankSettings(ranks);
                            active.applyCommands(commands);
                            Messages.configure(commands.messagePrefix());
                            if (visuals != null) visuals.applySettings(ranks.settings());
                            if (listener != null) listener.applySettings(ranks.settings());
                            commandTexts.install(nextTexts);
                            result.complete("Odświeżono wygląd TAB-u, czat i teksty commands/*.json. "
                                    + "Zmiana aliasów, uprawnień, regionów lub MySQL wymaga restartu.");
                        } catch (Exception failure) {
                            result.completeExceptionally(failure);
                        } finally {
                            busy.set(false);
                        }
                    });
                } catch (Exception failure) {
                    result.completeExceptionally(failure);
                    busy.set(false);
                }
            });
        } catch (RejectedExecutionException error) {
            busy.set(false);
            result.completeExceptionally(error);
        }
        return result;
    }

    static void verifySafeToReload(ToolsConfig.Database oldDb, ToolsConfig.Database newDb,
                                    CommandsFile oldCommands, CommandsFile newCommands,
                                    RanksFile oldRanks, RanksFile newRanks,
                                    RegionsFile oldRegions, RegionsFile newRegions) {
        if (!Objects.equals(GSON.toJsonTree(oldDb), GSON.toJsonTree(newDb)))
            throw new IllegalArgumentException("MySql.json zmieniony: wymagany pełny restart serwera.");
        if (!sameCommands(oldCommands, newCommands))
            throw new IllegalArgumentException("Komendy/aliasy/uprawnienia zmienione: wymagany restart.");
        if (!oldRanks.ranks().equals(newRanks.ranks()))
            throw new IllegalArgumentException("Definicje rang zmienione poza /ranga: wymagany restart.");
        if (!GSON.toJsonTree(oldRegions).equals(GSON.toJsonTree(newRegions)))
            throw new IllegalArgumentException("Regions.json zmieniony: wymagany restart.");
    }

    private static boolean sameCommands(CommandsFile a, CommandsFile b) {
        return same(a.tools(), b.tools()) && same(a.ranga(), b.ranga())
                && same(a.region(), b.region()) && same(a.lokalizacje(), b.lokalizacje())
                && same(a.chat(), b.chat())
                && same(a.msg(), b.msg()) && same(a.reply(), b.reply())
                && same(a.tp(),b.tp()) && same(a.vanish(),b.vanish())
                && same(a.helpop(),b.helpop()) && same(a.gamemode(),b.gamemode())
                && same(a.fly(),b.fly()) && same(a.broadcast(),b.broadcast())
                && same(a.inventoryopen(),b.inventoryopen()) && same(a.speed(),b.speed());
    }

    private static boolean same(CommandsFile.Entry a, CommandsFile.Entry b) {
        return a.enabled() == b.enabled()
                && a.description().equals(b.description())
                && a.permission().equals(b.permission())
                && a.aliases().equals(b.aliases());
    }

    @Override
    public void close() {
        closed = true;
        io.shutdownNow();
    }
}
