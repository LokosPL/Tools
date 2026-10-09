package pl.lokos.tools.inventorys;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionFlag;

import java.util.*;

/** Bezpieczna fabryka GUI: rozpoznajemy wlasny InventoryHolder + znaczniki PDC. */
public final class RegionMenuFactory {
    public enum View { LOCATIONS, EDIT }
    public static final class Holder implements InventoryHolder {
        private final UUID owner;
        private final View view;
        private final String region;
        private final int page;
        private Inventory inventory;
        private Holder(UUID owner, View view, String region, int page) {
            this.owner=owner;this.view=view;this.region=region;this.page=page;
        }
        public UUID owner() {return owner;}
        public View view() {return view;}
        public String region() {return region;}
        public int page() {return page;}
        @Override public Inventory getInventory() {return inventory;}
    }

    private final RegionManager regions;
    private final NamespacedKey key;
    public RegionMenuFactory(RegionManager regions,NamespacedKey key) {
        this.regions=regions;this.key=key;
    }

    private ItemStack item(Material type,String name,String action,String... lore) {
        ItemStack item=new ItemStack(type);
        ItemMeta meta=item.getItemMeta();
        meta.displayName(Colors.color(name));
        meta.lore(Arrays.stream(lore).map(Colors::color).toList());
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,action);
        item.setItemMeta(meta);
        return item;
    }
    public String action(ItemStack item) {
        if(item==null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
    }
    private void border(Inventory inventory) {
        ItemStack frame=item(Material.GRAY_STAINED_GLASS_PANE,"&8","noop");
        for(int slot=45;slot<54;slot++)inventory.setItem(slot,frame);
    }
    public void locations(Player player,int page) {
        List<Region> available=regions.index().all().values().stream()
                .filter(r->r.spawn()!=null && regions.canEnter(player,r))
                .sorted(Comparator.comparing(Region::name)).toList();
        int maxPage=Math.max(0,(available.size()-1)/45);
        int current=Math.min(Math.max(0,page),maxPage);
        Holder holder=new Holder(player.getUniqueId(),View.LOCATIONS,null,current);
        Inventory inventory=Bukkit.createInventory(holder,54,Colors.color("&8Lokalizacje &7• &a"+(current+1)));
        holder.inventory=inventory;
        border(inventory);
        for(int i=current*45;i<Math.min((current+1)*45,available.size());i++) {
            Region r=available.get(i);
            inventory.setItem(i-current*45,item(r.parent()==null?Material.GRASS_BLOCK:Material.ENDER_PEARL,
                    "&a&l"+r.name(),"tp:"+r.name(),
                    "&7Świat: &a"+Optional.ofNullable(Bukkit.getWorld(r.world())).map(w->w.getName()).orElse("Niedostępny"),
                    "&7Obszar: &a"+(r.maxX()-r.minX()+1)+" × "+(r.maxZ()-r.minZ()+1),
                    "&8",
                    "&aKliknij, aby rozpocząć teleportację."));
        }
        if(current>0)inventory.setItem(45,item(Material.ARROW,"&7Poprzednia strona","page:"+(current-1)));
        inventory.setItem(49,item(Material.COMPASS,"&a&lLOKALIZACJE","noop",
                "&7Dostępne punkty: &a"+available.size(),
                "&7Czas teleportacji: &a5 sekund"));
        if(current<maxPage)inventory.setItem(53,item(Material.ARROW,"&7Następna strona","page:"+(current+1)));
        player.openInventory(inventory);
    }

    public void edit(Player player,String name) {
        Region region=regions.index().byName(name);
        if(region==null) return;
        Holder holder=new Holder(player.getUniqueId(),View.EDIT,name,0);
        Inventory inventory=Bukkit.createInventory(holder,54,Colors.color("&8Region &7• &a"+name));
        holder.inventory=inventory; border(inventory);
        int slot=10;
        for(RegionFlag flag:RegionFlag.values()) {
            Boolean value=region.flags().get(flag);
            String state=value==null?"&7Dziedziczenie":value?"&aWłączona":"&cWyłączona";
            inventory.setItem(slot++,item(value==null?Material.PAPER:value?Material.LIME_DYE:Material.RED_DYE,
                    "&7"+flag.label(),"flag:"+flag.name(),
                    "&7Stan: "+state,
                    "&7Wartość z rodzica: &a"+(regions.index().enabled(region,flag)?"włączona":"wyłączona"),
                    "&8",
                    "&aKliknij, aby zmienić zasadę."));
            if(slot==17)slot=19;
            if(slot==26)slot=28;
        }
        inventory.setItem(40,item(Material.NAME_TAG,"&aWymagana ranga","noop",
                "&7Aktualna: &a"+(region.entryRank()==null?"wszyscy":region.entryRank()),
                "&7Zmień: &a/region edytuj "+name+" wejscie <ranga|wszyscy>"));
        inventory.setItem(49,item(Material.BOOK,"&aZASADY REGIONU","noop",
                "&7Kliknięcie flagi przełącza jej stan:",
                "&7dziedziczenie → tak → nie → dziedziczenie.",
                "&7Modyfikacje zapisują się w MySQL."));
        player.openInventory(inventory);
    }
}
