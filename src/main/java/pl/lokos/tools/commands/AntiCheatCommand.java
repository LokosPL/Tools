package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.anticheat.AntiCheatManager;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.helpers.StateChanges;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** /ac powiadomienia dotyczy wykonującego; /ac wlacz|wylacz jest globalne. */
public final class AntiCheatCommand implements BasicCommand {
    private final JavaPlugin plugin;
    private final AntiCheatManager service;
    private final RankManager ranks;
    private final String permission;
    public AntiCheatCommand(JavaPlugin plugin,AntiCheatManager service,RankManager ranks,String permission){
        this.plugin=plugin;this.service=service;this.ranks=ranks;this.permission=permission;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){
        return ToolsAccess.permitted(sender,ranks,permission)
                || ToolsAccess.permitted(sender,ranks,"tools.antycheat.alerts");
    }
    private boolean admin(CommandSender sender){return ToolsAccess.permitted(sender,ranks,permission);}
    private boolean alerts(CommandSender sender){return ToolsAccess.permitted(sender,ranks,"tools.antycheat.alerts");}
    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        if(args.length==0 || args.length==1 && args[0].equalsIgnoreCase("pomoc")){
            CommandTextRegistry.help(sender,"antycheat");return;
        }
        String command=args[0].toLowerCase(Locale.ROOT);
        if(args.length==1&&command.equals("status")){
            Messages.info(sender,"Antycheat: "+(service.state().enabled()?"&#70D6E8włączony":"&#FF727Fwyłączony"));
            if(sender instanceof Player player)Messages.info(sender,
                    "Powiadomienia: "+(service.state().receives(player.getUniqueId())?"włączone":"wyłączone"));
            Messages.info(sender,"Ochrona przeciążeniowa jest zawsze aktywna.");
            return;
        }
        if(args.length==1&&(command.equals("wlacz")||command.equals("wylacz"))){
            if(!admin(sender)){Messages.unknown(sender);return;}
            boolean enabled=command.equals("wlacz");
            finish(sender,service.enabled(enabled),enabled?"Włączono antycheat.":"Wyłączono detekcję antycheata.");
            return;
        }
        if(args.length==2&&command.equals("powiadomienia")){
            if(!alerts(sender)||!(sender instanceof Player player)){Messages.unknown(sender);return;}
            boolean enabled;
            if(args[1].equalsIgnoreCase("wlacz"))enabled=true;
            else if(args[1].equalsIgnoreCase("wylacz"))enabled=false;
            else {CommandTextRegistry.help(sender,"antycheat");return;}
            finish(sender,service.alerts(player.getUniqueId(),enabled),
                    enabled?"Włączono powiadomienia antycheata.":"Wyłączono powiadomienia antycheata.");
            return;
        }
        CommandTextRegistry.help(sender,"antycheat");
    }
    private void finish(CommandSender sender,CompletableFuture<Void> action,String ok){
        action.whenComplete((ignored,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(error==null)Messages.success(sender,ok);
                else if(!StateChanges.reportUnchanged(sender,error))
                    Messages.error(sender,"Nie udało się zapisać ustawień antycheata.");
            });
        });
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)||args.length==0)return List.of();
        List<String> values=new ArrayList<>(List.of("pomoc","status"));
        if(args.length==1){
            if(admin(sender))values.addAll(List.of("wlacz","wylacz"));
            if(alerts(sender)&&sender instanceof Player)values.add("powiadomienia");
        }else if(args.length==2&&args[0].equalsIgnoreCase("powiadomienia")&&alerts(sender))
            values=new ArrayList<>(List.of("wlacz","wylacz"));
        else return List.of();
        String typed=args[args.length-1].toLowerCase(Locale.ROOT);
        return values.stream().filter(s->s.startsWith(typed)).toList();
    }
}
