package pl.lokos.tools.inventorys;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.lokos.tools.helpers.Colors;
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
    private static final List<String> FEATURED=List.of(
            "tools.lokalizacje","tools.lokalizacje.instant","tools.region.bypass",
            "tools.region.admin","tools.ranga.admin","tools.admin","*");
    public RankMenuFactory(NamespacedKey key,RankManager ranks){this.key=key;this.ranks=ranks;}

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
        if(item==null||!item.hasItemMeta())return null;
        return item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
    }
    private Inventory inventory(Player p,View view,String name,int page,String title) {
        Holder holder=new Holder(p.getUniqueId(),name,view,page);
        Inventory inv=Bukkit.createInventory(holder,54,Colors.color(title));
        holder.inventory=inv;
        ItemStack glass=item(Material.GRAY_STAINED_GLASS_PANE,"&8","noop");
        for(int n=0;n<9;n++)inv.setItem(n,glass);
        for(int n=45;n<54;n++)inv.setItem(n,glass);
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
    public List<String> permissions(){
        Set<String> all=new TreeSet<>(FEATURED);
        Bukkit.getPluginManager().getPermissions().forEach(p->all.add(p.getName()));
        ranks.snapshot().permissions().values().forEach(all::addAll);
        return List.copyOf(all);
    }
    public void permissions(Player p,String name,int page) {
        if(!ranks.snapshot().ranks().containsKey(name))return;
        List<String> list=permissions();
        int max=Math.max(0,(list.size()-1)/36),current=Math.min(Math.max(0,page),max);
        Inventory inv=inventory(p,View.PERMISSIONS,name,current,"&8ᴜᴘʀᴀᴡɴɪᴇɴɪᴀ &8• &a"+name);
        Set<String> granted=ranks.snapshot().permissions().getOrDefault(name,Set.of());
        for(int i=current*36;i<Math.min(list.size(),(current+1)*36);i++){
            String permission=list.get(i);
            boolean active=granted.contains(permission);
            inv.setItem(9+i-current*36,item(active?Material.LIME_DYE:Material.RED_DYE,
                    (active?"&a":"&c")+permission,"toggle:"+permission,
                    "&8──────────────────────",
                    "&7Status: "+(active?"&aNadane":"&cWyłączone"),
                    permission.equals("*")?"&cPełne uprawnienia serwera":"&7Zarejestrowane uprawnienie pluginu",
                    "&8 ",
                    "&aKliknij, aby zmienić"));
        }
        footer(inv,current,max,"Uprawnienia: "+granted.size());
        inv.setItem(48,item(Material.OAK_DOOR,"&7Wróć","back"));
    }
    private void footer(Inventory inv,int current,int max,String label){
        if(current>0)inv.setItem(45,item(Material.ARROW,"&aPoprzednia strona","page:"+(current-1)));
        if(current<max)inv.setItem(53,item(Material.ARROW,"&aNastępna strona","page:"+(current+1)));
        inv.setItem(49,item(Material.BOOK,"&a"+label,"noop","&7Strona: &a"+(current+1)+"&8/&a"+(max+1)));
    }
}
