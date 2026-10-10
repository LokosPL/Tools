package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.whitelist.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class WhitelistCommand implements BasicCommand {
    private final JavaPlugin plugin;
    private final WhitelistService service;
    private final WhitelistMenu menu;

    public WhitelistCommand(JavaPlugin plugin, WhitelistService service, WhitelistMenu menu) {
        this.plugin=plugin;this.service=service;this.menu=menu;
    }
    @Override public String permission() { return "tools.whitelist.admin"; }
    @Override public void execute(CommandSourceStack source, String[] args) { handle(source.getSender(),args); }

    public void handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission(permission())) {
            Messages.error(sender,"Nie masz uprawnień do zarządzania whitelistą.");
            return;
        }
        if (args.length==0 || args[0].equalsIgnoreCase("gui")) {
            if (sender instanceof Player player) menu.open(player,0);
            else list(sender);
            return;
        }
        String action=args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "włącz","wlacz" -> {
                if(args.length != 2){usage(sender);return;}
                try {
                    WhitelistMode mode=WhitelistMode.parse(args[1]);
                    CompletableFuture<Void> activation=sender instanceof Player player
                            && !service.state().players().contains(player.getName().toLowerCase(Locale.ROOT))
                            ? service.add(player.getName()).thenCompose(ignored -> service.enable(mode))
                            : service.enable(mode);
                    update(sender,activation,"Włączono whitelistę: &f"+mode.title()+".");
                } catch (IllegalArgumentException error) {Messages.error(sender,error.getMessage());}
            }
            case "wyłącz","wylacz","off" -> {
                if(args.length!=1){usage(sender);return;}
                update(sender,service.disable(),"Wyłączono whitelistę.");
            }
            case "dodaj" -> {
                if(args.length!=2){usage(sender);return;}
                try { update(sender,service.add(args[1]),"Dodano gracza &f"+args[1]+"&a do whitelisty."); }
                catch (IllegalArgumentException error){Messages.error(sender,error.getMessage());}
            }
            case "usuń","usun" -> {
                if(args.length!=2){usage(sender);return;}
                try { update(sender,service.remove(args[1]),"Usunięto gracza &f"+args[1]+"&a z whitelisty."); }
                catch (IllegalArgumentException error){Messages.error(sender,error.getMessage());}
            }
            case "lista" -> {
                if (args.length!=1){usage(sender);return;}
                if(sender instanceof Player p)menu.open(p,0);
                else list(sender);
            }
            default -> usage(sender);
        }
    }
    private void list(CommandSender sender){
        Messages.title(sender,"WHITELIST");
        Messages.info(sender, "Stan: "+(service.state().enabled()?"&aWłączona":"&7Wyłączona"));
        Messages.info(sender, "Tryb: &f"+service.state().mode());
        Messages.info(sender,"Dodani: &f"+service.state().players().size());
        service.state().players().stream().sorted().forEach(n -> Messages.line(sender,"&8• &7"+n));
    }
    private void usage(CommandSender s){
        Messages.title(s,"ZARZĄDZANIE WHITELISTĄ");
        Messages.line(s,"&a/whitelist &8— &7Panel graficzny");
        Messages.line(s,"&a/whitelist włącz &7<prace_techniczne|chwilowa_przerwa|nowa_edycja|aktualizacja>");
        Messages.line(s,"&a/whitelist wyłącz");
        Messages.line(s,"&a/whitelist dodaj &7<nick>");
        Messages.line(s,"&a/whitelist usuń &7<nick>");
        Messages.line(s,"&a/whitelist lista &8— &7Główki graczy");
    }
    public void update(CommandSender s, CompletableFuture<Void> operation, String message) {
        operation.whenComplete((unused,problem)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(problem==null)Messages.success(s,message);
                else {
                    Throwable root=problem;
                    while(root instanceof CompletionException && root.getCause()!=null)root=root.getCause();
                    Messages.error(s,root.getMessage()==null?"Nie zapisano whitelisty.":root.getMessage());
                }
            });
        });
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args) {
        if (args.length==1) return prefix(List.of("włącz","wyłącz","dodaj","usuń","lista","gui"),args[0]);
        if (args.length==2) {
            if (Set.of("włącz","wlacz").contains(args[0].toLowerCase(Locale.ROOT)))
                return prefix(WhitelistMode.names().stream().map(String::toLowerCase).toList(), args[1]);
            if(Set.of("usuń","usun").contains(args[0].toLowerCase(Locale.ROOT)))
                return prefix(service.state().players(),args[1]);
            if(args[0].equalsIgnoreCase("dodaj"))
                return prefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);
        }
        return List.of();
    }
    private static List<String> prefix(Collection<String> names,String input) {
        return names.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .sorted().limit(80).toList();
    }
}
