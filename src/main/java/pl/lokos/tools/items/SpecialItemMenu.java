package pl.lokos.tools.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.GuiTheme;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.*;

/** GUI admina z własnym InventoryHolder — żadne kliknięcie gracza nie symuluje wydania. */
public final class SpecialItemMenu implements Listener {
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private final UUID viewer,recipient;
        private final Map<Integer,String> ids=new HashMap<>();
        private Holder(UUID viewer,UUID recipient){this.viewer=viewer;this.recipient=recipient;}
        @Override public Inventory getInventory(){return inventory;}
    }
    private final JavaPlugin plugin;
    private final SpecialItemService items;
    private final RankManager ranks;
    private final String permission;
    public SpecialItemMenu(JavaPlugin plugin,SpecialItemService items,RankManager ranks,String permission){
        this.plugin=plugin;this.items=items;this.ranks=ranks;this.permission=permission;
    }
    public void open(Player viewer,Player recipient){
        if(!ToolsAccess.allowed(viewer,ranks,permission,true)){
            Messages.unknown(viewer);return;
        }
        Holder holder=new Holder(viewer.getUniqueId(),recipient.getUniqueId());
        Inventory inv=Bukkit.createInventory(holder,54,
                Colors.color(GuiTheme.title("PRZEDMIOTY EVENTOWE")));
        holder.inventory=inv;
        GuiTheme.frame(inv);
        int i=0;
        for(var entry:items.config().items().entrySet()){
            if(!entry.getValue().enabled()||i>=28)continue;
            ItemStack icon=items.create(entry.getKey());
            ItemMeta meta=icon.getItemMeta();
            List<Component> lore=new ArrayList<>(meta.hasLore()?meta.lore():List.of());
            lore.add(Colors.color(" "));
            lore.add(Colors.color("&#70D6E8» Lewy klik: &7wydaj 1 sztukę"));
            lore.add(Colors.color("&#70D6E8» Prawy klik: &7wydaj 4 sztuki"));
            lore.add(Colors.color("&#A8A8B7Odbiorca: &#FFD166"+recipient.getName()));
            meta.lore(lore);
            icon.setItemMeta(meta);
            int slot=10+(i/7)*9+(i%7);
            inv.setItem(slot,icon);
            holder.ids.put(slot,entry.getKey());
            i++;
        }
        ItemStack close=new ItemStack(Material.BARRIER);
        ItemMeta closeMeta=close.getItemMeta();
        closeMeta.displayName(Colors.color("&#FF727F✘ Zamknij"));
        closeMeta.lore(GuiTheme.lore("&#70D6E8» Kliknij, aby wrócić do gry."));
        close.setItemMeta(closeMeta);
        inv.setItem(49,close);
        viewer.openInventory(inv);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof Holder holder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player viewer)
                ||!viewer.getUniqueId().equals(holder.viewer)
                ||event.getClickedInventory()!=event.getView().getTopInventory())return;
        if(!ToolsAccess.allowed(viewer,ranks,permission,true)){
            Messages.unknown(viewer);return;
        }
        if(event.getRawSlot()==49){
            // Zamykanie InventoryClickEvent odraczamy do następnego ticka.
            plugin.getServer().getScheduler().runTask(plugin,()->{
                if(viewer.isOnline())viewer.closeInventory();
            });
            return;
        }
        String id=holder.ids.get(event.getRawSlot());
        if(id==null)return;
        Player recipient=Bukkit.getPlayer(holder.recipient);
        if(recipient==null || !recipient.isOnline()){
            Messages.error(viewer,"Odbiorca opuścił serwer.");return;
        }
        int amount=event.isRightClick()?4:1;
        int available=0;
        for(ItemStack slot:recipient.getInventory().getStorageContents())
            if(slot==null||slot.getType().isAir())available++;
        if(available<amount){Messages.error(viewer,"Odbiorca potrzebuje "+amount+" wolnych miejsc.");return;}
        for(int n=0;n<amount;n++)recipient.getInventory().addItem(items.create(id));
        Messages.success(viewer,"Przekazano "+amount+" szt. "+id+" do "+recipient.getName()+".");
        if(viewer!=recipient)Messages.info(recipient,"Otrzymano przedmiot eventowy: "+id+".");
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof Holder)event.setCancelled(true);
    }
}
