package pl.lokos.tools.registry;

import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.config.ToolsConfigMigration;
import pl.lokos.tools.variables.PluginConstants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Centralny punkt rejestracji i wczytywania konfiguracji.
 * Dodajac nowa konfiguracje:
 * 1. utworz klase Java z polami i domyslnymi wartosciami,
 * 2. zaladuj ja tutaj przez JsonConfigManager.load(),
 * 3. korzystaj z gotowego obiektu dopiero po loadAll() w onEnable().
 */
public final class ConfigRegistry {
    private final JsonConfigManager json;
    private ToolsConfig tools;

    public ConfigRegistry(Path pluginDirectory) {
        this.json = new JsonConfigManager(pluginDirectory);
    }

    public void loadAll() throws IOException {
        tools = json.load(PluginConstants.CONFIG_FILE, ToolsConfig.class, ToolsConfig::new, ToolsConfig::validate,
                ToolsConfigMigration::apply);
    }

    public ToolsConfig tools() {
        return Objects.requireNonNull(tools, "Konfiguracje nie zostaly jeszcze wczytane.");
    }
}
