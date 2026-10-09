package pl.lokos.tools.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Migracja wylacznie niezmienionego, domyslnego presetu z Tools 1.0.2.
 * Ręcznie skonfigurowane dane polaczenia nigdy nie sa nadpisywane.
 */
public final class ToolsConfigMigration {
    private ToolsConfigMigration() {}

    public static void apply(JsonObject root) {
        JsonElement element = root.get("database");
        if (element == null || !element.isJsonObject()) {
            return;
        }

        JsonObject database = element.getAsJsonObject();
        if (matches(database, "enabled", false)
                && matches(database, "host", "127.0.0.1")
                && matches(database, "port", 3306)
                && matches(database, "database", "tools")
                && matches(database, "username", "tools")
                && matches(database, "password", "${TOOLS_DB_PASSWORD}")
                && matches(database, "sslMode", "PREFERRED")
                && matches(database, "poolSize", 6)
                && matches(database, "connectionTimeoutMs", 5000)) {
            database.addProperty("enabled", true);
            database.addProperty("username", "root");
            database.addProperty("password", "");
            database.addProperty("createDatabaseIfMissing", true);
        }
    }

    private static boolean matches(JsonObject object, String key, boolean value) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isBoolean()
                && object.get(key).getAsBoolean() == value;
    }

    private static boolean matches(JsonObject object, String key, int value) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                && object.get(key).getAsInt() == value;
    }

    private static boolean matches(JsonObject object, String key, String value) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isString()
                && object.get(key).getAsString().equals(value);
    }
}
