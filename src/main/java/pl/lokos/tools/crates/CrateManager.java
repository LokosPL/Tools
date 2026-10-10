package pl.lokos.tools.crates;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.events.EventManager;
import pl.lokos.tools.events.EventType;
import pl.lokos.tools.events.StateFile;
import pl.lokos.tools.helpers.*;
import pl.lokos.tools.items.SpecialItemService;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.security.ToolsAccess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pięć typów skrzyń z niezależnym menu i tokenami PDC.
 * Otwieranie zarejestrowanych skrzyń działa również przy INTERACT=false
 * w regionie; nie nadaje żadnego dostępu do innych bloków chronionego regionu.
 */
public final class CrateManager implements Listener,AutoCloseable {
    private static final String ADMIN="tools.skrzynia.admin";
    public static final class Menu implements InventoryHolder {
        private final CrateType type;
        private final boolean admin,browse;
        private final CratesState.Position position;
        private Inventory inv;
        private Menu(CrateType type,boolean admin,boolean browse,CratesState.Position position){
            this.type=type;this.admin=admin;this.browse=browse;this.position=position;
        }
        @Override public Inventory getInventory(){return inv;}
    }
    public static final class ResultMenu implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory(){return inv;}
    }
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final RegionManager regions;
    private final SpecialItemService items;
    private final EventManager events;
    private final CratesConfig config;
    private final StateFile<CratesState> storage;
    private final NamespacedKey keyType;
    private final NamespacedKey placementType;
    private final Map<String,TextDisplay> displays=new HashMap<>();
    private final Map<String,String> lastLabel=new HashMap<>();
    private final CrateActivityWindow activity=new CrateActivityWindow();
    private final Map<UUID,Long> lastOpened=new HashMap<>();
    private long ticks;

    public CrateManager(JavaPlugin plugin,RankManager ranks,RegionManager regions,
                        SpecialItemService items,EventManager events,Path folder) throws IOException{
        this.plugin=plugin;this.ranks=ranks;this.regions=regions;this.items=items;this.events=events;
        config=new JsonConfigManager(folder).load("Crates.json",CratesConfig.class,
                CratesConfig::new,CratesConfig::validate);
        storage=new StateFile<>(folder,"CratesState.json",CratesState.class,
                CratesState::new,CratesState::validate);
        keyType=new NamespacedKey(plugin,"crate_key");
        placementType=new NamespacedKey(plugin,"crate_placement");
        for(CrateType type:CrateType.values()){
            for(String drop:config.pool(type).keySet()){
                if(drop.startsWith("minecraft:") ||drop.equals("@event"))continue;
                if(items.config().get(drop)==null)
                    throw new IOException("Crates.json: nieznany przedmiot "+drop);
            }
        }
    }
    public CratesConfig config(){return config;}
    public CratesState state(){return storage.get();}
    private boolean admin(Player p){return ToolsAccess.allowed(p,ranks,ADMIN,true);}
    private static String at(Block block){
        return block.getWorld().getUID()+":"+block.getX()+":"+block.getY()+":"+block.getZ();
    }
    private static boolean slotFree(Player player){
        for(ItemStack item:player.getInventory().getStorageContents())
            if(item==null||item.getType().isAir())return true;
        return false;
    }
    private ItemStack icon(CrateType type,boolean place){
        Material material=place?Material.valueOf(type.block()):Material.TRIPWIRE_HOOK;
        ItemStack stack=new ItemStack(material);
        ItemMeta meta=stack.getItemMeta();
        meta.displayName(Colors.color(type.color()+"✦ "+(place?"POSTAW: ":"KLUCZ: ")+type.title()));
        meta.lore(GuiTheme.lore(
                "&#A8A8B7Typ: "+type.title(),
                place?"&#70D6E8» Postaw tylko na głównym spawnie."
                        :"&#70D6E8» Kliknij pasującą skrzynię na spawnie.",
                "&#A8A8B7» Unikatowy przedmiot Tools."));
        meta.getPersistentDataContainer().set(place?placementType:keyType,
                PersistentDataType.STRING,type.id());
        if(!place)meta.setEnchantmentGlintOverride(true);
        stack.setItemMeta(meta);
        return stack;
    }
    public void giveKey(Player player,CrateType type,int amount){
        if(type==null||amount<1||amount>64)throw new IllegalArgumentException("Liczba kluczy: 1-64.");
        ItemStack key=icon(type,false);key.setAmount(amount);
        Map<Integer,ItemStack> overflow=player.getInventory().addItem(key);
        for(ItemStack item:overflow.values())player.getWorld().dropItemNaturally(player.getLocation(),item);
        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.45f,1.7f);
    }
    public void givePlacement(Player player,CrateType type){
        if(!slotFree(player))throw new IllegalArgumentException("Brak miejsca w ekwipunku.");
        player.getInventory().addItem(icon(type,true));
    }
    /** Liczba autentycznych kluczy danego typu w ekwipunku gracza. */
    public int keyCount(Player player,CrateType type){
        int count=0;
        for(ItemStack item:player.getInventory().getStorageContents()){
            if(item==null||!item.hasItemMeta())continue;
            if(type.id().equals(item.getItemMeta().getPersistentDataContainer()
                    .get(keyType,PersistentDataType.STRING)))count+=item.getAmount();
        }
        return count;
    }
    public void keySummary(Player player){
        Messages.title(player,"TWOJE KLUCZE");
        for(CrateType type:CrateType.values())
            Messages.info(player,"&#70D6E8"+type.title()+" &#A8A8B7» &#FFD166"+keyCount(player,type));
        Messages.info(player,"&#A8A8B7Postęp AFK: &#FFD166"+
                state().afkMinutes().getOrDefault(player.getUniqueId().toString(),0)
                +"/"+config.afkKeyMinutes()+" minut.");
    }
    /** Panel publiczny wyświetla nagrody, ale nie otwiera skrzyń na odległość. */
    public void browseMenu(Player player){
        Menu holder=new Menu(null,false,true,null);
        Inventory inv=Bukkit.createInventory(holder,27,Colors.color("&#FFD166✦ SKRZYNIE I KLUCZE"));
        holder.inv=inv;
        ItemStack bg=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,bg);
        int[] slots={10,11,13,15,16};
        int index=0;
        for(CrateType type:CrateType.values()){
            ItemStack icon=icon(type,false);
            ItemMeta meta=icon.getItemMeta();
            List<Component> lore=new ArrayList<>(meta.lore());
            lore.add(Colors.color("&#FFD166» Twoje klucze: "+keyCount(player,type)));
            lore.add(Colors.color("&#70D6E8» Kliknij, aby sprawdzić nagrody."));
            meta.lore(lore);icon.setItemMeta(meta);
            inv.setItem(slots[index++],icon);
        }
        ItemStack close=new ItemStack(Material.BARRIER);
        ItemMeta c=close.getItemMeta();c.displayName(Colors.color("&#FF727F✘ Zamknij"));
        close.setItemMeta(c);inv.setItem(22,close);
        player.openInventory(inv);
    }
    public void adminMenu(Player admin){
        if(!admin(admin)){Messages.unknown(admin);return;}
        Menu holder=new Menu(null,true,false,null);
        Inventory inv=Bukkit.createInventory(holder,27,Colors.color("&#FFD166✦ SKRZYNIE SERWERA"));
        holder.inv=inv;
        ItemStack empty=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int n=0;n<inv.getSize();n++)inv.setItem(n,empty);
        int[] slots={10,11,13,15,16};
        int i=0;
        for(CrateType type:CrateType.values()){
            ItemStack item=icon(type,true);
            ItemMeta meta=item.getItemMeta();
            var lore=new ArrayList<>(meta.lore());
            lore.add(Colors.color("&#70D6E8» LPM: odbierz skrzynię do postawienia."));
            lore.add(Colors.color("&#FFD166» PPM: 1 klucz dla siebie."));
            lore.add(Colors.color("&#FFD166» Shift+PPM: 16 kluczy."));
            meta.lore(lore);item.setItemMeta(meta);
            inv.setItem(slots[i++],item);
        }
        admin.openInventory(inv);
    }
    private void show(Player player,CrateType type,CratesState.Position position){
        Menu holder=new Menu(type,false,false,position);
        Inventory inv=Bukkit.createInventory(holder,27,
                Colors.color(type.color()+"✦ SKRZYNIA "+type.title().toUpperCase(Locale.ROOT)));
        holder.inv=inv;
        ItemStack empty=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int n=0;n<inv.getSize();n++)inv.setItem(n,empty);
        ItemStack open=new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta=open.getItemMeta();
        meta.displayName(Colors.color("&#FFD166✦ Otwórz skrzynię"));
        meta.lore(GuiTheme.lore(
                "&#A8A8B7Klucze: &#FFD166"+keyCount(player,type),
                position==null?"&#FF727F» Podejdź do postawionej skrzyni.":"&#70D6E8» Kliknij, aby wylosować nagrodę.",
                "&#A8A8B7Zużywa 1 klucz i natychmiast wypłaca nagrodę."));
        open.setItemMeta(meta);
        inv.setItem(13,open);
        inv.setItem(11,icon(type,false));
        ItemStack reward=new ItemStack(Material.CHEST);
        ItemMeta rm=reward.getItemMeta();
        rm.displayName(Colors.color("&#70D6E8✦ Dostępne nagrody"));
        List<Component> lore=new ArrayList<>();
        int total=config.pool(type).values().stream().mapToInt(Integer::intValue).sum();
        for(var entry:config.pool(type).entrySet()){
            String value=entry.getKey();
            String label=value.equals("@event")?events.active()==null?"Aktualny przedmiot eventowy":
                    events.active().itemId():value.replace("minecraft:","");
            double percent=100d*entry.getValue()/Math.max(1,total);
            lore.add(Colors.color("&#A8A8B7» "+label+" &#FFD166"+
                    String.format(Locale.ROOT,"%.1f",percent)+"%"));
        }
        rm.lore(lore);reward.setItemMeta(rm);
        inv.setItem(15,reward);
        ItemStack back=new ItemStack(Material.ARROW);
        ItemMeta bm=back.getItemMeta();bm.displayName(Colors.color("&#70D6E8← Wszystkie skrzynie"));
        back.setItemMeta(bm);inv.setItem(18,back);
        player.openInventory(inv);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void interact(PlayerInteractEvent event){
        Block clicked=event.getClickedBlock();
        if(clicked==null)return;
        CratesState.Position crate=storage.get().get(at(clicked));
        if(crate==null)return;
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        event.setCancelled(true);
        if(!config.enabled()){
            Messages.error(event.getPlayer(),"Skrzynie są tymczasowo wyłączone.");
            return;
        }
        Player player=event.getPlayer();
        CrateType type=crate.kind();
        // Otwieramy w następnym ticku, po wszystkich regionowych listenerach;
        // brak ingerencji w pozostałe blokady INTERACT.
        Bukkit.getScheduler().runTask(plugin,()->{
            if(!player.isOnline()||!storage.get().crates().contains(crate))return;
            if(!clicked.getType().equals(Material.valueOf(type.block())))return;
            if(type==CrateType.EVENTOWA&&events.active()==null){
                Messages.error(player,"Skrzynia eventowa jest aktywna tylko podczas eventu.");return;
            }
            show(player,type,crate);
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void place(BlockPlaceEvent event){
        // Nie pozwalamy dołączyć drugiej połowy chestu do skrzyni Tools.
        Block block=event.getBlockPlaced();
        if(block.getType()==Material.CHEST||block.getType()==Material.TRAPPED_CHEST){
            for(org.bukkit.block.BlockFace face:List.of(
                    org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST)){
                Block neighbor=block.getRelative(face);
                if(storage.get().get(at(neighbor))!=null){
                    event.setCancelled(true);
                    Messages.error(event.getPlayer(),"Nie można łączyć skrzyni Tools w podwójną skrzynię.");
                    return;
                }
            }
        }
        ItemStack held=event.getItemInHand();
        if(held==null||!held.hasItemMeta())return;
        String id=held.getItemMeta().getPersistentDataContainer()
                .get(placementType,PersistentDataType.STRING);
        if(id==null)return;
        CrateType type=CrateType.parse(id);
        if(block.getType()==Material.CHEST||block.getType()==Material.TRAPPED_CHEST){
            for(org.bukkit.block.BlockFace face:List.of(
                    org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST)){
                if(block.getRelative(face).getType()==block.getType()){
                    event.setCancelled(true);
                    Messages.error(event.getPlayer(),"Zostaw jeden blok odstępu od innych skrzyń.");
                    return;
                }
            }
        }
        if(!admin(event.getPlayer())||!inSpawn(block)||type==null||
                !block.getType().equals(Material.valueOf(type.block()))
                ||storage.get().crates().size()>=config.maximumPlacedCrates()
                ||storage.get().get(at(block))!=null){
            event.setCancelled(true);
            Messages.error(event.getPlayer(),"Skrzynie można ustawiać tylko na głównym spawnie.");
            return;
        }
    }
    /** Zapis dopiero na MONITOR po regionach i wszystkich anulujących listenerach. */
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void recordPlacement(BlockPlaceEvent event){
        ItemStack held=event.getItemInHand();
        if(held==null||!held.hasItemMeta())return;
        CrateType type=CrateType.parse(held.getItemMeta()
                .getPersistentDataContainer().get(placementType,PersistentDataType.STRING));
        if(type==null||!admin(event.getPlayer()))return;
        Block block=event.getBlockPlaced();
        if(!inSpawn(block)||storage.get().get(at(block))!=null)return;
        CratesState.Position position=new CratesState.Position(block.getWorld().getUID().toString(),
                block.getX(),block.getY(),block.getZ(),type.id());
        Player actor=event.getPlayer();
        storage.update(old->old.with(position)).whenComplete((ignored,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(error!=null){
                    plugin.getLogger().severe("Nie zapisano postawionej skrzyni: "+error);
                    if(block.getType()==Material.valueOf(type.block()))block.setType(Material.AIR,false);
                    ItemStack refund=icon(type,true);
                    if(actor.isOnline()){
                        for(ItemStack overflow:actor.getInventory().addItem(refund).values())
                            actor.getWorld().dropItemNaturally(actor.getLocation(),overflow);
                    }else block.getWorld().dropItemNaturally(block.getLocation(),refund);
                    Messages.error(actor,"Nie zapisano skrzyni: blok cofnięto i zwrócono przedmiot.");
                }else{
                    hologram(position);
                    if(actor.isOnline())Messages.success(actor,"Ustawiono skrzynię "+type.title()+".");
                }
            });
        });
    }
    private boolean inSpawn(Block block){
        if(regions==null||!regions.ready())return false;
        Region spawn=regions.mainSpawn();
        return spawn!=null&&spawn.contains(block.getWorld().getUID(),block.getX(),block.getZ());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void breakBlock(BlockBreakEvent event){
        if(storage.get().get(at(event.getBlock()))==null)return;
        event.setCancelled(true);
        Messages.error(event.getPlayer(),"Ta skrzynia jest chroniona. Użyj /skrzynia usun.");
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void explode(org.bukkit.event.entity.EntityExplodeEvent event){
        event.blockList().removeIf(block->storage.get().get(at(block))!=null);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void explode(BlockExplodeEvent event){
        event.blockList().removeIf(block->storage.get().get(at(block))!=null);
    }
    /** Hoppery ani inne automaty nie mogą użyć dekoracyjnych skrzyń jako magazynów. */
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void hopper(InventoryMoveItemEvent event){
        if(managed(event.getSource())||managed(event.getDestination()))
            event.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void inventoryOpen(InventoryOpenEvent event){
        if(managed(event.getInventory()))event.setCancelled(true);
    }
    private boolean managed(Inventory inventory){
        Location at=inventory==null?null:inventory.getLocation();
        return at!=null&&storage.get().get(
                at.getWorld().getUID()+":"+at.getBlockX()+":"+at.getBlockY()+":"+at.getBlockZ())!=null;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void piston(BlockPistonExtendEvent event){
        for(Block block:event.getBlocks())if(storage.get().get(at(block))!=null){
            event.setCancelled(true);return;
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void piston(BlockPistonRetractEvent event){
        for(Block block:event.getBlocks())if(storage.get().get(at(block))!=null){
            event.setCancelled(true);return;
        }
    }
    public void remove(Player admin,Block block){
        if(!admin(admin))throw new IllegalArgumentException("Brak uprawnień.");
        CratesState.Position position=storage.get().get(at(block));
        if(position==null)throw new IllegalArgumentException("Wskazany blok nie jest skrzynią Tools.");
        storage.update(old->old.without(position.key())).whenComplete((ignored,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(error!=null){
                    plugin.getLogger().severe("Nie usunięto skrzyni: "+error);
                    if(admin.isOnline())Messages.error(admin,"Nie zapisano usunięcia skrzyni.");
                    return;
                }
                TextDisplay tag=displays.remove(position.key());if(tag!=null)tag.remove();
                lastLabel.remove(position.key());
                if(block.getType()==Material.valueOf(position.kind().block()))
                    block.setType(Material.AIR,false);
                if(admin.isOnline())Messages.success(admin,"Usunięto skrzynię.");
            });
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void menu(InventoryClickEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof ResultMenu){
            event.setCancelled(true);
            if(event.getRawSlot()==22 && event.getWhoClicked() instanceof Player resultPlayer)
                Bukkit.getScheduler().runTask(plugin,()->resultPlayer.closeInventory());
            return;
        }
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu holder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)||
                event.getClickedInventory()!=event.getView().getTopInventory())return;
        if(holder.browse){
            if(event.getRawSlot()==22){
                Bukkit.getScheduler().runTask(plugin,()->player.closeInventory());
                return;
            }
            int[] slots={10,11,13,15,16};
            for(int i=0;i<slots.length;i++)if(slots[i]==event.getRawSlot()){
                CrateType selected=CrateType.values()[i];
                Bukkit.getScheduler().runTask(plugin,()->show(player,selected,null));
                return;
            }
            return;
        }
        if(holder.admin){
            if(!admin(player)){Messages.unknown(player);return;}
            int[] slots={10,11,13,15,16};
            for(int i=0;i<slots.length;i++)if(slots[i]==event.getRawSlot()){
                try{
                    CrateType selected=CrateType.values()[i];
                    if(event.isRightClick()){
                        int amount=event.isShiftClick()?16:1;
                        giveKey(player,selected,amount);
                        Messages.success(player,"Otrzymano "+amount+" kluczy: "+selected.title()+".");
                    }else{
                        givePlacement(player,selected);
                        Messages.success(player,"Otrzymano skrzynię do postawienia na spawnie.");
                    }
                }catch(IllegalArgumentException error){Messages.error(player,error.getMessage());}
                return;
            }
            return;
        }
        if(event.getRawSlot()==18){
            Bukkit.getScheduler().runTask(plugin,()->browseMenu(player));
            return;
        }
        if(event.getRawSlot()!=13)return;
        if(holder.position==null){
            Messages.error(player,"Podgląd nagród nie otwiera skrzyni. Podejdź do bloku skrzyni na spawnie.");
            return;
        }
        openReward(player,holder.type,holder.position);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof Menu ||
                event.getView().getTopInventory().getHolder() instanceof ResultMenu)
            event.setCancelled(true);
    }
    private int findKey(Player player,CrateType type){
        ItemStack[] storage=player.getInventory().getStorageContents();
        for(int i=0;i<storage.length;i++){
            ItemStack item=storage[i];if(item==null||!item.hasItemMeta())continue;
            if(type.id().equals(item.getItemMeta().getPersistentDataContainer()
                    .get(keyType,PersistentDataType.STRING)))return i;
        }
        return -1;
    }
    private void openReward(Player player,CrateType type,CratesState.Position position){
        if(!config.enabled()){
            Messages.error(player,"Skrzynie są wyłączone w Crates.json.");
            return;
        }
        // GUI może pozostać otwarte po wyjściu gracza ze spawnu lub usunięciu skrzyni.
        if(!CrateOpeningPolicy.near(position,player.getWorld().getUID(),
                player.getLocation().getX(),player.getLocation().getY(),
                player.getLocation().getZ())
                ||!position.equals(storage.get().get(position.key()))){
            Messages.error(player,"Podejdź do właściwej skrzyni, aby ją otworzyć.");
            return;
        }
        Block chest=player.getWorld().getBlockAt(position.x(),position.y(),position.z());
        if(chest.getType()!=Material.valueOf(type.block())||position.kind()!=type){
            Messages.error(player,"Ta skrzynia nie jest już dostępna.");
            return;
        }
        Long previous=lastOpened.get(player.getUniqueId());
        long now=System.currentTimeMillis();
        if(previous!=null && now-previous<1200L){
            Messages.error(player,"Otwierasz skrzynie zbyt szybko.");
            return;
        }
        if(type==CrateType.EVENTOWA&&events.active()==null){
            Messages.error(player,"Event się zakończył.");return;
        }
        int keySlot=findKey(player,type);
        if(keySlot<0){Messages.error(player,"Nie masz pasującego klucza.");return;}
        if(!slotFree(player)){
            Messages.error(player,"Zwolnij miejsce w ekwipunku.");return;
        }
        Map<String,Integer> table=config.pool(type);
        int total=table.values().stream().mapToInt(Integer::intValue).sum();
        String roll=CratesConfig.choose(table,ThreadLocalRandom.current().nextInt(total));
        if(roll.equals("@event")){
            EventType current=events.active();
            if(current==null){Messages.error(player,"Event nie jest już aktywny.");return;}
            roll=current.itemId();
        }
        ItemStack prize;
        try{
            if(roll.startsWith("minecraft:")){
                Material m=Material.matchMaterial(roll.substring(10));
                if(m==null||!m.isItem())throw new IllegalArgumentException("Nieznany materiał "+roll);
                prize=new ItemStack(m,1);
            }else prize=items.create(roll);
        }catch(RuntimeException error){
            plugin.getLogger().warning("Błędna tabela dropów "+type.id()+": "+error.getMessage());
            Messages.error(player,"Skrzynia wymaga poprawienia konfiguracji.");return;
        }
        ItemStack key=player.getInventory().getItem(keySlot);
        if(key==null)return;
        lastOpened.put(player.getUniqueId(),now);
        if(key.getAmount()==1)player.getInventory().setItem(keySlot,null);
        else key.setAmount(key.getAmount()-1);
        Map<Integer,ItemStack> remaining=player.getInventory().addItem(prize);
        if(!remaining.isEmpty()){
            // Bezstratny fallback w sytuacji równoczesnej zmiany slotów.
            for(ItemStack overflow:remaining.values())player.getWorld().dropItemNaturally(player.getLocation(),overflow);
        }
        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.6f,1.5f);
        Messages.success(player,"Wygrałeś: "+Colors.plain(prize.getItemMeta()==null?
                prize.getType().name():prize.getItemMeta().hasDisplayName()?
                prize.getItemMeta().getDisplayName():prize.getType().name())+"!");
        // Nagroda trafia do ekwipunku przed animacją wyniku: brak utraty przy wyjściu.
        Bukkit.getScheduler().runTask(plugin,()->{
            if(player.isOnline())showResult(player,prize,type);
        });
    }
    private void showResult(Player player,ItemStack prize,CrateType type){
        ResultMenu holder=new ResultMenu();
        Inventory inv=Bukkit.createInventory(holder,27,Colors.color(type.color()+"✦ TWOJA NAGRODA"));
        holder.inv=inv;
        ItemStack bg=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,bg);
        ItemStack display=prize.clone();
        ItemMeta meta=display.getItemMeta();
        if(meta!=null){
            List<Component> lore=new ArrayList<>(meta.hasLore()?meta.lore():List.of());
            lore.add(Colors.color("&#89E5B0✔ Nagroda została już przyznana!"));
            meta.lore(lore);display.setItemMeta(meta);
        }
        inv.setItem(13,display);
        ItemStack close=new ItemStack(Material.BARRIER);
        ItemMeta cm=close.getItemMeta();cm.displayName(Colors.color("&#FF727F✘ Zamknij"));
        close.setItemMeta(cm);inv.setItem(22,close);
        player.openInventory(inv);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void killed(EntityDeathEvent event){
        if(!config.enabled()||!(event.getEntity() instanceof Enemy))return;
        Player player=event.getEntity().getKiller();
        if(player==null)return;
        double roll=ThreadLocalRandom.current().nextDouble();
        if(roll<config.specialKeyChanceFromHostileMob())
            giveKey(player,CrateType.SPECJALNA,1);
        else if(roll<config.specialKeyChanceFromHostileMob()+config.ordinaryKeyChanceFromHostileMob())
            giveKey(player,CrateType.ZWYKLA,1);
    }
    /** Wyłącznie region o nazwie afk, bez dopasowywania nazw częściowych. */
    private boolean inAfkRegion(Location location){
        if(regions==null||!regions.ready()||location==null)return false;
        // Sprawdzamy wskazany region AFK, nie region o największym priorytecie.
        // Dzięki temu podregiony nie zatrzymują naliczania wewnątrz strefy AFK.
        for(Region region:regions.index().all().values())
            if("afk".equalsIgnoreCase(region.name()))
                return region.contains(location.getWorld().getUID(),
                        location.getBlockX(),location.getBlockZ());
        return false;
    }
    /** Zapis aktywności bez SQL i bez interakcji z wątkiem I/O. */
    @EventHandler(priority=EventPriority.MONITOR)
    public void activityJoin(PlayerJoinEvent event){
        activity.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void activityQuit(PlayerQuitEvent event){
        activity.remove(event.getPlayer().getUniqueId());
        lastOpened.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void activityMove(PlayerMoveEvent event){
        Location to=event.getTo(),from=event.getFrom();
        if(to==null||event.getPlayer().isInsideVehicle())return;
        if(!inAfkRegion(to)){
            activity.remove(event.getPlayer().getUniqueId());
            return;
        }
        // Obrót głowy, kamera w bezruchu i przejazd wagonikiem nie liczą się.
        if(from.getBlockX()==to.getBlockX()&&from.getBlockY()==to.getBlockY()
                && from.getBlockZ()==to.getBlockZ())return;
        if(inAfkRegion(event.getPlayer().getLocation()))
            activity.record(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void activityInteract(PlayerInteractEvent event){
        if(inAfkRegion(event.getPlayer().getLocation()))
            activity.record(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void activityBreak(BlockBreakEvent event){
        if(inAfkRegion(event.getPlayer().getLocation()))
            activity.record(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void activityPlace(BlockPlaceEvent event){
        if(inAfkRegion(event.getPlayer().getLocation()))
            activity.record(event.getPlayer().getUniqueId(),System.currentTimeMillis());
    }

    public void tick(){
        if(!config.enabled())return;
        ticks++;
        if(ticks%60==0){
            Map<String,Integer> count=new HashMap<>(storage.get().afkMinutes());
            long now=System.currentTimeMillis();
            for(Player player:Bukkit.getOnlinePlayers()){
                if(!inAfkRegion(player.getLocation())){
                    activity.remove(player.getUniqueId());
                    continue;
                }
                if(!activity.recentlyActive(player.getUniqueId(),now,
                        config.afkActivityWindowMinutes()))continue;
                String id=player.getUniqueId().toString();
                int next=count.getOrDefault(id,0)+1;
                if(next>=config.afkKeyMinutes()){
                    count.put(id,0);
                    giveKey(player,CrateType.AFK,1);
                    Messages.info(player,"&#70D6E8✦ Nagroda za aktywność: klucz AFK.");
                }else count.put(id,next);
            }
            storage.update(old->old.withAfk(count));
        }
        if(!config.particles()&&!config.holograms())return;
        for(CratesState.Position position:storage.get().crates()){
            UUID worldId=UUID.fromString(position.world());
            World world=Bukkit.getWorld(worldId);
            if(world==null||!world.isChunkLoaded(position.x()>>4,position.z()>>4))continue;
            Block block=world.getBlockAt(position.x(),position.y(),position.z());
            if(block.getType()!=Material.valueOf(position.kind().block()))continue;
            if(config.holograms())hologram(position);
            if(config.particles()&&ticks%3==0){
                Location loc=block.getLocation().add(0.5,1.05,0.5);
                if(world.getPlayers().stream().anyMatch(p->p.getLocation().distanceSquared(loc)<24*24))
                    world.spawnParticle(Particle.END_ROD,loc,3,0.28,0.2,0.28,0.002);
            }
        }
    }
    private void hologram(CratesState.Position position){
        if(!config.holograms())return;
        String key=position.key();
        World world=Bukkit.getWorld(UUID.fromString(position.world()));
        if(world==null||!world.isChunkLoaded(position.x()>>4,position.z()>>4))return;
        TextDisplay display=displays.get(key);
        if(display==null||!display.isValid()){
            Location at=new Location(world,position.x()+0.5,position.y()+1.7,position.z()+0.5);
            display=world.spawn(at,TextDisplay.class,d->{
                d.setBillboard(Display.Billboard.CENTER);
                d.setPersistent(false);d.setSeeThrough(true);d.setShadowed(false);
                d.setDefaultBackground(false);
            });
            displays.put(key,display);
        }
        String name=position.kind().color()+"✦ SKRZYNIA "+position.kind().title().toUpperCase(Locale.ROOT);
        if(position.kind()==CrateType.EVENTOWA && events.active()!=null)
            name+="\n&#70D6E8"+events.active().title();
        String rendered=name+"\n&#A8A8B7» Prawy klik, aby otworzyć";
        if(!rendered.equals(lastLabel.put(key,rendered)))
            display.text(Colors.color(rendered));
    }
    @EventHandler public void chunkUnload(ChunkUnloadEvent event){
        for(var iterator=displays.entrySet().iterator();iterator.hasNext();){
            var entry=iterator.next();
            TextDisplay display=entry.getValue();
            if(display.getWorld().equals(event.getWorld()) &&
                    display.getLocation().getBlockX()>>4==event.getChunk().getX() &&
                    display.getLocation().getBlockZ()>>4==event.getChunk().getZ()){
                display.remove();iterator.remove();lastLabel.remove(entry.getKey());
            }
        }
    }
    @Override public void close(){
        for(TextDisplay display:displays.values())display.remove();
        displays.clear();lastLabel.clear();activity.clear();lastOpened.clear();storage.close();
    }
}
