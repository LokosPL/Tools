package pl.lokos.tools.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import pl.lokos.tools.manager.PlayerDataManager;

public final class AutosaveTask extends BukkitRunnable {
    private final PlayerDataManager playerData;

    public AutosaveTask(PlayerDataManager playerData) {
        this.playerData = playerData;
    }

    @Override
    public void run() {
        // Zbieramy tylko dane z Bukkit na glownym watku.
        // Operacje JDBC sa wykonywane przez DatabaseManager poza tym watkiem.
        playerData.autosave();
    }
}
