package pl.lokos.tools.utils;

import org.bukkit.Bukkit;

public final class ThreadChecks {
    private ThreadChecks() {
    }

    public static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Operacja Bukkit API wymaga glownego watku serwera.");
        }
    }
}
