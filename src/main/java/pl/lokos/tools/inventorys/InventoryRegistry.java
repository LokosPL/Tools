package pl.lokos.tools.inventorys;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.lokos.tools.utils.ThreadChecks;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class InventoryRegistry {
    private final Map<String, Function<Player, Inventory>> factories = new HashMap<>();

    public void register(String id, Function<Player, Inventory> factory) {
        ThreadChecks.requirePrimaryThread();
        if (factories.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(factory)) != null) {
            throw new IllegalArgumentException("GUI o identyfikatorze " + id + " jest juz zarejestrowane.");
        }
    }

    public boolean open(Player player, String id) {
        ThreadChecks.requirePrimaryThread();
        Function<Player, Inventory> factory = factories.get(id);
        if (factory == null) {
            return false;
        }
        player.openInventory(Objects.requireNonNull(factory.apply(player), "Fabryka GUI zwrocila null."));
        return true;
    }
}
