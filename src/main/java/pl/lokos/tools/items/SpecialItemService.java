package pl.lokos.tools.items;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.JsonConfigManager;
import pl.lokos.tools.helpers.Colors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Jeden moduł tworzenia przedmiotów i aktywacji ich efektów. */
public final class SpecialItemService implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey itemKey;
    private final SpecialItemsConfig config;

    public SpecialItemService(JavaPlugin plugin,Path directory) throws IOException {
        this.plugin=plugin;
        this.itemKey=new NamespacedKey(plugin,"special_item");
        this.config=new JsonConfigManager(directory).load("SpecialItems.json",
                SpecialItemsConfig.class,SpecialItemsConfig::new,SpecialItemsConfig::validate);
        for(var entry:config.items().entrySet()){
            if(Material.matchMaterial(entry.getValue().material())==null)
                throw new IOException("Nieznany material w SpecialItems.json: "+entry.getValue().material());
            for(String name:entry.getValue().enchantments().keySet())
                if(Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name))==null)
                    throw new IOException("Nieznane zaklęcie "+name+" w SpecialItems.json.");
        }
    }
    public SpecialItemsConfig config(){return config;}

    public ItemStack create(String id) {
        SpecialItemsConfig.Definition definition=config.get(id);
        if(definition==null||!definition.enabled())throw new IllegalArgumentException(
                "Ten przedmiot jest niedostępny: "+id);
        Material material=Material.matchMaterial(definition.material());
        if(material==null||!material.isItem())throw new IllegalArgumentException(
                "Błędny materiał przedmiotu: "+definition.material());
        ItemStack item=new ItemStack(material);
        ItemMeta meta=item.getItemMeta();
        if(meta==null)throw new IllegalStateException("Nie można nadać wyglądu temu przedmiotowi.");
        meta.displayName(Colors.color(definition.name()));
        meta.lore(definition.lore().stream().map(s->Colors.color(SpecialItemsConfig.format(s,definition))).toList());
        if(definition.unbreaking()>0) {
            var enchant=Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
            if(enchant!=null)meta.addEnchant(enchant,definition.unbreaking(),true);
        }
        for(var entry:definition.enchantments().entrySet()){
            var enchant=Registry.ENCHANTMENT.get(NamespacedKey.minecraft(entry.getKey()));
            if(enchant!=null)meta.addEnchant(enchant,entry.getValue(),true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS,ItemFlag.HIDE_ATTRIBUTES);
        meta.setEnchantmentGlintOverride(true);
        if(definition.customModelData()>0)meta.setCustomModelData(definition.customModelData());
        meta.getPersistentDataContainer().set(itemKey,PersistentDataType.STRING,id);
        item.setItemMeta(meta);
        return item;
    }
    public boolean isSpecial(ItemStack item,String key){
        if(item==null||!item.hasItemMeta())return false;
        return key.equals(item.getItemMeta().getPersistentDataContainer()
                .get(itemKey,PersistentDataType.STRING));
    }
    /**
     * Krótkie odnawianie efektów, aby przedmiot sam przestał działać
     * po zdjęciu. Nigdy nie nadpisuje silniejszego efektu gracza.
     */
    public void tick(){
        for(Player p:Bukkit.getOnlinePlayers()){
            ItemStack boots=p.getInventory().getBoots();
            if(boots==null||!boots.hasItemMeta())continue;
            String id=boots.getItemMeta().getPersistentDataContainer().get(itemKey,PersistentDataType.STRING);
            if(id==null)continue;
            SpecialItemsConfig.Definition definition=config.get(id);
            if(definition==null||!definition.enabled()||!definition.material().equals(boots.getType().name()))continue;
            apply(p,PotionEffectType.SPEED,definition.speedLevel());
            apply(p,PotionEffectType.JUMP_BOOST,definition.jumpLevel());
        }
    }
    private static void apply(Player player,PotionEffectType type,int level){
        if(level<=0)return;
        PotionEffect existing=player.getPotionEffect(type);
        if(existing!=null&&(existing.getAmplifier()>level-1 ||
                existing.getAmplifier()==level-1 && existing.getDuration()>55))return;
        player.addPotionEffect(new PotionEffect(type,60,level-1,true,false,true));
    }
}
