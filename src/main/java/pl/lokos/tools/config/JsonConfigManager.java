package pl.lokos.tools.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import pl.lokos.tools.variables.PluginConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDirectory;

    public JsonConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public ToolsConfig load() throws IOException {
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve(PluginConstants.CONFIG_FILE);
        if (Files.notExists(file)) {
            try (InputStream stream = JsonConfigManager.class.getResourceAsStream("/default-config.json")) {
                if (stream == null) {
                    throw new IOException("Brak default-config.json wewnatrz JAR.");
                }
                Files.write(file, stream.readAllBytes());
            }
        }
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ToolsConfig config = GSON.fromJson(reader, ToolsConfig.class);
            if (config == null) {
                throw new IllegalArgumentException("Pusty config.json.");
            }
            config.validate();
            return config;
        } catch (JsonParseException | IllegalArgumentException e) {
            throw new IOException("Bledny config.json: " + e.getMessage(), e);
        }
    }
}
