package pl.lokos.tools.security;

import java.util.Locale;
import java.util.Set;

/** Jedna polityka dla TAB i wykonania: ukryte polecenia wbudowane Bukkit/Paper/Minecraft. */
public final class CommandVisibilityPolicy {
    private static final Set<String> PRIVATE_ROOTS = Set.of(
            // Bukkit/Paper publikuje te aliasy bez przestrzeni nazw, często także bez permisji.
            // /? jest aliasem /help, a /icanhasbukkit to wbudowana komenda diagnostyczna.
            "help","?","icanhasbukkit",
            "plugins","pl","about","version","ver","reload","rl",
            "whitelist","bialalista","wl",
            "op","deop","stop","save-all","save-on","save-off",
            "ban","ban-ip","pardon","pardon-ip","banlist",
            "debug","perf","datapack","function",
            "gamemode","defaultgamemode","give","clear","enchant","effect",
            "experience","xp","teleport","tp","summon","setblock","fill",
            "fillbiome","clone","kill","item","loot","attribute","data",
            "execute","advancement","bossbar","gamerule","difficulty",
            "weather","time","worldborder","setworldspawn","spawnpoint",
            "kick","tag","team","scoreboard","schedule","title","tellraw",
            "spectate","seed","locate","forceload","jfr","tick","place","ride"
    );
    private CommandVisibilityPolicy() {}

    public static boolean technical(String command) {
        String name = command.toLowerCase(Locale.ROOT);
        return name.startsWith("bukkit:") || name.startsWith("minecraft:")
                || name.startsWith("paper:");
    }

    public static boolean nativeAdministrative(String command) {
        String name = command.toLowerCase(Locale.ROOT);
        if (name.startsWith("bukkit:") || name.startsWith("paper:")) return true;
        if (name.startsWith("minecraft:"))
            name = name.substring("minecraft:".length());
        return PRIVATE_ROOTS.contains(name);
    }

    public static boolean hideFromUnprivileged(String command) {
        return technical(command) || nativeAdministrative(command);
    }
}
