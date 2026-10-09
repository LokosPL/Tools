package pl.lokos.tools.config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ToolsConfig {
    private int autosaveSeconds = 30;
    private Database database = new Database();
    private Commands commands = new Commands();

    public int autosaveSeconds() {
        return autosaveSeconds;
    }

    public Database database() {
        return database;
    }

    public Commands commands() {
        return commands;
    }

    public void validate() {
        if (autosaveSeconds < 5 || autosaveSeconds > 3600) {
            throw new IllegalArgumentException("autosaveSeconds musi miec wartosc 5-3600.");
        }
        if (database == null) {
            throw new IllegalArgumentException("Brakuje sekcji database.");
        }
        if (commands == null) {
            throw new IllegalArgumentException("Brakuje sekcji commands.");
        }
        database.validate();
        commands.validate();
    }

    public static final class Commands {
        private Tools tools = new Tools();

        public Tools tools() {
            return tools;
        }

        private void validate() {
            if (tools == null) {
                throw new IllegalArgumentException("Brakuje sekcji commands.tools.");
            }
            tools.validate();
        }

        public static final class Tools {
            private boolean enabled = true;
            private String description = "Informacje, status i statystyki Tools";
            private List<String> aliases = List.of();
            private String permission = "tools.admin";

            public boolean enabled() { return enabled; }
            public String description() { return description; }
            public List<String> aliases() { return List.copyOf(aliases); }
            public String permission() { return permission; }

            private void validate() {
                if (description == null || description.isBlank()) {
                    throw new IllegalArgumentException("commands.tools.description nie moze byc pusty.");
                }
                if (permission == null || !permission.matches("[a-z0-9_.-]+")) {
                    throw new IllegalArgumentException("Nieprawidlowy commands.tools.permission.");
                }
                if (aliases == null) {
                    throw new IllegalArgumentException("Brakuje commands.tools.aliases.");
                }
                Set<String> used = new HashSet<>();
                for (String alias : aliases) {
                    if (alias == null || !alias.matches("[a-zA-Z0-9_-]+")
                            || alias.equalsIgnoreCase("tools")
                            || !used.add(alias.toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("Nieprawidlowy lub powtorzony alias komendy Tools: " + alias);
                    }
                }
            }
        }
    }

    public static final class Database {
        private boolean enabled = true;
        private boolean createDatabaseIfMissing = true;
        private String host = "127.0.0.1";
        private int port = 3306;
        private String database = "tools";
        private String username = "root";
        private String password = "";
        private String sslMode = "PREFERRED";
        private int poolSize = 6;
        private int connectionTimeoutMs = 5000;

        public boolean enabled() { return enabled; }
        public boolean createDatabaseIfMissing() { return createDatabaseIfMissing; }
        public String host() { return host; }
        public int port() { return port; }
        public String database() { return database; }
        public String username() { return username; }
        public String password() { return password; }
        public String sslMode() { return sslMode; }
        public int poolSize() { return poolSize; }
        public int connectionTimeoutMs() { return connectionTimeoutMs; }

        public String resolvedPassword() {
            if (password == null) {
                return "";
            }
            if (password.startsWith("${") && password.endsWith("}")) {
                String environmentName = password.substring(2, password.length() - 1);
                return System.getenv().getOrDefault(environmentName, "");
            }
            return password;
        }

        private void validate() {
            if (!enabled) {
                return;
            }
            if (host == null || host.isBlank() || host.contains("/") || host.contains("?")) {
                throw new IllegalArgumentException("Nieprawidlowy database.host.");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Nieprawidlowy port MySQL.");
            }
            if (database == null || !database.matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException("Nazwa bazy musi zawierac tylko litery, cyfry i '_'.");
            }
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Pusty database.username.");
            }
            if (password == null || (password.startsWith("${") && password.endsWith("}") && resolvedPassword().isEmpty())) {
                throw new IllegalArgumentException("Ustaw zmienna srodowiskowa hasla MySQL.");
            }
            if (!Set.of("DISABLED", "PREFERRED", "REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY").contains(sslMode)) {
                throw new IllegalArgumentException("Nieprawidlowy database.sslMode.");
            }
            if (poolSize < 1 || poolSize > 32 || connectionTimeoutMs < 250 || connectionTimeoutMs > 30000) {
                throw new IllegalArgumentException("Nieprawidlowe ustawienia puli polaczen.");
            }
        }
    }
}
