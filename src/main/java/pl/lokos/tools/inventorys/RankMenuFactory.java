package pl.lokos.tools.inventorys;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.GuiTheme;
import pl.lokos.tools.helpers.ToolsPermissionCatalog;
import pl.lokos.tools.config.CommandsFile;
import pl.lokos.tools.manager.*;
import java.util.*;

/** Bezpieczne GUI rang, uprawnienia z serwera oraz wbudowane uprawnienia Tools. */
public final class RankMenuFactory {
    public enum View { LIST, DETAILS, PERMISSIONS }
    public static final class Holder implements InventoryHolder {
        private final UUID owner;
        private final String rank;
        private final View view;
        private final int page;
        private Inventory inventory;
        Holder(UUID owner,String rank,View view,int page) {
            this.owner=owner;this.rank=rank;this.view=view;this.page=page;
        }
        public UUID owner(){return owner;}
        public String rank(){return rank;}
        public View view(){return view;}
        public int page(){return page;}
        @Override public Inventory getInventory(){return inventory;}
    }
    private final NamespacedKey key;
    private final RankManager ranks;
    private final ToolsPermissionCatalog catalog;
    public RankMenuFactory(NamespacedKey key, RankManager ranks, CommandsFile commands){
        this.key=key;this.ranks=ranks;this.catalog=new ToolsPermissionCatalog(commands);
    }

    private ItemStack item(Material type,String name,String action,String... lore) {
        ItemStack item=new ItemStack(type);
        ItemMeta meta=item.getItemMeta();
        meta.displayName(Colors.color(GuiTheme.itemTitle(name)));
        meta.lore(GuiTheme.lore(lore));
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,action);
        item.setItemMeta(meta);
        return item;
    }
    public String action(ItemStack item) {
        if(item==null||!item.hasItemMeta())return null;
        return item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
    }
    private Inventory inventory(Player p,View view,String name,int page,String title) {
        Holder holder=new Holder(p.getUniqueId(),name,view,page);
        Inventory inv=Bukkit.createInventory(holder,54,Colors.color(GuiTheme.title(title)));
        holder.inventory=inv;
        GuiTheme.frame(inv);
        p.openInventory(inv);
        return inv;
    }
    public void list(Player p,int page){
        List<RankSnapshot.Rank> all=ranks.snapshot().ranks().values().stream()
                .sorted(Comparator.comparingInt((RankSnapshot.Rank r)->r.position()==null?9999:r.position())
                        .thenComparing(RankSnapshot.Rank::name)).toList();
        int max=Math.max(0,(all.size()-1)/36),current=Math.min(Math.max(0,page),max);
        Inventory inv=inventory(p,View.LIST,null,current,"&8ʀᴀɴɢɪ &8• &a"+(current+1));
        for(int i=current*36;i<Math.min(all.size(),(current+1)*36);i++){
            var r=all.get(i);
            inv.setItem(i-current*36+9,item(r.name().equals("gracz")?Material.BOOK:Material.NAME_TAG,
                    "&a"+r.name(),"rank:"+r.name(),
                    "&8──────────────────────",
                    "&7Priorytet: &a"+(r.position()==null?"nieustawiony":r.position()),
                    "&7Uprawnienia: &a"+ranks.snapshot().permissions().getOrDefault(r.name(),Set.of()).size(),
                    r.name().equals("gracz")?"&7Ranga podstawowa, nieusuwalna":"&7Możesz nią zarządzać",
                    "&8 ",
                    "&aKliknij, aby otworzyć"));
        }
        footer(inv,current,max,"Liczba rang: "+all.size());
    }
    public void details(Player p,String name){
        RankSnapshot.Rank r=ranks.snapshot().ranks().get(name);
        if(r==null)return;
        Inventory inv=inventory(p,View.DETAILS,name,0,"&8ʀᴀɴɢᴀ &8• &a"+name);
        inv.setItem(11,item(Material.ENCHANTED_BOOK,"&a&lUPRAWNIENIA","perms",
                "&7Wybierz uprawnienia bez komend.",
                "&7Włącz lub wyłącz kliknięciem.",
                "&8 ",
                "&aKliknij, aby otworzyć"));
        inv.setItem(13,item(Material.NAME_TAG,"&a&lDANE RANGI","noop",
                "&7Nazwa: &a"+name,
                "&7Prefix: &f"+r.prefix(),
                "&7Suffix: &f"+r.suffix(),
                "&7Pozycja: &a"+(r.position()==null?"brak":r.position())));
        inv.setItem(15,item(Material.BOOK,"&a&lUSTAWIENIA","noop",
                "&7Edycja nazwy, prefixu, suffixu",
                "&7i pozycji nadal dostępna",
                "&7poprzez /ranga edytuj oraz /ranga pozycja."));
        inv.setItem(31,item(Material.BARRIER,"&cUSUWANIE","noop",
                name.equals("gracz")?"&cRangi Gracz nie można usunąć.":"&7/ranga usun "+name));
        inv.setItem(48,item(Material.OAK_DOOR,"&7Wróć","back"));
    }
    public List<String> permissions(){return catalog.suggestions();}

    public void permissions(Player p,String name,int page) {
        if(!ranks.snapshot().ranks().containsKey(name))return;
        List<ToolsPermissionCatalog.Feature> entries=catalog.features();
        int max=Math.max(0,(entries.size()-1)/36),current=Math.min(Math.max(0,page),max);
        Inventory inv=inventory(p,View.PERMISSIONS,name,current,"&8ᴜᴘʀᴀᴡɴɪᴇɴɪᴀ &8• &a"+name);
        Set<String> granted=ranks.snapshot().permissions().getOrDefault(name,Set.of());
        for(int i=current*36;i<Math.min(entries.size(),(current+1)*36);i++){
            ToolsPermissionCatalog.Feature feature=entries.get(i);
            String permission=feature.permission();
            boolean active=granted.contains(permission);
            List<String> tooltip=new ArrayList<>();
            tooltip.add(GuiTheme.RULE);
            tooltip.add("&7Stan: "+(active?"&aWłączone":"&cWyłączone"));
            tooltip.add("&8 ");
            tooltip.add("&7Działanie:");
            for(String line:wrap(feature.description(),42))tooltip.add("&7"+line);
            for(String line:wrap(feature.details(),42))tooltip.add("&7"+line);
            tooltip.add("&8 ");
            tooltip.add("&8Uprawnienie: &7"+permission);
            tooltip.add("&aKliknij, aby przełączyć");
            inv.setItem(9+i-current*36,item(active?Material.LIME_DYE:Material.RED_DYE,
                    (active?"&a":"&c")+feature.name(),"toggle:"+permission,
                    tooltip.toArray(String[]::new)));
        }
        footer(inv,current,max,"Funkcje Tools: "+entries.size());
        inv.setItem(48,item(Material.OAK_DOOR,"&7Wróć","back"));
    }
    private static List<String> wrap(String value,int limit) {
        List<String> out=new ArrayList<>();
        StringBuilder line=new StringBuilder();
        for(String word:value.split("\\s+")){
            if(line.length()>0 && line.length()+1+word.length()>limit){
                out.add(line.toString());
                line.setLength(0);
            }
            if(line.length()>0)line.append(' ');
            line.append(word);
        }
        if(!line.isEmpty())out.add(line.toString());
        return out;
    }

    private void footer(Inventory inv,int current,int max,String label){
        if(current>0)inv.setItem(45,item(Material.ARROW,"&aPoprzednia strona","page:"+(current-1)));
        if(current<max)inv.setItem(53,item(Material.ARROW,"&aNastępna strona","page:"+(current+1)));
        inv.setItem(49,item(Material.BOOK,"&a"+label,"noop","&7Strona: &a"+(current+1)+"&8/&a"+(max+1)));
    }
}
