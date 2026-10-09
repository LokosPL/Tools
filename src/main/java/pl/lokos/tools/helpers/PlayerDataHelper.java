package pl.lokos.tools.helpers;

public final class PlayerDataHelper {
    private PlayerDataHelper() {
    }

    public static String formatPlaytime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long rest = seconds % 60L;
        return days + "d " + hours + "h " + minutes + "m " + rest + "s";
    }
}
