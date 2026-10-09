package pl.lokos.tools.listeners;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.manager.RegionTeleportManager;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionFlag;

import java.util.concurrent.CompletionException;

/** Kliknięcia GUI mają sprawdzanego właściciela i pełną autoryzację przy akcji. */
public final class RegionMenuListener implements Listener {
    private final JavaPlugin plugin;
    private final RegionMenuFactory menus;
    private final RegionManager regions;
    private final RegionTeleportManager teleports;

    public RegionMenuListener(JavaPlugin plugin,RegionMenuFactory menus,RegionManager regions,RegionTeleportManager teleports) {
        this.plugin=plugin;
        this.menus=menus;this.regions=regions;this.teleports=teleports;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof RegionMenuFactory.Holder h)) return;
        event.setCancelled(true); // również hotbar, shift-click, drag z inventory gracza
        if(!(event.getWhoClicked() instanceof Player player) || !h.owner().equals(player.getUniqueId())) return;
        if(event.getClickedInventory()!=event.getView().getTopInventory()) return;
        String action=menus.action(event.getCurrentItem());
        if(action==null || action.equals("noop") || !regions.ready()) return;
        if(h.view()==RegionMenuFactory.View.LOCATIONS) {
            if(action.startsWith("page:")) {
                try {menus.locations(player,Integer.parseInt(action.substring(5)));}
                catch(NumberFormatException ignored){}
            } else if(action.startsWith("tp:")) {
                String name=action.substring(3);
                Region region=regions.index().byName(name);
                if(region==null || region.spawn()==null || !regions.canEnter(player,region)) {
                    Messages.error(player,"Ta lokalizacja nie jest dostępna.");
                } else {
                    plugin.getServer().getScheduler().runTask(plugin,()->{
                        if(player.isOnline()) {
                            player.closeInventory();
                            teleports.start(player,name);
                        }
                    });
                }
            }
        } else if(h.view()==RegionMenuFactory.View.EDIT) {
            if(!player.hasPermission("tools.region.admin")) {
                player.closeInventory();Messages.error(player,"Nie masz dostępu do edycji regionów.");return;
            }
            if(action.startsWith("flag:")) {
                Region region=regions.index().byName(h.region());
                if(region==null) {plugin.getServer().getScheduler().runTask(plugin,()->player.closeInventory());return;}
                RegionFlag flag=RegionFlag.valueOf(action.substring(5));
                Boolean current=region.flags().get(flag);
                Boolean next=current==null?true:current?false:null;
                regions.flag(region.name(),flag,next).whenComplete((none,error)-> {
                    if(!player.isOnline())return;
                    player.getServer().getScheduler().runTask(plugin,()-> {
                                if(!player.isOnline())return;
                                if(error!=null) {
                                    Messages.error(player,"Nie udało się zapisać reguły w MySQL.");
                                    player.closeInventory();
                                } else {
                                    menus.edit(player,region.name());
                                }
                            });
                });
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof RegionMenuFactory.Holder))return;
        if(event.getRawSlots().stream().anyMatch(slot->slot<event.getView().getTopInventory().getSize()))
            event.setCancelled(true);
    }
}
