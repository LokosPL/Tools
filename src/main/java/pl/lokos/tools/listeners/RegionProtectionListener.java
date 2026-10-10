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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.security.ToolsAccess;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.manager.PlayerStatusBar;
import pl.lokos.tools.region.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Ochrona regionow, takze zmian z sasiadujacych chunkow. Bez SQL w eventach. */
public final class RegionProtectionListener implements Listener {
    private static final pl.lokos.tools.helpers.CommandMessages display = new pl.lokos.tools.helpers.CommandMessages("region");
    private final RegionManager regions;
    private final PlayerStatusBar statusBar;
    private final RegionSelection selection;
    private final NamespacedKey wandKey;
    private final String adminPermission;

    public RegionProtectionListener(RegionManager regions, RegionSelection selection, NamespacedKey wandKey,
                                   String adminPermission, PlayerStatusBar statusBar) {
        this.regions=regions;this.selection=selection;this.wandKey=wandKey;this.adminPermission=adminPermission;
        this.statusBar=statusBar;
    }

    private boolean denies(Player player, Location loc, RegionFlag flag) {
        if(loc==null)return false;
        if(!regions.ready())return true;
        return regions.protectedLocation(player,loc,flag);
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
        statusBar.protectedArea(player);
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
        if (!ToolsAccess.admin(player,regions.ranks(),adminPermission)) return;
        boolean first=e.getAction()==Action.LEFT_CLICK_BLOCK;
        boolean second=e.getAction()==Action.RIGHT_CLICK_BLOCK;
        if (!first && !second) return;
        e.setCancelled(true);
        selection.choose(player.getUniqueId(),first,e.getClickedBlock().getLocation());
        Location at=e.getClickedBlock().getLocation();
        display.info(player,(first?"Punkt pierwszy":"Punkt drugi")+
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
        if(denies(e.getPlayer(),e.getBlock().getLocation(),RegionFlag.FLUIDS)
                || denies(e.getPlayer(),e.getBlock().getRelative(e.getBlockFace()).getLocation(),RegionFlag.FLUIDS)) {
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
        if (e.getPlayer()!=null
                ? denies(e.getPlayer(),e.getBlock().getLocation(),RegionFlag.FIRE)
                : denies(e.getBlock().getLocation(),RegionFlag.FIRE))e.setCancelled(true);
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
        e.setCancelled(true);
    }
    /** Moby, ktore wchodza na spawn z zewnatrz, sa usuwane. */
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void mobMove(io.papermc.paper.event.entity.EntityMoveEvent event) {
        if(!regions.ready())return;
        if(!(event.getEntity() instanceof org.bukkit.entity.Mob))return;
        if(event.getEntity().getPersistentDataContainer().has(wandKey,PersistentDataType.BYTE)
                && Byte.valueOf((byte)2).equals(event.getEntity().getPersistentDataContainer()
                        .get(wandKey,PersistentDataType.BYTE)))return;
        if(event.getFrom().getBlockX()==event.getTo().getBlockX()
                && event.getFrom().getBlockZ()==event.getTo().getBlockZ())return;
        if(!denies(event.getFrom(),RegionFlag.MOBS)
                && denies(event.getTo(),RegionFlag.MOBS))event.getEntity().remove();
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
        RegionFlag flag=e.getEntity() instanceof org.bukkit.entity.ItemFrame
                ? RegionFlag.ITEM_FRAMES : RegionFlag.BUILD;
        if(denies(e.getPlayer(),e.getEntity().getLocation(),flag))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void hangingBreak(HangingBreakEvent e) {
        RegionFlag flag=e.getEntity() instanceof org.bukkit.entity.ItemFrame
                ? RegionFlag.ITEM_FRAMES : RegionFlag.BREAK;
        Player actor=e instanceof HangingBreakByEntityEvent breakByEntity
                && breakByEntity.getRemover() instanceof Player player ? player : null;
        if(actor!=null ? denies(actor,e.getEntity().getLocation(),flag)
                : denies(e.getEntity().getLocation(),flag))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interact(PlayerInteractEvent e) {
        if(e.getClickedBlock()==null ||
                (isWand(e.getItem()) && regions.canManage(e.getPlayer()))) return;
        if (e.getAction()!=Action.RIGHT_CLICK_BLOCK && e.getAction()!=Action.PHYSICAL) return;
        RegionFlag flag=interactionFlag(e.getClickedBlock().getType());
        if(denies(e.getPlayer(),e.getClickedBlock().getLocation(),flag)) {
            e.setCancelled(true);
            blocked(e.getPlayer());
        }
    }
    private static RegionFlag interactionFlag(Material m) {
        String name=m.name();
        if(name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER_BOX")
                || name.equals("ENDER_CHEST"))return RegionFlag.CHESTS;
        if(name.contains("CRAFTING_TABLE") || name.contains("CRAFTER"))return RegionFlag.CRAFTING;
        if(name.contains("FURNACE") || name.contains("SMOKER") || name.contains("BLAST_FURNACE"))
            return RegionFlag.FURNACES;
        if(name.contains("ANVIL") || name.contains("GRINDSTONE") || name.contains("SMITHING_TABLE"))
            return RegionFlag.ANVILS;
        if(name.equals("ENCHANTING_TABLE"))return RegionFlag.ENCHANTING;
        if(name.equals("BREWING_STAND"))return RegionFlag.BREWING;
        if(name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE_GATE"))
            return RegionFlag.DOORS;
        if(name.endsWith("_BUTTON"))return RegionFlag.BUTTONS;
        if(name.equals("LEVER"))return RegionFlag.LEVERS;
        if(name.endsWith("_PRESSURE_PLATE"))return RegionFlag.PRESSURE_PLATES;
        if(name.equals("HOPPER"))return RegionFlag.HOPPERS;
        return RegionFlag.INTERACT;
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interactEntity(PlayerInteractEntityEvent e) {
        RegionFlag flag=e.getRightClicked() instanceof org.bukkit.entity.ItemFrame
                ? RegionFlag.ITEM_FRAMES : e.getRightClicked() instanceof org.bukkit.entity.Vehicle
                ? RegionFlag.VEHICLES : RegionFlag.INTERACT;
        if(denies(e.getPlayer(),e.getRightClicked().getLocation(),flag)) {
            e.setCancelled(true);blocked(e.getPlayer());
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void manipulate(PlayerArmorStandManipulateEvent e) {
        if(denies(e.getPlayer(),e.getRightClicked().getLocation(),RegionFlag.ARMOR_STANDS)) {
            e.setCancelled(true);blocked(e.getPlayer());
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void inventories(InventoryMoveItemEvent e) {
        if(denies(e.getSource().getLocation(),RegionFlag.HOPPERS)
                || denies(e.getDestination().getLocation(),RegionFlag.HOPPERS))
            e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void crafting(CraftItemEvent event){
        if(event.getWhoClicked() instanceof Player player &&
                denies(player,player.getLocation(),RegionFlag.CRAFTING)) {
            event.setCancelled(true);blocked(player);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void vehicleDestroy(org.bukkit.event.vehicle.VehicleDestroyEvent event){
        Player p=event.getAttacker() instanceof Player player?player:null;
        if(denies(p,event.getVehicle().getLocation(),RegionFlag.VEHICLES)) {
            event.setCancelled(true);
            if(p!=null)blocked(p);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void damage(EntityDamageEvent e) {
        Entity victim=e.getEntity();
        // Sprawdzamy również pozycję atakującego. Atak z regionu chronionego
        // w gracza tuż poza granicą nie może obchodzić flagi PvP.
        if(e instanceof EntityDamageByEntityEvent attack) {
            Entity attacker=attack.getDamager();
            Player player=attacker instanceof Player p ? p
                    : attacker instanceof Projectile projectile && projectile.getShooter() instanceof Player p ? p : null;
            if (player != null) {
                if (victim instanceof Player) {
                    if (denies(player,victim.getLocation(),RegionFlag.PVP)
                            || denies(player,player.getLocation(),RegionFlag.PVP)) {
                        e.setCancelled(true);blocked(player);
                    }
                } else if (denies(player,victim.getLocation(),RegionFlag.DAMAGE)) {
                    e.setCancelled(true);blocked(player);
                }
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
            statusBar.deniedEntry(e.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drop(PlayerDropItemEvent event){
        if(denies(event.getPlayer(),event.getPlayer().getLocation(),RegionFlag.ITEMS_DROP)) {
            event.setCancelled(true);blocked(event.getPlayer());
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void pickup(EntityPickupItemEvent event){
        if(event.getEntity() instanceof Player p && denies(p,p.getLocation(),RegionFlag.ITEMS_PICKUP)) {
            event.setCancelled(true);blocked(p);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void portal(PlayerPortalEvent event){
        if(denies(event.getPlayer(),event.getFrom(),RegionFlag.PORTALS)
                || (event.getTo()!=null && denies(event.getPlayer(),event.getTo(),RegionFlag.PORTALS))) {
            event.setCancelled(true);blocked(event.getPlayer());
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void launch(ProjectileLaunchEvent event){
        if(!(event.getEntity().getShooter() instanceof Player player))return;
        if(event.getEntity() instanceof org.bukkit.entity.EnderPearl
                && denies(player,player.getLocation(),RegionFlag.ENDER_PEARLS)) {
            event.setCancelled(true);blocked(player);
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
