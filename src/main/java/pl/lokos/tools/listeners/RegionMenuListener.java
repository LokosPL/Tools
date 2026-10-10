package pl.lokos.tools.listeners;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.*;
import pl.lokos.tools.region.*;

import java.util.*;
import java.util.concurrent.CompletionException;

/** Autoryzacja w kazdym kliknieciu, serializacja mutacji i bezpieczne odswiezenie menu. */
public final class RegionMenuListener implements Listener {
    private static final pl.lokos.tools.helpers.CommandMessages display = new pl.lokos.tools.helpers.CommandMessages("region");
    private final JavaPlugin plugin;
    private final RegionMenuFactory menus;
    private final String adminPermission;
    private final String locationsPermission;
    private final RegionManager regions;
    private final RegionTeleportManager teleports;
    private final RegionBorderPreview borders;
    private final Set<UUID> processing=new HashSet<>();

    public RegionMenuListener(JavaPlugin plugin,RegionMenuFactory menus,RegionManager regions,
            RegionTeleportManager teleports,RegionBorderPreview borders,String adminPermission,String locationsPermission){
        this.plugin=plugin;this.menus=menus;this.regions=regions;this.teleports=teleports;
        this.borders=borders;this.adminPermission=adminPermission;this.locationsPermission=locationsPermission;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof RegionMenuFactory.Holder holder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)
                || !holder.owner().equals(player.getUniqueId())
                || event.getClickedInventory()!=event.getView().getTopInventory()
                || !regions.ready() || processing.contains(player.getUniqueId()))return;
        String action=menus.action(event.getCurrentItem());
        if(action==null||action.equals("noop"))return;
        RegionMenuFactory.View view=holder.view();
        if(view==RegionMenuFactory.View.LOCATIONS || view==RegionMenuFactory.View.LOCATION_CHILDREN){
            if(!ToolsCommandVisibilityListener.allowed(player,regions.ranks(),locationsPermission)) {
                display.unknown(player);
                later(player,player::closeInventory);
                return;
            }
            if(action.startsWith("browse:")){
                String name=action.substring(7);
                later(player,()->menus.children(player,name,0));
            } else if(action.equals("locations")){
                later(player,()->menus.locations(player,0));
            } else if(action.startsWith("tp:")){
                String name=action.substring(3);
                Region r=regions.index().byName(name);
                if(r==null||r.spawn()==null||!regions.canEnter(player,r)){
                    display.error(player,"Lokalizacja niedostępna.");return;
                }
                later(player,()->{player.closeInventory();teleports.start(player,name);});
            } else if(action.startsWith("page:")){
                int page=parsePage(action);
                later(player,()->{
                    if(view==RegionMenuFactory.View.LOCATION_CHILDREN)
                        menus.children(player,holder.region(),page);
                    else menus.locations(player,page);
                });
            }
            return;
        }
        if(!player.hasPermission(adminPermission)){
            display.error(player,"Nie masz dostępu do edycji regionu.");
            later(player,player::closeInventory);
            return;
        }
        Region r=regions.index().byName(holder.region());
        if(r==null){later(player,player::closeInventory);return;}
        switch(action){
            case "flags" -> later(player,()->menus.flags(player,r.name(),0));
            case "ranks" -> later(player,()->menus.rankAccess(player,r.name(),0));
            case "back" -> later(player,()->menus.edit(player,r.name()));
            case "close" -> later(player,player::closeInventory);
            case "border" -> {
                borders.preview(player,r);
                later(player,player::closeInventory);
            }
            default -> {
                if(action.startsWith("page:")){
                    int next=parsePage(action);
                    later(player,()->{
                        if(view==RegionMenuFactory.View.FLAGS)menus.flags(player,r.name(),next);
                        else if(view==RegionMenuFactory.View.RANKS)menus.rankAccess(player,r.name(),next);
                    });
                }else if(view==RegionMenuFactory.View.FLAGS && action.startsWith("flag:")){
                    RegionFlag flag;
                    try {flag=RegionFlag.valueOf(action.substring(5));}
                    catch(IllegalArgumentException e){return;}
                    Boolean before=r.flags().get(flag);
                    Boolean after=RegionFlagCycle.next(before);
                    processing.add(player.getUniqueId());
                    regions.flag(r.name(),flag,after).whenComplete((v,error)->finish(player,holder,error,
                            ()->menus.flags(player,r.name(),holder.page())));
                }else if(view==RegionMenuFactory.View.RANKS && action.startsWith("rank:")){
                    String rank=action.substring(5);
                    if(!rank.equals("wszyscy") && !regions.ranks().snapshot().ranks().containsKey(rank)){
                        display.error(player,"Wybrana ranga już nie istnieje.");return;
                    }
                    processing.add(player.getUniqueId());
                    regions.entryRank(r.name(),rank.equals("wszyscy")?null:rank)
                            .whenComplete((v,error)->finish(player,holder,error,
                                    ()->menus.rankAccess(player,r.name(),holder.page())));
                }
            }
        }
    }

    private void finish(Player player,RegionMenuFactory.Holder previous,Throwable error,Runnable refresh){
        if(!plugin.isEnabled())return;
        plugin.getServer().getScheduler().runTask(plugin,()->{
            processing.remove(player.getUniqueId());
            if(!player.isOnline())return;
            if(error!=null){
                Throwable root=error;
                while(root instanceof CompletionException && root.getCause()!=null)root=root.getCause();
                display.error(player,"Nie zapisano ustawienia: "+root.getMessage());
                return;
            }
            if(player.getOpenInventory().getTopInventory().getHolder()==previous)refresh.run();
        });
    }
    private void later(Player p,Runnable action){
        plugin.getServer().getScheduler().runTask(plugin,()->{
            if(p.isOnline())action.run();
        });
    }
    private static int parsePage(String action){
        try{return Math.max(0,Math.min(10000,Integer.parseInt(action.substring(5))));}
        catch(NumberFormatException e){return 0;}
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof RegionMenuFactory.Holder
                && event.getRawSlots().stream().anyMatch(s->s<event.getView().getTopInventory().getSize()))
            event.setCancelled(true);
    }
}
