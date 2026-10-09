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

    public ToolsCommand(JavaPlugin plugin, DatabaseManager database, PlayerRepository repository,
                        PlayerDataManager playerData, String permission) {
        this.plugin = plugin;
        this.database = database;
        this.repository = repository;
        this.playerData = playerData;
        this.permission = permission;
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
            Messages.line(sender, "&a/tools status &8- &7Stan połączenia MySQL");
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

    private void respond(Runnable callback) {
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTask(plugin, callback);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("pomoc", "status", "ping", "stats").stream()
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
