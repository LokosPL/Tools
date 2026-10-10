package pl.lokos.tools.helpers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/**
 * Wspólna paleta GUI Tools. Dark / gold / cyan, szare objaśnienia,
 * krótkie instrukcje i spójny układ. Bez zasobów innych pluginów.
 */
public final class GuiTheme {
    public static final String GOLD="&#FFD166";
    public static final String CYAN="&#70D6E8";
    public static final String MINT="&#86E6BC";
    public static final String RED="&#FF727F";
    public static final String GRAY="&#A8A8B7";
    public static final String DIM="&#737388";
    public static final String RULE="&8&m                              ";

    private GuiTheme() {}

    public static String title(String text) {
        return GOLD+"» &l"+Colors.plain(text)+" &8«";
    }

    public static String itemTitle(String original) {
        if(original.startsWith("&c"))return RED+original.substring(2);
        if(original.startsWith("&7"))return GRAY+original.substring(2);
        if(original.startsWith("&a"))return GOLD+original.substring(2);
        if(original.startsWith("&#"))return original;
        return GOLD+original;
    }

    public static List<net.kyori.adventure.text.Component> lore(String... lines) {
        List<net.kyori.adventure.text.Component> formatted=new ArrayList<>();
        for(String raw:lines) {
            if(raw==null)continue;
            String line=raw;
            if(line.startsWith("&8────"))line=RULE;
            if(line.startsWith("&aKliknij"))line=MINT+"» "+line.substring("&a".length());
            else if(line.startsWith("&7Kliknij"))line=CYAN+"» "+line.substring("&7".length());
            else if(line.startsWith("&7"))line=GRAY+line.substring(2);
            if(line.startsWith("&8 " ))line= " ";
            formatted.add(Colors.color(line));
        }
        return List.copyOf(formatted);
    }

    public static ItemStack border(Material material){
        ItemStack stack=new ItemStack(material);
        ItemMeta meta=stack.getItemMeta();
        meta.displayName(Colors.color("&8 "));
        stack.setItemMeta(meta);
        return stack;
    }
    public static void frame(Inventory inventory){
        ItemStack black=border(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack gray=border(Material.GRAY_STAINED_GLASS_PANE);
        for(int n=0;n<9;n++)if(inventory.getItem(n)==null)inventory.setItem(n,n%2==0?black:gray);
        for(int n=45;n<54;n++)if(inventory.getItem(n)==null)inventory.setItem(n,n%2==0?black:gray);
    }
}
