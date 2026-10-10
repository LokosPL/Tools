package pl.lokos.tools.security;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.whitelist.WhitelistService;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lekkie limity logowań bez CAPTCHA ani opóźnień normalnych graczy.
 * Nie wykonuje SQL, DNS ani HTTP. Przy botnecie z wielu adresów IP trzeba
 * dodatkowo użyć filtra na proxy/firewallu (przed procesem Paper).
 */
public final class AntiBotGuard implements Listener {
    private final JavaPlugin plugin;
    private final WhitelistService whitelist;
    private final ConcurrentHashMap<String,Counter> ips=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,Long> trusted=new ConcurrentHashMap<>();
    private final boolean enabled;
    private static final String KICK="&cZbyt wiele prób połączenia.\n&7Spróbuj ponownie za krótką chwilę.";
    private static final class Counter {
        long window;
        int attempts;
        synchronized boolean allow(long now) {
            if(now-window>=60_000){window=now;attempts=0;}
            return ++attempts<=30;
        }
    }
    public AntiBotGuard(JavaPlugin plugin,WhitelistService whitelist,boolean enabled) {
        this.plugin=plugin;this.whitelist=whitelist;this.enabled=enabled;
    }
    @EventHandler(priority=EventPriority.LOWEST)
    public void login(AsyncPlayerPreLoginEvent event) {
        if(!enabled || event.getLoginResult()!=AsyncPlayerPreLoginEvent.Result.ALLOWED)return;
        String name=event.getName().toLowerCase(Locale.ROOT);
        String address=event.getAddress().getHostAddress();
        if(whitelist.state().players().contains(name)
                || trusted.containsKey(name+"@"+address))return;
        long now=System.currentTimeMillis();
        Counter bucket=ips.computeIfAbsent(address,ignored->new Counter());
        if(!bucket.allow(now))
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,Colors.color(KICK));
        // Ograniczona pamięć: czyszczenie leniwie przy wzroście liczby adresów.
        if(ips.size()>10000) {
            ips.entrySet().removeIf(e->now-e.getValue().window>120000);
            if(ips.size()>10000)ips.clear();
        }
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        if(!enabled)return;
        String name=event.getPlayer().getName().toLowerCase(Locale.ROOT);
        if(trusted.size()>20000)trusted.clear();
        String address=event.getPlayer().getAddress()==null ? "" :
                event.getPlayer().getAddress().getAddress().getHostAddress();
        trusted.put(name+"@"+address,System.currentTimeMillis());
    }
}
