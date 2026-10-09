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
import pl.lokos.tools.permissions.LuckPermsBridge;
import pl.lokos.tools.manager.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import pl.lokos.tools.commands.RankCommand;
import java.util.concurrent.CompletionException;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ToolsCommand implements BasicCommand {
    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final PlayerRepository repository;
    private final PlayerDataManager playerData;
    private final String permission;
    private final MonitoringService monitoring;
    private final HotReloadService reload;
    private final LuckPermsBridge luckPerms;
    private final RankManager ranks;

    public ToolsCommand(JavaPlugin plugin, DatabaseManager database, PlayerRepository repository,
                        PlayerDataManager playerData, String permission,
                        MonitoringService monitoring, HotReloadService reload,
                        LuckPermsBridge luckPerms, RankManager ranks) {
        this.plugin = plugin;
        this.database = database;
        this.repository = repository;
        this.playerData = playerData;
        this.permission = permission;
        this.monitoring = monitoring;
        this.reload = reload;
        this.luckPerms = luckPerms;
        this.ranks = ranks;
    }

    @Override
    public String permission() {
        return permission;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0 || args[0].equalsIgnoreCase("pomoc") || args[0].equalsIgnoreCase("help")) {
            Messages.title(sender, "NARZĘDZIA SERWERA");
            Messages.line(sender, "&a/tools status &8- &7Stan połączenia bazy danych");
            Messages.line(sender, "&a/tools zdrowie &8- &7Pamięć, TPS, baza i wątki");
            Messages.line(sender, "&a/tools diagnostyka &8- &7Czasy pracy modułów");
            Messages.line(sender, "&a/tools przeladuj &8- &7Bezpieczne odświeżenie wyglądu i tekstów");
            Messages.line(sender, "&a/tools lp &8- &7Uprawnienia kontekstowe LuckPerms (opcjonalnie)");
            Messages.line(sender, "&a/tools ping &8- &7Czas odpowiedzi bazy danych");
            Messages.line(sender, "&a/tools stats <nick> &8- &7Statystyki gracza");
            Messages.line(sender, "&a/ranga lista &8- &7Lista dostępnych rang");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                String state = database == null ? "Wyłączona" : database.status().displayName();
                Messages.info(sender, "Baza danych: &a" + state
                        + "&7 | Graczy online: &a" + (playerData == null ? 0 : playerData.onlineCount()));
            }
            case "zdrowie" -> {
                MonitoringService.Health h = monitoring.snapshot();
                Messages.title(sender, "STAN SERWERA");
                Messages.info(sender, "TPS (1 min): &a" + String.format(Locale.ROOT, "%.2f", h.tps())
                        + " &8| &7Wątki JVM: &a" + h.threads());
                Messages.info(sender, "Pamięć: &a" + h.usedMb() + " / " + h.maxMb()
                        + " MiB &8(&a" + h.heapPercent() + "%&8)");
                Messages.info(sender, "Baza: &a" + h.databaseStatus()
                        + " &8(&7" + h.backend() + "&8) &8| &7Zadania: &a" + h.pluginTasks());
                Messages.info(sender, "SQL: &a" + h.queries() + " &7zapytań &8| &7Błędów: &c"
                        + h.sqlFailures() + " &8| &7Kolejka: &a" + h.sqlQueued()
                        + " &8| &7Oczekuje: &a" + h.sqlWaiting());
                Messages.info(sender, "Ostatnie zapytanie: &a" + h.lastSqlMs() + " ms");
            }
            case "diagnostyka" -> {
                MonitoringService.Health health = monitoring.snapshot();
                Messages.title(sender, "CZAS MODUŁÓW");
                if (health.modules().isEmpty()) Messages.info(sender, "Brak zebranych pomiarów.");
                for (MonitoringService.ModuleTiming timing : health.modules().stream().limit(8).toList()) {
                    Messages.info(sender, timing.name() + ": &a"
                            + String.format(Locale.ROOT, "%.3f", timing.meanMs())
                            + " ms średnio &8| &7max: &a" + timing.maxMs()
                            + " ms &8| &7ponad 50 ms: &a" + timing.slowCalls());
                }
                Messages.hint(sender, "Pomiary obejmują tylko własne zadania Tools; nie są pełnym profilerem TPS.");
            }
            case "lp" -> luckPermsCommand(sender, args);
            case "przeladuj" -> {
                Messages.info(sender, "Wczytuję i sprawdzam konfiguracje w tle...");
                reload.reload().whenComplete((message, error) -> respond(() -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null
                                ? error.getCause() : error;
                        Messages.error(sender, cause.getMessage() == null
                                ? "Nie udało się przeładować konfiguracji." : cause.getMessage());
                    } else {
                        Messages.success(sender, message);
                    }
                }));
            }
            case "ping" -> {
                if (repository == null) {
                    Messages.error(sender, "Baza MySQL jest wyłączona w konfiguracji.");
                    return;
                }
                Messages.info(sender, "Sprawdzanie połączenia z MySQL...");
                repository.ping().whenComplete((delay, error) -> respond(() -> {
                    if (error != null) {
                        Messages.error(sender, "Nie można połączyć się z MySQL. Sprawdź konsolę.");
                        plugin.getLogger().log(Level.WARNING, "Błąd testu MySQL", error);
                    } else {
                        Messages.success(sender, "Połączenie z bazą działa. Opóźnienie: &a" + delay + " ms");
                    }
                }));
            }
            case "stats" -> {
                if (args.length != 2) {
                    Messages.error(sender, "Brakuje nicku gracza.");
                    Messages.usage(sender, "/tools stats <nick>");
                    return;
                }
                if (repository == null) {
                    Messages.error(sender, "Baza MySQL jest wyłączona.");
                    return;
                }
                String name = args[1];
                repository.findByName(name).whenComplete((stats, error) -> respond(() -> {
                    if (error != null) {
                        Messages.error(sender, "Nie udało się pobrać statystyk.");
                        plugin.getLogger().log(Level.WARNING, "Błąd statystyk MySQL", error);
                    } else if (stats == null) {
                        Messages.error(sender, "Nie znaleziono gracza &c&n" + name + "&r&c w bazie.");
                    } else {
                        Messages.title(sender, "STATYSTYKI " + stats.name());
                        Messages.info(sender, "Wejścia: &a" + stats.joins());
                        Messages.info(sender, "Czas gry: &a" + PlayerDataHelper.formatPlaytime(stats.playtimeMs()));
                    }
                }));
            }
            default -> {
                Messages.error(sender, "Nieznana podkomenda: &c&n" + args[0] + "&r&c.");
                Messages.hint(sender, "Użyj &a/tools pomoc&7, aby zobaczyć polecenia.");
            }
        }
    }

    private void luckPermsCommand(CommandSender sender, String[] args) {
        if (luckPerms == null) {
            Messages.error(sender, "LuckPerms nie jest zainstalowany lub jego API nie jest dostępne.");
            Messages.hint(sender, "Komendy LP działają tylko przy zainstalowanym LuckPerms.");
            return;
        }
        if (args.length < 4) {
            Messages.title(sender, "LUCKPERMS — UPRAWNIENIA");
            Messages.line(sender, "&a/tools lp nadaj &7<nick> <uprawnienie> <czas|*> [świat]");
            Messages.line(sender, "&a/tools lp dziedzicz &7<nick> <grupa> <czas|*> [świat]");
            Messages.line(sender, "&a/tools lp sprawdz &7<nick> <uprawnienie> [świat]");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String nickname = args[2];
        String node = args[3];
        Player online = Bukkit.getPlayerExact(nickname);
        CompletableFuture<UUID> resolved;
        if (online != null) resolved = CompletableFuture.completedFuture(online.getUniqueId());
        else if (ranks != null) resolved = ranks.repository().findPlayer(nickname);
        else {
            Messages.error(sender, "Baza graczy nie jest dostępna.");
            return;
        }
        CompletableFuture<?> call;
        if (action.equals("sprawdz") && (args.length == 4 || args.length == 5)) {
            String world = args.length == 5 ? args[4] : null;
            call = resolved.thenCompose(uuid -> uuid == null ?
                    CompletableFuture.failedFuture(new IllegalArgumentException("Nie znaleziono gracza.")) :
                    luckPerms.hasPermission(uuid, node, world))
                    .thenAccept(allowed -> respond(() -> Messages.info(sender,
                            "LuckPerms: " + nickname + " &8→ &a" + node + " &8= "
                                    + (allowed ? "&aPozwolono" : "&cOdmówiono"))));
        } else if ((action.equals("nadaj") || action.equals("dziedzicz"))
                && (args.length == 5 || args.length == 6)) {
            Long expires;
            try { expires = RankCommand.parseTime(args[4]); }
            catch (IllegalArgumentException error) { Messages.error(sender, error.getMessage()); return; }
            String world = args.length == 6 ? args[5] : null;
            call = resolved.thenCompose(uuid -> {
                if (uuid == null)
                    return CompletableFuture.failedFuture(new IllegalArgumentException(
                            "Gracz musi najpierw wejść na serwer."));
                return action.equals("nadaj")
                        ? luckPerms.grantPermission(uuid, node, expires, world)
                        : luckPerms.inheritGroup(uuid, node, expires, world);
            }).thenRun(() -> respond(() -> Messages.success(sender,
                    "Zapisano w LuckPerms: &a" + nickname + " &8→ &a" + node)));
        } else {
            Messages.error(sender, "Nieprawidłowe argumenty komendy LuckPerms.");
            Messages.hint(sender, "Użyj &a/tools lp &7aby zobaczyć składnię.");
            return;
        }
        call.exceptionally(error -> {
            Throwable root = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause() : error;
            respond(() -> Messages.error(sender, root.getMessage() == null
                    ? "Nie udało się zapisać zmian w LuckPerms." : root.getMessage()));
            return null;
        });
    }

    private void respond(Runnable callback) {
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTask(plugin, callback);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().hasPermission(permission)) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("pomoc", "status", "ping", "stats", "zdrowie", "diagnostyka", "przeladuj", "lp").stream()
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
