package pl.lokos.tools.whitelist;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.GuiTheme;

import java.util.*;

/** GUI i akcje trzymane w InventoryHolder, bez interpretacji displayName. */
public final class WhitelistMenu {
    public static final class Holder implements InventoryHolder {
        private final UUID viewer;
        private final int page;
        private final Map<Integer,String> actions = new HashMap<>();
        private Inventory inventory;
        Holder(UUID viewer, int page) { this.viewer=viewer; this.page=page; }
        public UUID viewer(){return viewer;}
        public int page(){return page;}
        public String action(int slot){return actions.get(slot);}
        public Inventory getInventory(){return inventory;}
    }
    private final WhitelistService service;
    public WhitelistMenu(WhitelistService service) { this.service=service; }

    public void open(Player player, int requested) {
        List<String> users = new ArrayList<>(service.state().players());
        users.sort(String.CASE_INSENSITIVE_ORDER);
        int max = Math.max(0, (users.size() - 1) / 27);
        int page = Math.min(max, Math.max(0, requested));
        Holder holder = new Holder(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(holder, 54, Colors.color(GuiTheme.title("WHITELIST • " + (page+1))));
        holder.inventory = inv;
        GuiTheme.frame(inv);
        inv.setItem(4, icon(service.state().enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                service.state().enabled() ? "&a&lWhitelist włączona" : "&7Whitelist wyłączona",
                "&8Tryb: &f" + service.state().mode(), "&7Kliknij, aby zmienić stan"));
        holder.actions.put(4, service.state().enabled() ? "disable" : "mode:PRACE_TECHNICZNE");
        WhitelistMode[] modes = WhitelistMode.values();
        for (int i=0;i<modes.length;i++) {
            WhitelistMode m = modes[i];
            inv.setItem(i+9,icon(m.name().equals(service.state().mode()) && service.state().enabled()
                    ? Material.EMERALD : Material.PAPER,"&#85CFFF"+m.title(),
                    "&7Włącz serwer w tym trybie.", "&aKliknij, aby wybrać"));
            holder.actions.put(i+9,"mode:"+m.name());
        }
        for (int i=0;i<Math.min(27,users.size()-page*27);i++) {
            String name=users.get(page*27+i);
            ItemStack head = icon(Material.PLAYER_HEAD, "&#78CFFF" + name, "&7Na liście dozwolonych.", "&cKliknij, aby usunąć");
            if (head.getItemMeta() instanceof SkullMeta skull) {
                // Samo przypisanie OfflinePlayer nie odpytuje API Mojang synchronicznie.
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(name));
                head.setItemMeta(skull);
            }
            inv.setItem(18+i,head);
            holder.actions.put(18+i,"remove:"+name);
        }
        if(page>0) { inv.setItem(45,icon(Material.ARROW,"&7Poprzednia strona"));holder.actions.put(45,"page:"+(page-1));}
        if(page<max){inv.setItem(53,icon(Material.ARROW,"&7Następna strona"));holder.actions.put(53,"page:"+(page+1));}
        inv.setItem(49,icon(Material.BOOK,"&#85CFFF&lGracze: &f"+users.size(),
                "&7Dodawanie: &f/whitelist dodaj <nick>",
                "&7Usuwanie: kliknij główkę gracza"));
        player.openInventory(inv);
    }
    private static ItemStack icon(Material material,String name,String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Colors.color(GuiTheme.itemTitle(name)));
        meta.lore(GuiTheme.lore(lore));
        stack.setItemMeta(meta);
        return stack;
    }
}
