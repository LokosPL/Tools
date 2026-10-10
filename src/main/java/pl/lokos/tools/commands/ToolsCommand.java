package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.helpers.PlayerDataHelper;
import pl.lokos.tools.manager.PlayerDataManager;
import pl.lokos.tools.diagnostics.MonitoringService;
import pl.lokos.tools.config.HotReloadService;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import pl.lokos.tools.config.CommandTextRegistry;
import java.util.concurrent.CompletionException;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ToolsCommand implements BasicCommand {
    private static final pl.lokos.tools.helpers.CommandMessages display = new pl.lokos.tools.helpers.CommandMessages("tools");
    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final PlayerRepository repository;
    private final PlayerDataManager playerData;
    private final String permission;
    private final MonitoringService monitoring;
    private final HotReloadService reload;
    private final RankManager ranks;

    public ToolsCommand(JavaPlugin plugin, DatabaseManager database, PlayerRepository repository,
                        PlayerDataManager playerData, String permission,
                        MonitoringService monitoring, HotReloadService reload, RankManager ranks) {
        this.plugin = plugin;
        this.database = database;
        this.repository = repository;
        this.playerData = playerData;
        this.permission = permission;
        this.monitoring = monitoring;
        this.reload = reload;
        this.ranks = ranks;
    }

    @Override
    public String permission() {
        return permission;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (!ToolsAccess.admin(sender,ranks,permission)) {
            display.unknown(sender);
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("pomoc") || args[0].equalsIgnoreCase("help")) {
            CommandTextRegistry.help(sender,"tools");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                String state = database == null ? "Wyłączona" : database.status().displayName();
                display.info(sender, "Baza danych: &a" + state
                        + "&7 | Graczy online: &a" + (playerData == null ? 0 : playerData.onlineCount()));
            }
            case "zdrowie" -> {
                MonitoringService.Health h = monitoring.snapshot();
                display.title(sender, "STAN SERWERA");
                display.info(sender, "TPS (1 min): &a" + String.format(Locale.ROOT, "%.2f", h.tps())
                        + " &8| &7Wątki JVM: &a" + h.threads());
                display.info(sender, "Pamięć: &a" + h.usedMb() + " / " + h.maxMb()
                        + " MiB &8(&a" + h.heapPercent() + "%&8)");
                display.info(sender, "Baza: &a" + h.databaseStatus()
                        + " &8(&7" + h.backend() + "&8) &8| &7Zadania: &a" + h.pluginTasks());
                display.info(sender, "SQL: &a" + h.queries() + " &7zapytań &8| &7Błędów: &c"
                        + h.sqlFailures() + " &8| &7Kolejka: &a" + h.sqlQueued()
                        + " &8| &7Oczekuje: &a" + h.sqlWaiting());
                display.info(sender, "Ostatnie zapytanie: &a" + h.lastSqlMs() + " ms");
            }
            case "diagnostyka" -> {
                MonitoringService.Health health = monitoring.snapshot();
                display.title(sender, "CZAS MODUŁÓW");
                if (health.modules().isEmpty()) display.info(sender, "Brak zebranych pomiarów.");
                for (MonitoringService.ModuleTiming timing : health.modules().stream().limit(8).toList()) {
                    display.info(sender, timing.name() + ": &a"
                            + String.format(Locale.ROOT, "%.3f", timing.meanMs())
                            + " ms średnio &8| &7max: &a" + timing.maxMs()
                            + " ms &8| &7ponad 50 ms: &a" + timing.slowCalls());
                }
                display.hint(sender, "Pomiary obejmują tylko własne zadania Tools; nie są pełnym profilerem TPS.");
            }
            case "przeladuj" -> {
                display.info(sender, "Wczytuję i sprawdzam konfiguracje w tle...");
                reload.reload().whenComplete((message, error) -> respond(() -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null
                                ? error.getCause() : error;
                        display.error(sender, cause.getMessage() == null
                                ? "Nie udało się przeładować konfiguracji." : cause.getMessage());
                    } else {
                        display.success(sender, message);
                    }
                }));
            }
            case "ping" -> {
                if (repository == null) {
                    display.error(sender, "Baza MySQL jest wyłączona w konfiguracji.");
                    return;
                }
                display.info(sender, "Sprawdzanie połączenia z MySQL...");
                repository.ping().whenComplete((delay, error) -> respond(() -> {
                    if (error != null) {
                        display.error(sender, "Nie można połączyć się z MySQL. Sprawdź konsolę.");
                        plugin.getLogger().log(Level.WARNING, "Błąd testu MySQL", error);
                    } else {
                        display.success(sender, "Połączenie z bazą działa. Opóźnienie: &a" + delay + " ms");
                    }
                }));
            }
            case "stats" -> {
                if (args.length != 2) {
                    display.error(sender, "Brakuje nicku gracza.");
                    display.usage(sender, "/tools stats <nick>");
                    return;
                }
                if (repository == null) {
                    display.error(sender, "Baza MySQL jest wyłączona.");
                    return;
                }
                String name = args[1];
                repository.findByName(name).whenComplete((stats, error) -> respond(() -> {
                    if (error != null) {
                        display.error(sender, "Nie udało się pobrać statystyk.");
                        plugin.getLogger().log(Level.WARNING, "Błąd statystyk MySQL", error);
                    } else if (stats == null) {
                        display.error(sender, "Nie znaleziono gracza &c&n" + name + "&r&c w bazie.");
                    } else {
                        display.title(sender, "STATYSTYKI " + stats.name());
                        display.info(sender, "Wejścia: &a" + stats.joins());
                        display.info(sender, "Czas gry: &a" + PlayerDataHelper.formatPlaytime(stats.playtimeMs()));
                    }
                }));
            }
            default -> {
                display.error(sender, "Nieznana podkomenda: &c&n" + args[0] + "&r&c.");
                display.hint(sender, "Użyj &a/tools pomoc&7, aby zobaczyć polecenia.");
            }
        }
    }

    private void respond(Runnable callback) {
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTask(plugin, callback);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!ToolsAccess.admin(source.getSender(),ranks,permission)) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("pomoc", "status", "ping", "stats", "zdrowie", "diagnostyka", "przeladuj").stream()
                    .filter(option -> option.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }
}
