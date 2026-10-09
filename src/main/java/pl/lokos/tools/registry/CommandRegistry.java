package pl.lokos.tools.registry;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.commands.ToolsCommand;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.manager.PlayerDataManager;

import java.util.Objects;

public final class CommandRegistry {
    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(DatabaseManager database, PlayerRepository repository, PlayerDataManager playerData) {
        PluginCommand command = Objects.requireNonNull(plugin.getCommand("tools"), "Brak komendy tools w plugin.yml.");
        ToolsCommand handler = new ToolsCommand(plugin, database, repository, playerData);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }
}
