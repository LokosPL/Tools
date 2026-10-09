package pl.lokos.tools.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.inventorys.RankMenuFactory;
import pl.lokos.tools.manager.RankManager;

import java.util.*;
import java.util.concurrent.CompletionException;

/** Zweryfikowane klikniecia GUI rang z odswiezeniem po atomowym zapisie JSON. */
public final class RankMenuListener implements Listener {
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final RankMenuFactory menus;
    private final String adminPermission;
    private final Set<UUID> pending=new HashSet<>();
    public RankMenuListener(JavaPlugin plugin,RankManager ranks,RankMenuFactory menus,String permission) {
        this.plugin=plugin;this.ranks=ranks;this.menus=menus;this.adminPermission=permission;
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof RankMenuFactory.Holder h))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p) || !h.owner().equals(p.getUniqueId())
                || event.getClickedInventory()!=event.getView().getTopInventory()
                || pending.contains(p.getUniqueId()))return;
        if(!p.hasPermission(adminPermission)){Messages.error(p,"Brak uprawnień.");later(p,p::closeInventory);return;}
        String action=menus.action(event.getCurrentItem());
        if(action==null||action.equals("noop"))return;
        if(action.startsWith("page:")){
            int page;
            try{page=Integer.parseInt(action.substring(5));}
            catch(NumberFormatException e){return;}
            int finalPage=Math.max(0,Math.min(page,10000));
            later(p,()->{
                if(h.view()==RankMenuFactory.View.LIST)menus.list(p,finalPage);
                else if(h.view()==RankMenuFactory.View.PERMISSIONS)menus.permissions(p,h.rank(),finalPage);
            });
        }else if(action.startsWith("rank:") && h.view()==RankMenuFactory.View.LIST){
            String name=action.substring(5);
            if(ranks.snapshot().ranks().containsKey(name))later(p,()->menus.details(p,name));
        }else if(action.equals("perms") && h.view()==RankMenuFactory.View.DETAILS){
            later(p,()->menus.permissions(p,h.rank(),0));
        }else if(action.equals("back")){
            later(p,()->{
                if(h.view()==RankMenuFactory.View.PERMISSIONS)menus.details(p,h.rank());
                else menus.list(p,0);
            });
        }else if(action.startsWith("toggle:") && h.view()==RankMenuFactory.View.PERMISSIONS){
            String permission=action.substring(7);
            if(!menus.permissions().contains(permission) || !ranks.snapshot().ranks().containsKey(h.rank()))return;
            pending.add(p.getUniqueId());
            ranks.togglePermission(h.rank(),permission).whenComplete((v,error)->{
                if(!plugin.isEnabled())return;
                plugin.getServer().getScheduler().runTask(plugin,()->{
                    pending.remove(p.getUniqueId());
                    if(!p.isOnline())return;
                    if(error!=null){
                        Throwable cause=error;
                        while(cause instanceof CompletionException && cause.getCause()!=null)cause=cause.getCause();
                        Messages.error(p,"Nie zapisano uprawnienia: "+cause.getMessage());
                    }else if(p.getOpenInventory().getTopInventory().getHolder()==h){
                        menus.permissions(p,h.rank(),h.page());
                    }
                });
            });
        }
    }
    private void later(Player p,Runnable run){
        plugin.getServer().getScheduler().runTask(plugin,()->{if(p.isOnline())run.run();});
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent e){
        if(e.getView().getTopInventory().getHolder() instanceof RankMenuFactory.Holder
                && e.getRawSlots().stream().anyMatch(slot->slot<e.getView().getTopInventory().getSize()))
            e.setCancelled(true);
    }
}
