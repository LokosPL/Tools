package pl.lokos.tools.security;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;
import java.util.Set;
import java.util.UUID;

/**
 * Jedyna polityka autoryzacji komend Tools: bez zgadywania uprawnień przez
 * PermissionDefault i bez przypadkowego dziedziczenia wildcard z rangi 'gracz'.
 */
public final class ToolsAccess {
    private ToolsAccess() {}

    public static boolean allowed(boolean op, RankSnapshot snapshot, UUID uuid,
                                  String node, boolean administrative) {
        if (op) return true;
        if (snapshot==null || node==null || uuid==null) return false;
        RankSnapshot.Rank rank=snapshot.forPlayer(uuid);
        if (rank==null) return false; // brak wczytanej bazy = brak przywilejów
        boolean standard="gracz".equals(rank.name());
        if (administrative && standard) return false;
        // Domyślna ranga nie może odziedziczyć obcych/administracyjnych node'ów
        // nawet gdy ktoś omyłkowo zapisze je w Ranks.json.
        if (standard && !publicNode(node)) return false;
        Set<String> granted=snapshot.permissions().getOrDefault(rank.name(),Set.of());
        if (!standard && granted.contains("*")) return true;
        return granted.contains(node);
    }

    public static boolean publicNode(String node) {
        return "tools.event.info".equals(node) || "tools.eventy.use".equals(node)
                || "tools.skrzynie.use".equals(node) || "tools.klucze.use".equals(node)
                || "tools.spawn".equals(node)
                || "tools.lokalizacje".equals(node)
                || "tools.lokalizacje.instant".equals(node);
    }

    public static boolean allowed(Player player, RankManager ranks, String node,
                                  boolean administrative) {
        // OP zachowuje dostęp również podczas uruchamiania modułu SQL.
        return player.isOp() || (ranks != null && allowed(false,ranks.snapshot(),
                player.getUniqueId(),node,administrative));
    }

    public static boolean admin(CommandSender sender, RankManager ranks, String node) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender instanceof Player player && allowed(player,ranks,node,true);
    }

    public static boolean permitted(CommandSender sender, RankManager ranks, String node) {
        if (sender instanceof ConsoleCommandSender) return true;
        return sender instanceof Player player && allowed(player,ranks,node,false);
    }
}
