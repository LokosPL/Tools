package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.function.Supplier;
import pl.lokos.tools.whitelist.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class WhitelistCommand implements BasicCommand {
    private static final pl.lokos.tools.helpers.CommandMessages display = new pl.lokos.tools.helpers.CommandMessages("whitelist");
    private final JavaPlugin plugin;
    private final WhitelistService service;
    private final WhitelistMenu menu;
    private final Supplier<RankManager> ranks;

    public WhitelistCommand(JavaPlugin plugin, WhitelistService service, WhitelistMenu menu,
                            Supplier<RankManager> ranks) {
        this.plugin=plugin;this.service=service;this.menu=menu;
        this.ranks=ranks;
    }
    @Override public String permission() { return "tools.whitelist.admin"; }
    @Override public void execute(CommandSourceStack source, String[] args) { handle(source.getSender(),args); }

    public void handle(CommandSender sender, String[] args) {
        if (!ToolsAccess.admin(sender,ranks.get(),permission())) {
            display.unknown(sender);
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
                } catch (IllegalArgumentException error) {display.error(sender,error.getMessage());}
            }
            case "wyłącz","wylacz","off" -> {
                if(args.length!=1){usage(sender);return;}
                update(sender,service.disable(),"Wyłączono whitelistę.");
            }
            case "dodaj" -> {
                if(args.length!=2){usage(sender);return;}
                try { update(sender,service.add(args[1]),"Dodano gracza &f"+args[1]+"&a do whitelisty."); }
                catch (IllegalArgumentException error){display.error(sender,error.getMessage());}
            }
            case "usuń","usun" -> {
                if(args.length!=2){usage(sender);return;}
                try { update(sender,service.remove(args[1]),"Usunięto gracza &f"+args[1]+"&a z whitelisty."); }
                catch (IllegalArgumentException error){display.error(sender,error.getMessage());}
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
        display.title(sender,"WHITELIST");
        display.info(sender, "Stan: "+(service.state().enabled()?"&aWłączona":"&7Wyłączona"));
        display.info(sender, "Tryb: &f"+service.state().mode());
        display.info(sender,"Dodani: &f"+service.state().players().size());
        service.state().players().stream().sorted().forEach(n -> display.line(sender,"&8• &7"+n));
    }
    private void usage(CommandSender sender) {
        CommandTextRegistry.help(sender,"whitelist");
    }
    public void update(CommandSender s, CompletableFuture<Void> operation, String message) {
        operation.whenComplete((unused,problem)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(problem==null)display.success(s,message);
                else {
                    Throwable root=problem;
                    while(root instanceof CompletionException && root.getCause()!=null)root=root.getCause();
                    display.error(s,root.getMessage()==null?"Nie zapisano whitelisty.":root.getMessage());
                }
            });
        });
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args) {
        if (!ToolsAccess.admin(source.getSender(),ranks.get(),permission())) return List.of();
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
