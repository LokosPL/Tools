package pl.lokos.tools.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Ochrona regionow, takze zmian z sasiadujacych chunkow. Bez SQL w eventach. */
public final class RegionProtectionListener implements Listener {
    private final RegionManager regions;
    private final RegionSelection selection;
    private final NamespacedKey wandKey;

    public RegionProtectionListener(RegionManager regions, RegionSelection selection, NamespacedKey wandKey) {
        this.regions=regions;this.selection=selection;this.wandKey=wandKey;
    }

    private boolean denies(Player player, Location loc, RegionFlag flag) {
        if(loc==null)return false;
        if(!regions.ready())return true;
        return (player==null||!regions.bypass(player)) && regions.protectedLocation(loc,flag);
    }
    private boolean denies(Location loc,RegionFlag flag) {
        if(loc==null)return false;
        if(!regions.ready())return true;
        return regions.protectedLocation(loc,flag);
    }
    private boolean deniesEither(Location a,Location b,RegionFlag flag) {
        return denies(a,flag) || denies(b,flag);
    }
    private void blocked(Player player) {
        // actionbar zamiast zalewania chatu przy kazdym uderzeniu.
        player.sendActionBar(pl.lokos.tools.helpers.Colors.color("&cᴛᴇɴ ᴏʙꜱᴢᴀʀ ᴊᴇꜱᴛ ᴄʜʀᴏɴɪᴏɴʏ"));
    }

    public boolean isWand(ItemStack item) {
        if(item==null || item.getType()!=Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        Byte flag=item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.BYTE);
        return flag!=null && flag==1;
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void wand(PlayerInteractEvent e) {
        if (e.getClickedBlock()==null || !isWand(e.getItem())) return;
        Player player=e.getPlayer();
        if (!player.hasPermission("tools.region.admin")) return;
        boolean first=e.getAction()==Action.LEFT_CLICK_BLOCK;
        boolean second=e.getAction()==Action.RIGHT_CLICK_BLOCK;
        if (!first && !second) return;
        e.setCancelled(true);
        selection.choose(player.getUniqueId(),first,e.getClickedBlock().getLocation());
        Location at=e.getClickedBlock().getLocation();
        Messages.info(player,(first?"Punkt pierwszy":"Punkt drugi")+
                " &8» &a"+at.getBlockX()+"&7, &a"+at.getBlockY()+"&7, &a"+at.getBlockZ());
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void breakBlock(BlockBreakEvent e) {
        if(denies(e.getPlayer(),e.getBlock().getLocation(),RegionFlag.BREAK)) {
            e.setCancelled(true);blocked(e.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void place(BlockPlaceEvent e) {
        if(denies(e.getPlayer(),e.getBlockPlaced().getLocation(),RegionFlag.BUILD)) {
            e.setCancelled(true);blocked(e.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void fluidFlow(BlockFromToEvent e) {
        if(deniesEither(e.getBlock().getLocation(),e.getToBlock().getLocation(),RegionFlag.FLUIDS))
            e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void emptyBucket(PlayerBucketEmptyEvent e) {
        if(denies(e.getPlayer(),e.getBlock().getLocation(),RegionFlag.FLUIDS)) {
            e.setCancelled(true);blocked(e.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void fillBucket(PlayerBucketFillEvent e) {
        if(denies(e.getPlayer(),e.getBlock().getLocation(),RegionFlag.FLUIDS)) {
            e.setCancelled(true);blocked(e.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void ignite(BlockIgniteEvent e) {
        if (e.getPlayer()!=null && regions.bypass(e.getPlayer())) return;
        if (denies(e.getBlock().getLocation(),RegionFlag.FIRE)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void burn(BlockBurnEvent e) {
        if (denies(e.getBlock().getLocation(),RegionFlag.FIRE)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void dispense(BlockDispenseEvent e) {
        Location origin=e.getBlock().getLocation();
        Location destination=e.getBlock().getBlockData() instanceof org.bukkit.block.data.Directional facing
                ? e.getBlock().getRelative(facing.getFacing()).getLocation() : origin;
        if(denies(origin,RegionFlag.INTERACT) || denies(destination,RegionFlag.FLUIDS))
            e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void grow(BlockGrowEvent e) {
        if(denies(e.getBlock().getLocation(),RegionFlag.BUILD))e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void spread(BlockSpreadEvent e) {
        if (deniesEither(e.getSource().getLocation(),e.getBlock().getLocation(),RegionFlag.BUILD))
            e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void fade(BlockFadeEvent e) {
        if (denies(e.getBlock().getLocation(),RegionFlag.BUILD)) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void pistonExtend(BlockPistonExtendEvent e) {
        if(denies(e.getBlock().getLocation(),RegionFlag.PISTONS)) {e.setCancelled(true);return;}
        for(Block block:e.getBlocks()) {
            if(deniesEither(block.getLocation(),block.getRelative(e.getDirection()).getLocation(),RegionFlag.PISTONS)) {
                e.setCancelled(true);return;
            }
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void pistonRetract(BlockPistonRetractEvent e) {
        if(denies(e.getBlock().getLocation(),RegionFlag.PISTONS)) {e.setCancelled(true);return;}
        for(Block block:e.getBlocks()) {
            if(deniesEither(block.getLocation(),block.getRelative(e.getDirection()).getLocation(),RegionFlag.PISTONS)) {
                e.setCancelled(true);return;
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void explosion(EntityExplodeEvent e) {
        e.blockList().removeIf(block->denies(block.getLocation(),RegionFlag.EXPLOSIONS));
        if(denies(e.getLocation(),RegionFlag.EXPLOSIONS))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void blockExplosion(BlockExplodeEvent e) {
        e.blockList().removeIf(block->denies(block.getLocation(),RegionFlag.EXPLOSIONS));
        if(denies(e.getBlock().getLocation(),RegionFlag.EXPLOSIONS))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void mobs(CreatureSpawnEvent e) {
        if(!denies(e.getLocation(),RegionFlag.MOBS)) return;
        if(e.getSpawnReason()==CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            boolean adminNearby=e.getLocation().getNearbyPlayers(5.0).stream().anyMatch(regions::bypass);
            if(adminNearby) {
                e.getEntity().getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE,(byte)2);
                return;
            }
        }
        e.setCancelled(true);
    }
    /** Moby, ktore wchodza na spawn z zewnatrz, sa usuwane. */
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void mobMove(io.papermc.paper.event.entity.EntityMoveEvent event) {
        if(event.getEntity() instanceof Player)return;
        if(event.getEntity().getPersistentDataContainer().has(wandKey,PersistentDataType.BYTE)
                && Byte.valueOf((byte)2).equals(event.getEntity().getPersistentDataContainer()
                        .get(wandKey,PersistentDataType.BYTE)))return;
        if(denies(event.getTo(),RegionFlag.MOBS))event.getEntity().remove();
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void chunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        if(!regions.ready())return;
        for(Entity entity:event.getChunk().getEntities()) {
            if(!(entity instanceof org.bukkit.entity.Mob))continue;
            Byte override=entity.getPersistentDataContainer().get(wandKey,PersistentDataType.BYTE);
            if(override!=null&&override==2)continue;
            if(denies(entity.getLocation(),RegionFlag.MOBS))entity.remove();
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void entityBlocks(EntityChangeBlockEvent e) {
        if(denies(e.getBlock().getLocation(),RegionFlag.BUILD)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hangingPlace(HangingPlaceEvent e) {
        if(denies(e.getPlayer(),e.getEntity().getLocation(),RegionFlag.BUILD))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hangingBreak(HangingBreakEvent e) {
        if(e instanceof HangingBreakByEntityEvent byEntity
                && byEntity.getRemover() instanceof Player player && regions.bypass(player))return;
        if(denies(e.getEntity().getLocation(),RegionFlag.BREAK))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interact(PlayerInteractEvent e) {
        if(e.getClickedBlock()==null || isWand(e.getItem())) return;
        if (e.getAction()!=Action.RIGHT_CLICK_BLOCK && e.getAction()!=Action.PHYSICAL) return;
        if(denies(e.getPlayer(),e.getClickedBlock().getLocation(),RegionFlag.INTERACT))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interactEntity(PlayerInteractEntityEvent e) {
        if(denies(e.getPlayer(),e.getRightClicked().getLocation(),RegionFlag.INTERACT)) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void manipulate(PlayerArmorStandManipulateEvent e) {
        if(denies(e.getPlayer(),e.getRightClicked().getLocation(),RegionFlag.INTERACT))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void inventories(InventoryMoveItemEvent e) {
        if(denies(e.getSource().getLocation(),RegionFlag.INTERACT)
                || denies(e.getDestination().getLocation(),RegionFlag.INTERACT))
            e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void damage(EntityDamageEvent e) {
        Entity victim=e.getEntity();
        Region target=regions.at(victim.getLocation());
        if(target==null && !regions.inHalo(victim.getLocation()))return;
        if(e instanceof EntityDamageByEntityEvent attack) {
            Entity attacker=attack.getDamager();
            Player player=attacker instanceof Player p ? p
                    : attacker instanceof Projectile projectile && projectile.getShooter() instanceof Player p ? p : null;
            if(player!=null && regions.bypass(player)) return;
            if (player != null && victim instanceof Player) {
                Region attackerRegion=regions.at(player.getLocation());
                if(denies(player,victim.getLocation(),RegionFlag.PVP)
                        || denies(player,player.getLocation(),RegionFlag.PVP))e.setCancelled(true);
                return;
            }
        }
        if(denies(victim.getLocation(),RegionFlag.DAMAGE))e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void enter(PlayerMoveEvent e) {
        if(e.getTo()==null || (e.getFrom().getWorld().equals(e.getTo().getWorld())
                && e.getFrom().getBlockX()==e.getTo().getBlockX()
                && e.getFrom().getBlockZ()==e.getTo().getBlockZ())) return;
        Region next=regions.at(e.getTo());
        if(next!=null && !regions.canEnter(e.getPlayer(),next)) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(pl.lokos.tools.helpers.Colors.color(
                    "&cʙʀᴀᴋ ᴅᴏꜱᴛᴇ̨ᴘᴜ &8» &7Wymagana wyższa ranga"));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void teleport(PlayerTeleportEvent e) {
        if(e.getTo()!=null) {
            Region next=regions.at(e.getTo());
            if(next!=null && !regions.canEnter(e.getPlayer(),next))e.setCancelled(true);
        }
    }
}
