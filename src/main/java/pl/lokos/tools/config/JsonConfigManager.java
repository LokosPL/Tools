package pl.lokos.tools.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Uniwersalny loader konfiguracji JSON: wartosci domyslne sa definiowane
 * WYLACZNIE w klasach Java, nie w plikach resources.
 *
 * Tworzy brakujacy plik JSON, a przy kolejnych startach uzupelnia jedynie
 * brakujace klucze domyslnymi wartosciami z klasy Java. Istniejace ustawienia
 * administratora i nieznane klucze sa zachowywane.
 */
public final class JsonConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path dataDirectory;

    public JsonConfigManager(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
    }

    public <T> T load(String fileName, Class<T> type, Supplier<T> defaultsFactory,
                      Consumer<T> validator) throws IOException {
        return load(fileName, type, defaultsFactory, validator, target -> { });
    }

    /**
     * Migrator dostaje kopie pliku JSON. Wynik zapisujemy tylko po walidacji.
     */
    public <T> T load(String fileName, Class<T> type, Supplier<T> defaultsFactory,
                      Consumer<T> validator, Consumer<JsonObject> migration) throws IOException {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9_-]+\\.json")) {
            throw new IllegalArgumentException("Nieprawidlowa nazwa pliku konfiguracyjnego.");
        }
        Objects.requireNonNull(type);
        Objects.requireNonNull(defaultsFactory);
        Objects.requireNonNull(validator);
        Objects.requireNonNull(migration);
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve(fileName);
        try {
            T defaults = Objects.requireNonNull(defaultsFactory.get(), "Fabryka konfiguracji zwrocila null.");
            validator.accept(defaults);
            JsonObject javaDefaults = GSON.toJsonTree(defaults).getAsJsonObject();

            if (Files.notExists(file)) {
                writeAtomically(file, javaDefaults);
                return defaults;
            }

            JsonObject existing;
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("Korzeniem " + fileName + " musi byc obiekt JSON.");
                }
                existing = element.getAsJsonObject();
            }

            // Dodajemy nowe opcje z Java bez nadpisywania ustawien na dysku.
            JsonObject merged = existing.deepCopy();
            migration.accept(merged);
            boolean changed = !merged.equals(existing);
            changed |= mergeMissing(javaDefaults, merged);
            T config = Objects.requireNonNull(GSON.fromJson(merged, type), "Pusta konfiguracja.");
            validator.accept(config);

            // Nie dotykamy pliku, jesli nic sie nie zmienilo; blad walidacji
            // nie powoduje uszkodzenia ani nadpisania istniejacego JSON.
            if (changed) {
                writeAtomically(file, merged);
            }
            return config;
        } catch (JsonParseException | IllegalArgumentException | IllegalStateException error) {
            throw new IOException("Niepoprawny " + fileName + ": " + error.getMessage(), error);
        }
    }

    private static boolean mergeMissing(JsonObject defaults, JsonObject target) {
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement fallback = entry.getValue();
            if (!target.has(key)) {
                target.add(key, fallback.deepCopy());
                changed = true;
            } else if (fallback.isJsonObject() && target.get(key).isJsonObject()) {
                changed |= mergeMissing(fallback.getAsJsonObject(), target.getAsJsonObject(key));
            }
        }
        return changed;
    }

    private static void writeAtomically(Path file, JsonObject json) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), ".tools-config-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
