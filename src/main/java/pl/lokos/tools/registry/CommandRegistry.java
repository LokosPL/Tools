package pl.lokos.tools.registry;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.commands.ToolsCommand;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.database.DatabaseManager;
import pl.lokos.tools.database.PlayerRepository;
import pl.lokos.tools.manager.PlayerDataManager;

public final class CommandRegistry {
    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Wywoluj wylacznie podczas onEnable(), po zaladowaniu i walidacji konfiguracji.
    // Rejestracja Paper BasicCommand nie wymaga sekcji commands w plugin.yml.
    public void register(ToolsConfig.Commands configuration,
                         DatabaseManager database, PlayerRepository repository, PlayerDataManager playerData) {
        ToolsConfig.Commands.Tools tools = configuration.tools();
        if (!tools.enabled()) {
            plugin.getLogger().info("Komenda /tools wylaczona w config.json.");
            return;
        }

        if (plugin.getServer().getPluginManager().getPermission(tools.permission()) == null) {
            plugin.getServer().getPluginManager().addPermission(
                    new Permission(tools.permission(), PermissionDefault.OP));
        }

        plugin.registerCommand(
                "tools",
                tools.description(),
                tools.aliases(),
                new ToolsCommand(plugin, database, repository, playerData, tools.permission()));
    }
}
