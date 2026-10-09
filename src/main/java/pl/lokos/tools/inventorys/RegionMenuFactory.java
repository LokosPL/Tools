package pl.lokos.tools.inventorys;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.*;

import java.util.*;

/** GUI z kontrolowanym Holder i PDC: klikanie nigdy nie opiera sie na nazwach przedmiotow. */
public final class RegionMenuFactory {
    public enum View { LOCATIONS, EDIT, FLAGS, RANKS }
    public static final class Holder implements InventoryHolder {
        private final UUID owner;
        private final View view;
        private final String region;
        private final int page;
        private Inventory inventory;
        private Holder(UUID owner,View view,String region,int page){
            this.owner=owner;this.view=view;this.region=region;this.page=page;
        }
        public UUID owner(){return owner;}
        public View view(){return view;}
        public String region(){return region;}
        public int page(){return page;}
        @Override public Inventory getInventory(){return inventory;}
    }
    private final RegionManager regions;
    private final RankManager ranks;
    private final NamespacedKey key;
    private final int waitingSeconds;
    public RegionMenuFactory(RegionManager regions,RankManager ranks,NamespacedKey key,int waitingSeconds){
        this.regions=regions;this.ranks=ranks;this.key=key;this.waitingSeconds=waitingSeconds;
    }
    private ItemStack item(Material type,String name,String action,String... lore) {
        ItemStack stack=new ItemStack(type);
        ItemMeta meta=stack.getItemMeta();
        meta.displayName(Colors.color(name));
        meta.lore(Arrays.stream(lore).map(Colors::color).toList());
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,action);
        stack.setItemMeta(meta);
        return stack;
    }
    public String action(ItemStack stack) {
        if(stack==null || !stack.hasItemMeta())return null;
        return stack.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);
    }
    private Inventory open(Player player,View view,String region,int page,String title) {
        Holder holder=new Holder(player.getUniqueId(),view,region,page);
        Inventory inventory=Bukkit.createInventory(holder,54,Colors.color(title));
        holder.inventory=inventory;
        ItemStack bg=item(Material.GRAY_STAINED_GLASS_PANE,"&8","noop");
        for(int slot=0;slot<9;slot++)inventory.setItem(slot,bg);
        for(int slot=45;slot<54;slot++)inventory.setItem(slot,bg);
        player.openInventory(inventory);
        return inventory;
    }
    public void locations(Player player,int page) {
        List<Region> available=regions.index().all().values().stream()
                .filter(r->r.spawn()!=null && regions.canEnter(player,r))
                .sorted(Comparator.comparing(Region::name)).toList();
        int current=Math.max(0,Math.min(page,Math.max(0,(available.size()-1)/36)));
        Inventory inventory=open(player,View.LOCATIONS,null,current,"&8ʟᴏᴋᴀʟɪᴢᴀᴄᴊᴇ &8• &a"+(current+1));
        for(int i=current*36;i<Math.min((current+1)*36,available.size());i++){
            Region region=available.get(i);
            inventory.setItem(9+i-current*36,item(
                    region.parent()==null?Material.GRASS_BLOCK:Material.ENDER_PEARL,
                    "&a&l"+region.name(),"tp:"+region.name(),
                    "&8──────────────────────",
                    "&7Świat: &a"+Optional.ofNullable(Bukkit.getWorld(region.world()))
                            .map(World::getName).orElse("niedostępny"),
                    "&7Obszar: &a"+(region.maxX()-region.minX()+1)+" × "+(region.maxZ()-region.minZ()+1),
                    "&7Typ: &a"+(region.parent()==null?"główny":"podregion"),
                    "&8 ",
                    "&aKliknij, aby się teleportować"));
        }
        navigation(inventory,current,(available.size()-1)/36,"Dostępne lokalizacje: "+available.size());
    }
    public void edit(Player player,String name) {
        Region region=regions.index().byName(name);
        if(region==null)return;
        Inventory inventory=open(player,View.EDIT,name,0,"&8ʀᴇɢɪᴏɴ &8• &a"+name);
        inventory.setItem(11,item(Material.SHIELD,"&a&lZABEZPIECZENIA","flags",
                "&8──────────────────────",
                "&7Wszystkie zasady, podzielone",
                "&7na strony i kategorie.",
                "&8 ",
                "&aKliknij, aby zarządzać"));
        inventory.setItem(13,item(Material.NAME_TAG,"&a&lDOSTĘP RANG","ranks",
                "&8──────────────────────",
                "&7Wymagana ranga: &a"+(region.entryRank()==null?"wszyscy":region.entryRank()),
                "&7Wybierz ją bez wpisywania komend.",
                "&8 ",
                "&aKliknij, aby wybrać rangę"));
        inventory.setItem(15,item(Material.COMPASS,"&a&lPUNKT TELEPORTACJI","noop",
                "&8──────────────────────",
                "&7Status: "+(region.spawn()==null?"&cnieustawiony":"&austawiony"),
                "&7Aby ustawić punkt, stań",
                "&7w regionie i użyj /region spawn."));
        inventory.setItem(30,item(Material.MAP,"&a&lGRANICE REGIONU","border",
                "&7Pokaż narożniki i obrys",
                "&7lokalnymi cząsteczkami.",
                "&8 ",
                "&aKliknij, aby wyświetlić"));
        inventory.setItem(32,item(Material.BOOK,"&a&lINFORMACJE","noop",
                "&7Świat: &a"+region.world(),
                "&7Rozmiar: &a"+(region.maxX()-region.minX()+1)+" × "+(region.maxZ()-region.minZ()+1),
                "&7Rodzic: &a"+(region.parent()==null?"brak":region.parent())));
        inventory.setItem(49,item(Material.BARRIER,"&cZamknij","close"));
    }
    private static List<Integer> slots(){
        List<Integer> slots=new ArrayList<>();
        for(int row=1;row<=4;row++)for(int col=1;col<=7;col++)slots.add(row*9+col);
        return slots;
    }
    public void flags(Player player,String name,int page){
        Region region=regions.index().byName(name);
        if(region==null)return;
        List<RegionFlag> flags=List.of(RegionFlag.values());
        int maximum=Math.max(0,(flags.size()-1)/28);
        int current=Math.max(0,Math.min(page,maximum));
        Inventory inventory=open(player,View.FLAGS,name,current,"&8ᴢᴀꜱᴀᴅʏ &8• &a"+name+" &8["+ (current+1)+"]");
        List<Integer> slots=slots();
        for(int i=current*28;i<Math.min(flags.size(),(current+1)*28);i++){
            RegionFlag f=flags.get(i);
            Boolean state=region.flags().get(f);
            boolean effective=regions.index().enabled(region,f);
            String mode=state==null?"&7Dziedziczenie":state?"&aDozwolone":"&cZabronione";
            inventory.setItem(slots.get(i-current*28),item(state==null?Material.LIGHT_GRAY_DYE:
                    state?Material.LIME_DYE:Material.RED_DYE,
                    (effective?"&a":"&c")+f.label(),"flag:"+f.name(),
                    "&8──────────────────────",
                    "&7Kategoria: &a"+f.category(),
                    "&7Stan: "+mode,
                    "&7Efektywnie: "+(effective?"&aDozwolone":"&cZabronione"),
                    "&8 ",
                    "&7Zmiana: &adziedzicz → tak → nie",
                    "&aKliknij, aby przełączyć"));
        }
        navigation(inventory,current,maximum,"Zasady: "+flags.size());
        inventory.setItem(48,item(Material.OAK_DOOR,"&7Wróć do regionu","back"));
    }
    public void rankAccess(Player player,String name,int page){
        Region region=regions.index().byName(name);
        if(region==null)return;
        List<RankSnapshot.Rank> all=ranks.snapshot().ranks().values().stream()
                .filter(RankSnapshot.Rank::assignable)
                .sorted(Comparator.comparingInt(r->r.position()==null?9999:r.position()))
                .toList();
        int current=Math.max(0,Math.min(page,Math.max(0,(all.size()-1)/27)));
        Inventory inventory=open(player,View.RANKS,name,current,"&8ᴅᴏꜱᴛᴇ̨ᴘ ᴅᴏ &a"+name);
        inventory.setItem(10,item(Material.LIME_WOOL,"&aWszyscy gracze","rank:wszyscy",
                "&7Brak ograniczeń rangi.",
                "&7Aktualnie: "+(region.entryRank()==null?"&aTAK":"&cNIE"),
                "&aKliknij, aby wybrać"));
        List<Integer> positions=slots();
        int slotIndex=1;
        for(int i=current*27;i<Math.min(all.size(),(current+1)*27);i++){
            RankSnapshot.Rank r=all.get(i);
            inventory.setItem(positions.get(slotIndex++),item(
                    name.equals(r.name())?Material.EMERALD:Material.NAME_TAG,
                    "&a"+r.name(),"rank:"+r.name(),
                    "&7Pozycja: &a"+r.position(),
                    "&7Gracze tej rangi i wyższych",
                    "&7uzyskają dostęp do regionu.",
                    "&7Wybrana: "+(r.name().equals(region.entryRank())?"&aTAK":"&7nie"),
                    "&aKliknij, aby wybrać"));
        }
        navigation(inventory,current,Math.max(0,(all.size()-1)/27),"Dostęp: "+(region.entryRank()==null?"wszyscy":region.entryRank()));
        inventory.setItem(48,item(Material.OAK_DOOR,"&7Wróć do regionu","back"));
    }
    private void navigation(Inventory inv,int current,int max,String label){
        if(current>0)inv.setItem(45,item(Material.ARROW,"&aPoprzednia strona","page:"+(current-1)));
        if(current<max)inv.setItem(53,item(Material.ARROW,"&aNastępna strona","page:"+(current+1)));
        inv.setItem(49,item(Material.BOOK,"&a&l"+label,"noop",
                "&7Strona: &a"+(current+1)+"&8/&a"+(max+1),
                "&8──────────────────────"));
    }
}
