package pl.lokos.tools.helpers;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Jeden styl HEX i nieinwazyjne dźwięki sukcesu oraz błędu. */
public final class Messages {
    private Messages() {}
    private static volatile String prefix="";
    private static final Map<Player,Long> lastSound=new WeakHashMap<>();
    private static final Map<CommandSender,java.util.Map.Entry<String,Long>> lastUnchanged=new WeakHashMap<>();
    public static void configure(String value){prefix=value==null?"":value;}
    private static String format(String text){return prefix+text;}

    private static void notifySound(CommandSender to,boolean success){
        // API dźwięków Paper tylko na głównym wątku; callbacki SQL nie wykonują Bukkit API.
        if(!(to instanceof Player player) || !Bukkit.isPrimaryThread())return;
        long now=System.currentTimeMillis();
        Long previous=lastSound.get(player);
        if(previous!=null && now-previous<300)return;
        lastSound.put(player,now);
        player.playSound(player.getLocation(),
                success?Sound.ENTITY_EXPERIENCE_ORB_PICKUP:Sound.ENTITY_VILLAGER_NO,
                SoundCategory.MASTER,success?0.50f:0.42f,success?1.38f:0.88f);
    }

    public static void success(CommandSender to,String value) {
        to.sendMessage(Colors.color(format("&#70D6E8✔ &8» &7"+value)));
        notifySound(to,true);
    }
    public static net.kyori.adventure.text.Component unknownComponent() {
        return Colors.color(format("&#FF727F✘ &8» &7Nieznana komenda."));
    }
    public static void unknown(CommandSender to) {
        to.sendMessage(unknownComponent());
        notifySound(to,false);
    }
    public static void error(CommandSender to,String value) {
        to.sendMessage(Colors.color(format("&#FF727F✘ &8» &7"+value)));
        notifySound(to,false);
    }
    /** Stan niezmieniony: jeden neutralny komunikat, bez dźwięku sukcesu i bez ponownego zapisu. */
    public static void unchanged(CommandSender to,String value) {
        long now=System.currentTimeMillis();
        var last=lastUnchanged.get(to);
        if(last!=null && last.getKey().equals(value) && now-last.getValue()<1200L)return;
        lastUnchanged.put(to, new java.util.AbstractMap.SimpleImmutableEntry<>(value,now));
        to.sendMessage(Colors.color(format("&#FFD166» &7"+value)));
    }
    public static void info(CommandSender to,String value) {
        to.sendMessage(Colors.color(format("&#70D6E8» &7"+value)));
    }
    public static void line(CommandSender to,String value) {
        to.sendMessage(Colors.color(format(value)));
    }
    public static void title(CommandSender to,String value) {
        to.sendMessage(Colors.color(format("&#FFD166&l» "+value+" &8«")));
    }
    public static void usage(CommandSender to,String syntax) {
        to.sendMessage(Colors.color(format("&#FF727F✘ &8» &7Składnia: &#FFD166"+syntax)));
        notifySound(to,false);
    }
    public static void hint(CommandSender to,String value) {
        to.sendMessage(Colors.color(format("&#70D6E8➜ &7"+value)));
    }
}
