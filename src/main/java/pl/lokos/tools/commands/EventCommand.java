package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.events.*;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.helpers.StateChanges;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import pl.lokos.tools.staff.StaffParsers;

import java.util.*;
import java.util.concurrent.*;

/** /event (admin) i pojedyncze publiczne komendy /zima itp. */
public final class EventCommand implements BasicCommand {
    private final JavaPlugin plugin;
    private final EventManager service;
    private final RankManager ranks;
    private final EventType information;
    private final String permission;
    public EventCommand(JavaPlugin plugin,EventManager service,RankManager ranks,
                        EventType info,String permission){
        this.plugin=plugin;this.service=service;this.ranks=ranks;this.information=info;
        this.permission=permission;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){
        return information!=null ? ToolsAccess.permitted(sender,ranks,"tools.event.info")
                : ToolsAccess.permitted(sender,ranks,permission)
                && ToolsAccess.admin(sender,ranks,"tools.event.admin");
    }
    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        try{
            if(information!=null){
                if(information==EventType.METEORY && args.length==1
                        && args[0].equalsIgnoreCase("gdzie")){
                    service.meteors().sendHints(sender);return;
                }
                if(args.length>0){Messages.error(sender,
                        "Użycie: /"+information.id()
                        +(information==EventType.METEORY?" [gdzie]":""));return;}
                if(!(sender instanceof Player player)){
                    Messages.info(sender,information.title()+": "+information.description());return;
                }
                service.open(player,information);return;
            }
            if(args.length==0){help(sender);return;}
            if(args.length==1&&args[0].equalsIgnoreCase("gui")){
                if(sender instanceof Player player)service.openHub(player);
                else Messages.error(sender,"GUI dostępne tylko w grze.");
                return;
            }
            String action=args[0].toLowerCase(Locale.ROOT);
            if(args.length==1&&action.equals("status")){
                EventType current=service.active();
                Messages.info(sender,current==null?"Obecnie nie trwa żaden event.":
                        "Aktywny event: "+current.title()+" | /"+current.id());return;
            }
            if(args.length==1&&action.equals("lista")){
                Messages.title(sender,"DOSTĘPNE EVENTY");
                for(EventType type:EventType.values())
                    Messages.info(sender,"&#70D6E8"+type.id()+" &#A8A8B7» "+type.description());
                return;
            }
            if(args.length==3&&action.equals("wlacz")){
                EventType type=EventType.parse(args[1]);
                if(type==null)throw new IllegalArgumentException("Nieznany event. Wpisz /event lista.");
                int seconds=StaffParsers.duration(args[2],service.config().maxDurationMinutes()*60);
                complete(sender,service.start(type,seconds),"Uruchomiono event: "+type.title()+".");
                return;
            }
            if(args.length==1&&(action.equals("zakoncz")||action.equals("zakończ"))){
                complete(sender,service.stop(),"Zakończono event.");return;
            }
            help(sender);
        }catch(IllegalArgumentException error){Messages.error(sender,error.getMessage());}
    }
    private void help(CommandSender sender){
        Messages.title(sender,"WYDARZENIA");
        Messages.info(sender,"&#70D6E8/event gui &#A8A8B7» Panel wydarzeń");
        Messages.info(sender,"&#70D6E8/event lista &#A8A8B7» Lista wydarzeń");
        Messages.info(sender,"&#70D6E8/event status &#A8A8B7» Aktywny event");
        Messages.info(sender,"&#70D6E8/event wlacz <tryb> <30m|2h|1d>");
        Messages.info(sender,"&#70D6E8/event zakoncz &#A8A8B7» Zatrzymaj event");
    }
    private void complete(CommandSender sender,CompletableFuture<Void> future,String msg){
        future.whenComplete((ignored,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(error==null)Messages.success(sender,msg);
                else if(!StateChanges.reportUnchanged(sender,error))
                    Messages.error(sender,"Nie zapisano eventu: "+StateChanges.root(error).getMessage());
            });
        });
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        if(!canUse(source.getSender())||args.length==0)return List.of();
        if(information!=null){
            if(information==EventType.METEORY && args.length==1
                    && "gdzie".startsWith(args[0].toLowerCase(Locale.ROOT)))
                return List.of("gdzie");
            return List.of();
        }
        List<String> result=new ArrayList<>();
        if(args.length==1)result.addAll(List.of("gui","status","lista","wlacz","zakoncz"));
        else if(args.length==2&&args[0].equalsIgnoreCase("wlacz"))result.addAll(EventType.names());
        else if(args.length==3&&args[0].equalsIgnoreCase("wlacz"))
            result.addAll(List.of("30m","1h","2h","6h","12h","1d","3d","7d"));
        String typed=args[args.length-1].toLowerCase(Locale.ROOT);
        return result.stream().filter(s->s.startsWith(typed)).toList();
    }
}
