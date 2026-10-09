package pl.lokos.tools.database;

import java.util.Locale;

public enum DatabaseType {
    MYSQL, MARIADB, SQLITE;

    public static DatabaseType parse(String value) {
        if (value == null) throw new IllegalArgumentException("Nie ustawiono typu bazy.");
        try { return valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Obsługiwane typy baz: MYSQL, MARIADB, SQLITE.", error);
        }
    }

    public boolean sqlite() { return this == SQLITE; }
}
