package pl.lokos.tools.enums;

public enum DatabaseStatus {
    DISABLED, STARTING, READY, FAILED, CLOSING, CLOSED;

    public String displayName() {
        return switch (this) {
            case DISABLED -> "Wylaczona";
            case STARTING -> "Uruchamianie";
            case READY -> "Gotowe";
            case FAILED -> "Blad";
            case CLOSING -> "Zamykanie";
            case CLOSED -> "Zamknieta";
        };
    }
}
