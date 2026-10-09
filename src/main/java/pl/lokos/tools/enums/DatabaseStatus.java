package pl.lokos.tools.enums;

public enum DatabaseStatus {
    DISABLED, STARTING, READY, FAILED, CLOSING, CLOSED;

    public String displayName() {
        return switch (this) {
            case DISABLED -> "Wyłączona";
            case STARTING -> "Uruchamianie";
            case READY -> "Gotowe";
            case FAILED -> "Błąd";
            case CLOSING -> "Zamykanie";
            case CLOSED -> "Zamknięta";
        };
    }
}
