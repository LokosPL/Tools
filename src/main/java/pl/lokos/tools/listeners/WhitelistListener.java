package pl.lokos.tools.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.commands.WhitelistCommand;
import pl.lokos.tools.config.HotReloadService;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.whitelist.*;
import pl.lokos.tools.security.ToolsAccess;
import pl.lokos.tools.basic.ToolsPlugin;
import pl.lokos.tools.helpers.StateChanges;

import java.util.*;
import java.util.concurrent.CompletionException;

/** Wstępna blokada logowania, menu GUI, własne przechwycenie vanilla /whitelist i /reload. */
public final class WhitelistListener implements Listener {
    private static final pl.lokos.tools.helpers.CommandMessages display = new pl.lokos.tools.helpers.CommandMessages("whitelist");
    private final JavaPlugin plugin;
    private final WhitelistService whitelist;
    private final WhitelistMenu menu;
    private final WhitelistCommand command;
    private final HotReloadService reload;
    public WhitelistListener(JavaPlugin plugin,WhitelistService whitelist,WhitelistMenu menu,
                             WhitelistCommand command,HotReloadService reload) {
        this.plugin=plugin;this.whitelist=whitelist;this.menu=menu;
        this.command=command;this.reload=reload;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void prelogin(AsyncPlayerPreLoginEvent event) {
        if(event.getLoginResult()==AsyncPlayerPreLoginEvent.Result.ALLOWED &&
                !whitelist.allowed(event.getName()))
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,Colors.color(whitelist.refusal()));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void join(PlayerJoinEvent event){
        Player player=event.getPlayer();
        if(!whitelist.allowed(player.getName()))
            later(()->{if(player.isOnline())player.kick(Colors.color(whitelist.refusal()));});
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof WhitelistMenu.Holder holder))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)
                || !holder.viewer().equals(player.getUniqueId())
                || event.getClickedInventory()!=event.getView().getTopInventory()
                )return;
        if(!ToolsAccess.admin(player,((ToolsPlugin)plugin).ranks(),"tools.whitelist.admin")) {
            display.unknown(player);
            later(player::closeInventory);
            return;
        }
        String action=holder.action(event.getRawSlot());
        if(action==null)return;
        if(action.startsWith("page:")){
            later(()->menu.open(player,Integer.parseInt(action.substring(5))));
            return;
        }
        java.util.concurrent.CompletableFuture<Void> operation;
        if(action.startsWith("mode:"))
            operation=whitelist.enable(WhitelistMode.parse(action.substring(5)),player.getName());
        else if(action.equals("disable"))
            operation=whitelist.disable();
        else if(action.startsWith("remove:"))
            operation=whitelist.remove(action.substring(7));
        else return;
        operation.whenComplete((ignored,error)->later(()->{
            if(!player.isOnline())return;
            if(error!=null) {
                if(StateChanges.reportUnchanged(player,error))return;
                display.error(player,"Nie zapisano zmian whitelisty.");
            } else {
                display.success(player,"Zapisano zmianę whitelisty.");
                if(player.getOpenInventory().getTopInventory().getHolder() instanceof WhitelistMenu.Holder)
                    menu.open(player,holder.page());
            }
        }));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof WhitelistMenu.Holder)
            event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void playerCommand(PlayerCommandPreprocessEvent event){
        if(event.isCancelled()) return;
        String raw=event.getMessage();
        if(!raw.startsWith("/"))return;
        handleVanilla(event.getPlayer(),raw.substring(1),()->event.setCancelled(true));
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void consoleCommand(ServerCommandEvent event){
        handleVanilla(event.getSender(),event.getCommand(),()->event.setCancelled(true));
    }

    private void handleVanilla(org.bukkit.command.CommandSender sender,String line,Runnable cancel){
        String[] parts=line.split("\\s+");
        if(parts.length==0)return;
        String root=parts[0].toLowerCase(Locale.ROOT);
        if(root.equals("whitelist") || root.equals("minecraft:whitelist")){
            cancel.run();
            if(!ToolsAccess.admin(sender,((ToolsPlugin)plugin).ranks(),"tools.whitelist.admin")) {
                pl.lokos.tools.helpers.Messages.unknown(sender);
                return;
            }
            command.handle(sender,Arrays.copyOfRange(parts,1,parts.length));
            return;
        }
        if(root.equals("reload") || root.equals("minecraft:reload") || root.equals("bukkit:reload")){
            cancel.run();
            if(!ToolsAccess.admin(sender,((ToolsPlugin)plugin).ranks(),"tools.admin")){
                pl.lokos.tools.helpers.Messages.unknown(sender);return;
            }
            display.info(sender,"Sprawdzanie konfiguracji Tools w tle...");
            for(Player online:Bukkit.getOnlinePlayers())
                online.sendMessage(Colors.color(whitelist.state().reloadMessage()));
            reload.reload().thenCompose(message->whitelist.reload().thenApply(ignored->message))
                    .whenComplete((message,error)->later(()->{
                        if(error==null){
                            display.success(sender,"Przeładowano ustawienia Tools. "+message);
                            for(Player online:Bukkit.getOnlinePlayers())
                                online.sendMessage(Colors.color("&#86E6BC✔ &7Konfiguracja serwera została odświeżona."));
                        }
                        else {
                            Throwable rootError=error;
                            while(rootError instanceof CompletionException && rootError.getCause()!=null)
                                rootError=rootError.getCause();
                            display.error(sender,"Nie przeładowano ustawień: "+rootError.getMessage());
                        }
                    }));
        }
    }
    private void later(Runnable callback){
        if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,callback);
    }
}
