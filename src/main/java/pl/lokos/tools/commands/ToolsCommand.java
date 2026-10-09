package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.helpers.PlayerDataHelper;
import pl.lokos.tools.manager.PlayerDataManager;
import pl.lokos.tools.variables.PluginConstants;

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
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(PluginConstants.PREFIX + "Komendy: /tools status, /tools ping, /tools stats <nick>");
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                String state = database == null ? "DISABLED" : database.status().name();
                sender.sendMessage(PluginConstants.PREFIX + "MySQL: §e" + state +
                        "§7 | sesje online: §e" + (playerData == null ? 0 : playerData.onlineCount()));
            }
            case "ping" -> {
                if (repository == null) {
                    sender.sendMessage(PluginConstants.PREFIX + "§cMySQL jest wylaczony w config.json.");
                    return;
                }
                sender.sendMessage(PluginConstants.PREFIX + "Sprawdzam MySQL...");
                repository.ping().whenComplete((latency, error) -> respond(sender, () -> {
                    if (error != null) {
                        sender.sendMessage(PluginConstants.PREFIX + "§cBlad polaczenia MySQL. Zobacz logi.");
                        plugin.getLogger().log(Level.WARNING, "MySQL ping failed", error);
                    } else {
                        sender.sendMessage(PluginConstants.PREFIX + "MySQL odpowiada: §a" + latency + " ms");
                    }
                }));
            }
            case "stats" -> {
                if (args.length != 2) {
                    sender.sendMessage(PluginConstants.PREFIX + "Uzycie: /tools stats <nick>");
                    return;
                }
                if (repository == null) {
                    sender.sendMessage(PluginConstants.PREFIX + "§cMySQL jest wylaczony.");
                    return;
                }
                String name = args[1];
                repository.findByName(name).whenComplete((stats, error) -> respond(sender, () -> {
                    if (error != null) {
                        sender.sendMessage(PluginConstants.PREFIX + "§cNie udalo sie pobrac statystyk.");
                        plugin.getLogger().log(Level.WARNING, "MySQL statistics query failed", error);
                    } else if (stats == null) {
                        sender.sendMessage(PluginConstants.PREFIX + "Brak danych dla " + name + ".");
                    } else {
                        sender.sendMessage(PluginConstants.PREFIX + "§b" + stats.name() +
                                "§7 | wejscia: §e" + stats.joins() +
                                "§7 | czas gry: §e" + PlayerDataHelper.formatPlaytime(stats.playtimeMs()));
                    }
                }));
            }
            default -> sender.sendMessage(PluginConstants.PREFIX + "Nieznana komenda. /tools help");
        }
    }

    private void respond(CommandSender sender, Runnable callback) {
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, callback);
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("help", "status", "ping", "stats").stream()
                    .filter(option -> option.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
