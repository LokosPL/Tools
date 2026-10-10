package pl.lokos.tools.events;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.crates.CrateManager;
import pl.lokos.tools.crates.CrateType;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.GuiTheme;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.items.SpecialItemService;
import pl.lokos.tools.manager.BossBarHub;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionFlag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Jeden aktywny event, trwały zapis; event nie modyfikuje regionów,
 * biomów, worldbordera ani bloków świata. Drop wyłącznie po zatwierdzonej akcji.
 */
public final class EventManager implements Listener,AutoCloseable {
    public static final class Menu implements InventoryHolder {
        private final EventType type;
        private Inventory inv;
        private Menu(EventType type){this.type=type;}
        @Override public Inventory getInventory(){return inv;}
    }
    public static final class ChallengeMenu implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory(){return inv;}
    }
    private final JavaPlugin plugin;
    private final EventConfig config;
    private final StateFile<EventState> storage;
    private final SpecialItemService items;
    private final BossBarHub bars;
    private final RegionManager regions;
    private final NamespacedKey tokenKey;
    private final MeteorShower meteors;
    private final Map<String,Long> recentlyPlaced=new LinkedHashMap<>();
    private final Map<UUID,Integer> challengeBuffer=new HashMap<>();
    private final Set<UUID> pendingDelivery=new HashSet<>();
    private long challengeSession;
    private long challengeSeconds;
    private CrateManager crates;
    private BossBar bar;
    private int visualsCounter;

    public EventManager(JavaPlugin plugin,Path folder,SpecialItemService items,
                        BossBarHub bars,RegionManager regions) throws IOException{
        this.plugin=plugin;this.items=items;this.bars=bars;this.regions=regions;
        config=new JsonConfigManager(folder).load("Events.json",EventConfig.class,
                EventConfig::new,EventConfig::validate);
        storage=new StateFile<>(folder,"EventState.json",EventState.class,EventState::new,EventState::validate);
        tokenKey=new NamespacedKey(plugin,"event_token");
        meteors=new MeteorShower(plugin,this,config,regions);
    }
    public void setCrates(CrateManager crates){this.crates=crates;}
    public MeteorShower meteors(){return meteors;}
    /** Nagroda z jednorazowo zebranego meteorytu; losowa pamiątka jest dodatkiem. */
    void meteorCollected(Player player,int keys,boolean rare){
        if(active()!=EventType.METEORY||crates==null)return;
        crates.giveKey(player,CrateType.EVENTOWA,keys);
        if(rare){
            try{
                ItemStack prize=items.create(EventType.METEORY.itemId());
                for(ItemStack overflow:player.getInventory().addItem(prize).values())
                    player.getWorld().dropItemNaturally(player.getLocation(),overflow);
            }catch(RuntimeException error){
                plugin.getLogger().severe("Nie wydano specjalnej nagrody ze złotego meteorytu: "+error);
            }
        }
        Messages.success(player,(rare?"&#FFD166✦ ZŁOTY METEORYT!":"&#FFD166☄ Znaleziono meteoryt!")
                +" &#89E5B0+"+keys+" klucz(e) eventowe."
                +(rare?" &#FFD166Bonus: specjalny kilof!":""));
        challengeAction(player,EventType.METEORY);
        reward(player,EventType.METEORY);
    }
    public EventConfig config(){return config;}
    public EventState state(){return storage.get();}
    public EventType active(){return storage.get().active(System.currentTimeMillis());}
    public CompletableFuture<Void> start(EventType type,int seconds){
        if(!config.enabled())throw new IllegalArgumentException("Eventy są wyłączone w Events.json.");
        if(type==null||seconds<1||seconds>config.maxDurationMinutes()*60)
            throw new IllegalArgumentException("Niepoprawny typ lub czas eventu.");
        EventType current=active();
        if(current!=null)throw new IllegalArgumentException("Trwa już event "+current.title()+". Zakończ go najpierw.");
        long now=System.currentTimeMillis(),until=now+seconds*1000L;
        return flushChallenges().thenCompose(v->
                storage.update(old->old.started(type,now,until))).thenRun(()->Bukkit.getScheduler()
                .runTask(plugin,()->{
                    render();
                    Bukkit.broadcast(Colors.color("&#FFD166✦ Rozpoczął się event &#70D6E8"+type.title()
                            +"&#A8A8B7! Szczegóły: &#70D6E8/"+type.id()));
                }));
    }
    public CompletableFuture<Void> stop(){
        if(active()==null)throw new IllegalArgumentException("Nie trwa żaden event.");
        return flushChallenges().thenCompose(v->
                storage.update(EventState::ended)).thenRun(()->Bukkit.getScheduler().runTask(plugin,()->{
            render();Bukkit.broadcast(Colors.color("&#FFD166✦ Event zakończony. &#A8A8B7Dziękujemy za udział!"));
        }));
    }
    public ItemStack token(EventType type){
        ItemStack item=new ItemStack(Material.valueOf(type.tokenMaterial()));
        ItemMeta meta=item.getItemMeta();
        meta.displayName(Colors.color(config.tokenName().replace("{event}",type.title())));
        meta.lore(GuiTheme.lore(config.tokenLore()
                .replace("{needed}",Integer.toString(config.tokensForKey()))));
        meta.getPersistentDataContainer().set(tokenKey,PersistentDataType.STRING,type.id());
        item.setItemMeta(meta);
        return item;
    }
    /** Publiczny panel wyboru ośmiu wydarzeń. */
    public void openHub(Player player){
        Menu holder=new Menu(null);
        Inventory inv=Bukkit.createInventory(holder,27,Colors.color("&#FFD166✦ WYDARZENIA SERWERA"));
        holder.inv=inv;
        ItemStack bg=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,bg);
        int[] slots={10,11,12,13,14,15,16,22};
        int n=0;
        for(EventType type:EventType.values()){
            ItemStack icon=new ItemStack(Material.valueOf(type.tokenMaterial()));
            ItemMeta meta=icon.getItemMeta();
            meta.displayName(Colors.color("&#FFD166✦ "+type.title()));
            meta.lore(GuiTheme.lore(
                    "&#A8A8B7"+type.description(),
                    active()==type?"&#89E5B0✔ AKTYWNY":"&#FF727F✘ Nieaktywny",
                    "&#70D6E8» Kliknij, aby zobaczyć szczegóły.",
                    "&#A8A8B7» Komenda: /"+type.id()));
            icon.setItemMeta(meta);
            inv.setItem(slots[n++],icon);
        }
        ItemStack close=new ItemStack(Material.BARRIER);
        ItemMeta c=close.getItemMeta();
        c.displayName(Colors.color("&#FF727F✘ Zamknij panel"));
        close.setItemMeta(c);inv.setItem(26,close);
        ItemStack challenges=new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta cm=challenges.getItemMeta();
        cm.displayName(Colors.color("&#89E5B0✦ WYZWANIA EVENTOWE"));
        cm.lore(GuiTheme.lore("&#A8A8B7» Trzy unikalne cele dla każdego wydarzenia.",
                "&#FFD166» Nagrody: klucze eventowe",
                "&#70D6E8» Kliknij, aby sprawdzić."));
        challenges.setItemMeta(cm);inv.setItem(24,challenges);
        player.openInventory(inv);
    }
    /** Trzy poziomy wyzwań dla aktywnego wydarzenia. */
    public void openChallenges(Player player){
        EventType type=active();
        if(type==null){
            Messages.error(player,"Nie ma aktywnego eventu.");
            openHub(player);
            return;
        }
        ChallengeMenu holder=new ChallengeMenu();
        Inventory inv=Bukkit.createInventory(holder,27,Colors.color("&#89E5B0✦ WYZWANIA: "+type.title()));
        holder.inv=inv;
        ItemStack bg=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int i=0;i<27;i++)inv.setItem(i,bg);
        int progress=state().challengePoints(player.getUniqueId())
                +challengeBuffer.getOrDefault(player.getUniqueId(),0);
        int mask=state().challengeAwarded(player.getUniqueId());
        int[] goals=EventChallenges.goals(type);
        List<String> titles=EventChallenges.names(type);
        int[] slots={10,13,16};
        for(int i=0;i<3;i++){
            boolean done=(mask&(1<<i))!=0;
            ItemStack icon=new ItemStack(done?Material.EMERALD:Material.PAPER);
            ItemMeta meta=icon.getItemMeta();
            meta.displayName(Colors.color(done?"&#89E5B0✔ "+titles.get(i):
                    "&#FFD166✦ "+titles.get(i)));
            meta.lore(GuiTheme.lore("&#A8A8B7Cel: "+EventChallenges.action(type),
                    "&#A8A8B7Postęp: &#FFD166"+Math.min(progress,goals[i])+"/"+goals[i],
                    "&#A8A8B7Nagroda: &#89E5B0"+config.challengeKeyRewards().get(i)+" klucz(e)",
                    done?"&#89E5B0✔ Ukończone":"&#70D6E8» Liczy się tylko uczciwa akcja.",
                    "&#A8A8B7» Nagrody są wypłacane automatycznie."));
            icon.setItemMeta(meta);
            inv.setItem(slots[i],icon);
        }
        ItemStack info=new ItemStack(Material.NETHER_STAR);
        ItemMeta im=info.getItemMeta();
        im.displayName(Colors.color("&#89E5B0✦ Twój postęp"));
        im.lore(GuiTheme.lore("&#A8A8B7Event: "+type.title(),
                "&#A8A8B7Punkty: &#FFD166"+progress,
                "&#A8A8B7Klucze do wypłaty: &#FFD166"+
                        state().pendingKeys(player.getUniqueId()),
                "&#A8A8B7Przy pełnym ekwipunku odbierz klucze później.",
                "&#70D6E8» /eventy wyzwania"));
        info.setItemMeta(im);inv.setItem(4,info);
        ItemStack back=new ItemStack(Material.ARROW);
        ItemMeta bm=back.getItemMeta();
        bm.displayName(Colors.color("&#70D6E8← Lista eventów"));
        back.setItemMeta(bm);inv.setItem(22,back);
        ItemStack close=new ItemStack(Material.BARRIER);
        ItemMeta cm=close.getItemMeta();cm.displayName(Colors.color("&#FF727F✘ Zamknij"));
        close.setItemMeta(cm);inv.setItem(26,close);
        player.openInventory(inv);
    }
    public void open(Player player,EventType type){
        Menu holder=new Menu(type);
        Inventory inv=Bukkit.createInventory(holder,27,
                Colors.color("&#FFD166✦ "+type.title()));
        holder.inv=inv;
        ItemStack bg=GuiTheme.border(Material.BLACK_STAINED_GLASS_PANE);
        for(int i=0;i<27;i++)inv.setItem(i,bg);
        ItemStack header=new ItemStack(Material.valueOf(type.tokenMaterial()));
        ItemMeta h=header.getItemMeta();
        h.displayName(Colors.color("&#FFD166✦ "+type.title()));
        h.lore(GuiTheme.lore(
                "&#A8A8B7"+type.description(),
                " ",
                active()==type?"&#70D6E8✔ Event jest aktywny.":"&#FF727F✘ Event obecnie nieaktywny.",
                "&#A8A8B7» Postęp aktywnego eventu: &#FFD166"+(active()==type?state().progress(player.getUniqueId()):0)
                        +"/"+config.tokensForKey(),
                "&#70D6E8» Zbieraj pamiątki podczas wydarzenia.",
                type==EventType.METEORY?"&#FFD166☄ Meteoryty: "+meteors.activeCount()
                        +" | /meteory gdzie | /meteory namierz"
                        :"&#A8A8B7» Realizuj cele swojego eventu."));
        header.setItemMeta(h);inv.setItem(11,header);
        ItemStack prize;
        try{prize=items.create(type.itemId());}
        catch(RuntimeException ex){prize=new ItemStack(Material.CHEST);}
        ItemMeta p=prize.getItemMeta();
        if(p!=null){
            List<net.kyori.adventure.text.Component> lines=new ArrayList<>(
                    p.hasLore()?p.lore():List.of());
            lines.add(Colors.color(" "));
            lines.add(Colors.color("&#70D6E8» Nagroda w skrzyni eventowej"));
            p.lore(lines);prize.setItemMeta(p);
        }
        inv.setItem(15,prize);
        ItemStack challenge=new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta challengeMeta=challenge.getItemMeta();
        challengeMeta.displayName(Colors.color("&#89E5B0✦ Wyzwania tego eventu"));
        challengeMeta.lore(GuiTheme.lore("&#A8A8B7Trzy cele z nagrodami za postęp.",
                "&#70D6E8» Kliknij, aby przejść do wyzwań."));
        challenge.setItemMeta(challengeMeta);inv.setItem(13,challenge);
        ItemStack back=new ItemStack(Material.ARROW);
        ItemMeta bm=back.getItemMeta();
        bm.displayName(Colors.color("&#70D6E8← Wszystkie eventy"));
        back.setItemMeta(bm);inv.setItem(18,back);
        ItemStack close=new ItemStack(Material.BARRIER);
        ItemMeta c=close.getItemMeta();
        c.displayName(Colors.color("&#FF727F✘ Zamknij"));
        close.setItemMeta(c);inv.setItem(22,close);
        player.openInventory(inv);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof ChallengeMenu){
            event.setCancelled(true);
            if(!(event.getWhoClicked() instanceof Player player))return;
            if(event.getRawSlot()==22)
                Bukkit.getScheduler().runTask(plugin,()->openHub(player));
            else if(event.getRawSlot()==26)
                Bukkit.getScheduler().runTask(plugin,()->player.closeInventory());
            return;
        }
        if(!(event.getView().getTopInventory().getHolder() instanceof Menu holder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player))return;
        if(holder.type==null){
            if(event.getRawSlot()==26){
                Bukkit.getScheduler().runTask(plugin,()->player.closeInventory());
                return;
            }
            if(event.getRawSlot()==24){
                Bukkit.getScheduler().runTask(plugin,()->openChallenges(player));
                return;
            }
            int[] slots={10,11,12,13,14,15,16,22};
            for(int i=0;i<slots.length;i++)if(event.getRawSlot()==slots[i]){
                EventType selected=EventType.values()[i];
                Bukkit.getScheduler().runTask(plugin,()->open(player,selected));
                return;
            }
            return;
        }
        if(event.getRawSlot()==18){
            Bukkit.getScheduler().runTask(plugin,()->openHub(player));
        }else if(event.getRawSlot()==13){
            Bukkit.getScheduler().runTask(plugin,()->openChallenges(player));
        }else if(event.getRawSlot()==22){
            Bukkit.getScheduler().runTask(plugin,()->player.closeInventory());
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof Menu ||
                event.getView().getTopInventory().getHolder() instanceof ChallengeMenu)
            event.setCancelled(true);
    }
    /** Punkt za rzeczywiście ukończoną akcję. Zapis zbiorczy co kilka sekund. */
    private void challengeAction(Player player,EventType type){
        if(!config.challengesEnabled()||type!=active()||
                player.getGameMode()!=GameMode.SURVIVAL)return;
        long session=state().startedAt();
        if(challengeSession!=session){
            challengeBuffer.clear();
            challengeSession=session;
        }
        challengeBuffer.merge(player.getUniqueId(),1,(a,b)->Math.min(1000,a+b));
    }
    private CompletableFuture<Void> flushChallenges(){
        if(challengeBuffer.isEmpty())return CompletableFuture.completedFuture(null);
        Map<UUID,Integer> batch=Map.copyOf(challengeBuffer);
        challengeBuffer.clear();
        EventState snapshot=state();
        EventType type=EventType.parse(snapshot.type());
        long session=snapshot.startedAt();
        if(type==null)return CompletableFuture.completedFuture(null);
        int[] goals=EventChallenges.goals(type);
        List<Integer> rewards=config.challengeKeyRewards();
        CompletableFuture<Void> future=storage.update(old->{
            EventState next=old;
            for(var entry:batch.entrySet())
                next=next.challengeActions(entry.getKey(),entry.getValue(),type,session,
                        goals,rewards);
            return next;
        });
        future.whenComplete((ignored,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(error!=null){
                    plugin.getLogger().severe("Nie zapisano wyzwań eventowych: "+error);
                    if(state().startedAt()==session)
                        batch.forEach((uuid,count)->challengeBuffer.merge(uuid,count,
                                (a,b)->Math.min(1000,a+b)));
                }else for(UUID uuid:batch.keySet()){
                    Player player=Bukkit.getPlayer(uuid);
                    if(player!=null)deliverChallengeKeys(player);
                }
            });
        });
        return future;
    }

    /** Wypłata nagród po zapisie wyzwania; przy pełnym ekwipunku odbiór później. */
    public void deliverChallengeKeys(Player player){
        if(crates==null||!player.isOnline())return;
        UUID uuid=player.getUniqueId();
        int amount=Math.min(16,state().pendingKeys(uuid));
        if(amount==0||!pendingDelivery.add(uuid))return;
        if(player.getInventory().firstEmpty()<0){
            pendingDelivery.remove(uuid);
            return;
        }
        try{
            crates.giveKey(player,CrateType.EVENTOWA,amount);
        }catch(RuntimeException error){
            pendingDelivery.remove(uuid);
            plugin.getLogger().severe("Nie wydano kluczy wyzwania: "+error);
            return;
        }
        // Dopiero po wydaniu kluczy próbujemy zamknąć kolejkę.
        // W przypadku błędu zapisu blokujemy kolejną automatyczną wypłatę
        // do restartu, żeby nie mnożyć nagród podczas awarii dysku.
        storage.update(old->old.keysDelivered(uuid,amount)).whenComplete((v,error)->{
            if(error!=null){
                plugin.getLogger().severe("Nie potwierdzono odbioru kluczy wyzwań: "+error);
                return;
            }
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                pendingDelivery.remove(uuid);
                if(player.isOnline())
                    Messages.success(player,"&#FFD166✦ Wyzwanie: odebrano &#89E5B0"
                            +amount+" kluczy eventowych!");
            });
        });
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void challengeJoin(PlayerJoinEvent event){
        UUID uuid=event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            Player player=Bukkit.getPlayer(uuid);
            if(player!=null)deliverChallengeKeys(player);
        },20L);
    }

    private boolean roll(double chance){return ThreadLocalRandom.current().nextDouble()<chance;}
    private void reward(Player player,EventType type){
        if(type==null||!roll(config.tokenChance()))return;
        ItemStack earned=token(type);
        Map<Integer,ItemStack> overflow=player.getInventory().addItem(earned);
        for(ItemStack item:overflow.values())player.getWorld().dropItemNaturally(player.getLocation(),item);
        player.playSound(player.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,0.4f,1.4f);
        org.bukkit.Particle particle=switch(type){
            case ZIMA -> Particle.SNOWFLAKE;
            case HALLOWEEN -> Particle.SOUL_FIRE_FLAME;
            case WIELKANOC, ZNIWA -> Particle.HAPPY_VILLAGER;
            case LATO, WEDKOWANIE -> Particle.SPLASH;
            case ZABOJSTWA -> Particle.CRIT;
            case METEORY -> Particle.END_ROD;
        };
        player.spawnParticle(particle,player.getLocation().add(0,1,0),8,0.5,0.4,0.5,0.01);
        int next=state().progress(player.getUniqueId())+1;
        if(next>=config.tokensForKey()){
            next=0;
            if(crates!=null)crates.giveKey(player,CrateType.EVENTOWA,1);
            Messages.success(player,"Zdobyto klucz do skrzyni eventowej!");
        }else if(roll(config.eventKeyChance())&&crates!=null)
            crates.giveKey(player,CrateType.EVENTOWA,1);
        final int progress=next;
        if(progress>0)Messages.info(player,"&#70D6E8✦ Pamiątka eventowa &#A8A8B7» "+
                progress+"/"+config.tokensForKey());
        storage.update(old->old.progress(player.getUniqueId(),progress));
    }
    private static String position(Block block){
        return block.getWorld().getUID()+":"+block.getX()+":"+block.getY()+":"+block.getZ();
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void place(BlockPlaceEvent event){
        if(recentlyPlaced.size()>30000){
            Iterator<String> oldest=recentlyPlaced.keySet().iterator();
            if(oldest.hasNext()){oldest.next();oldest.remove();}
        }
        recentlyPlaced.put(position(event.getBlockPlaced()),System.currentTimeMillis());
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void breakBlock(BlockBreakEvent event){
        EventType type=active();
        if(type==null || event.getPlayer().getGameMode()!=GameMode.SURVIVAL)return;
        if(recentlyPlaced.remove(position(event.getBlock()))!=null)return;
        Material material=event.getBlock().getType();
        boolean valid=switch(type){
            case ZIMA -> material.isBlock() && material.isSolid() && material!=Material.BEDROCK;
            case WIELKANOC -> material.name().contains("FLOWER") ||
                    material==Material.GRASS_BLOCK ||material==Material.SHORT_GRASS||
                    material==Material.DANDELION ||material==Material.POPPY;
            case ZNIWA -> switch(material){case WHEAT,CARROTS,POTATOES,BEETROOTS,
                    NETHER_WART->true;default->false;}
                    && event.getBlock().getBlockData() instanceof Ageable ripe
                    && ripe.getAge()==ripe.getMaximumAge();
            case METEORY -> material.name().endsWith("_ORE")||material==Material.ANCIENT_DEBRIS;
            default -> false;
        };
        if(valid){
            if(type!=EventType.METEORY)challengeAction(event.getPlayer(),type);
            reward(event.getPlayer(),type);
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void fishing(PlayerFishEvent event){
        EventType type=active();
        if((type==EventType.LATO||type==EventType.WEDKOWANIE)
                && event.getState()==PlayerFishEvent.State.CAUGHT_FISH)
        {
            challengeAction(event.getPlayer(),type);
            reward(event.getPlayer(),type);
        }
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void mobDeath(EntityDeathEvent event){
        EventType type=active();
        if(type!=EventType.HALLOWEEN||!(event.getEntity() instanceof Enemy))return;
        Player killer=event.getEntity().getKiller();
        if(killer!=null){
            challengeAction(killer,type);
            reward(killer,type);
        }
    }
    /** Nie punktuj walki na spawnie ani w regionie z zakazem PvP. */
    private boolean protectedPvP(Location loc){
        if(regions==null||!regions.ready())return true; // brak wczytanych regionów = brak nagród
        Region spawn=regions.mainSpawn();
        if(spawn!=null && spawn.contains(loc.getWorld().getUID(),loc.getBlockX(),loc.getBlockZ()))
            return true;
        return regions.protectedLocation(loc,RegionFlag.PVP);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void playerDeath(PlayerDeathEvent event){
        if(active()!=EventType.ZABOJSTWA)return;
        Player victim=event.getEntity(),killer=victim.getKiller();
        if(killer==null)return;
        if(!PvPKillPolicy.eligible(killer.getUniqueId(),victim.getUniqueId(),
                killer.getGameMode()==GameMode.SURVIVAL,
                victim.getGameMode()==GameMode.SURVIVAL,
                protectedPvP(killer.getLocation()),protectedPvP(victim.getLocation())))return;
        long now=System.currentTimeMillis();
        long interval=config.minimumPvPKillIntervalSeconds()*1000L;
        if(!state().canRewardPvPKill(killer.getUniqueId(),victim.getUniqueId(),now,interval))return;
        // Wpis antyfarmowy aktualizujemy przed losowaniem nagrody.
        // Jeden asynchroniczny StateFile zapisuje też historię par po restarcie.
        storage.update(old->old.withPvPKill(killer.getUniqueId(),victim.getUniqueId(),now,interval))
                .whenComplete((ignored,error)->{
                    if(!plugin.isEnabled())return;
                    Bukkit.getScheduler().runTask(plugin,()->{
                        if(error!=null){
                            plugin.getLogger().severe("Nie zapisano antyfarmu PvP: "+error);
                            return;
                        }
                        if(killer.isOnline() && active()==EventType.ZABOJSTWA){
                            challengeAction(killer,EventType.ZABOJSTWA);
                            reward(killer,EventType.ZABOJSTWA);
                        }
                    });
                });
    }
    public void tick(){
        if(++challengeSeconds%config.challengeFlushSeconds()==0)
            flushChallenges();
        if(challengeSeconds%20==0 && !state().pendingKeys().isEmpty())
            for(Player player:Bukkit.getOnlinePlayers())
                if(state().pendingKeys(player.getUniqueId())>0)deliverChallengeKeys(player);
        meteors.tick();
        EventType type=active();
        if(type==null){
            if(state().type()!=null){
                storage.update(EventState::ended);
                Bukkit.broadcast(Colors.color("&#FFD166✦ Event dobiegł końca."));
            }
            render();
            return;
        }
        render();
        if(type==EventType.ZIMA && config.snowParticles() && ++visualsCounter%3==0){
            for(Player player:Bukkit.getOnlinePlayers()){
                if(player.getWorld().getEnvironment()!=World.Environment.NORMAL)continue;
                // Śnieg jako lokalny efekt wizualny w każdym biomie; bez zmiany bloków,
                // temperatury biomu i istniejących flag regionów.
                Location center=player.getLocation().add(0,5,0);
                player.spawnParticle(Particle.SNOWFLAKE,center,config.snowParticleCount(),
                        10,3,10,0.015);
            }
        }
        if(recentlyPlaced.size()>20000)recentlyPlaced.entrySet().removeIf(
                e->System.currentTimeMillis()-e.getValue()>3600000L);
    }
    private void render(){
        EventType type=active();
        if(type==null){
            if(bar!=null){bar=null;bars.setGlobal(BossBarHub.Slot.EVENT,null,null);}
            return;
        }
        if(bar==null){
            bar=BossBar.bossBar(net.kyori.adventure.text.Component.empty(),1f,
                    BossBar.Color.YELLOW,BossBar.Overlay.PROGRESS);
            bars.setGlobal(BossBarHub.Slot.EVENT,bar,null);
        }
        long seconds=Math.max(0,(state().endsAt()-System.currentTimeMillis()+999)/1000);
        String time=seconds>=86400?(seconds/86400)+"d "+((seconds%86400)/3600)+"h":
                seconds>=3600?(seconds/3600)+"h "+((seconds%3600)/60)+"m":
                        (seconds/60)+"m "+(seconds%60)+"s";
        bar.name(Colors.color(config.eventBar().replace("{event}",type.title())
                .replace("{time}",time).replace("{command}",type.id())));
        float length=Math.max(1,state().endsAt()-state().startedAt());
        bar.progress(Math.max(0,Math.min(1,(state().endsAt()-System.currentTimeMillis())/length)));
    }
    @Override public void close(){
        flushChallenges();
        meteors.close();
        bars.setGlobal(BossBarHub.Slot.EVENT,null,null);
        storage.close();
    }
}
