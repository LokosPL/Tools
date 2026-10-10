package pl.lokos.tools.commands;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;

import java.util.*;

/**
 * Ochrona otwartego ekwipunku: utrata permisji podczas sesji blokuje dalsze
 * kliknięcia oraz drag. Nie kopiujemy ekwipunku, więc zmiany są rzeczywiste.
 */
public final class InventoryAudit implements Listener {
    private record Opened(Inventory inventory,boolean ender) {}
    private final Map<UUID,Opened> opened=new HashMap<>();
    private final RankManager ranks;
    private final JavaPlugin plugin;
    public InventoryAudit(JavaPlugin plugin,RankManager ranks){
        this.plugin=plugin;this.ranks=ranks;
    }
    public void track(Player viewer,Inventory top,boolean ender){
        opened.put(viewer.getUniqueId(),new Opened(top,ender));
    }
    private boolean revoke(Player player,Inventory top){
        Opened active=opened.get(player.getUniqueId());
        if(active==null||active.inventory()!=top)return false;
        boolean allowed=ToolsAccess.allowed(player,ranks,"tools.inventoryopen",false)
                && (!active.ender()||ToolsAccess.allowed(player,ranks,
                "tools.inventoryopen.enderchest",false));
        if(allowed)return false;
        opened.remove(player.getUniqueId());
        // Paper ostrzega przed closeInventory() wewnątrz InventoryClickEvent.
        // Odroczenie zamknięcia nie osłabia blokady: zdarzenie już jest anulowane.
        plugin.getServer().getScheduler().runTask(plugin,()->{
            if(player.isOnline() && player.getOpenInventory().getTopInventory()==top)
                player.closeInventory();
        });
        Messages.unknown(player);
        return true;
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player p && revoke(p,event.getView().getTopInventory()))
            event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event){
        if(event.getWhoClicked() instanceof Player p && revoke(p,event.getView().getTopInventory()))
            event.setCancelled(true);
    }
    @EventHandler public void close(InventoryCloseEvent event){
        if(event.getPlayer() instanceof Player p)opened.remove(p.getUniqueId());
    }
    @EventHandler public void leave(PlayerQuitEvent event){opened.remove(event.getPlayer().getUniqueId());}
}
