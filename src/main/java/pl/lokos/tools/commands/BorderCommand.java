package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.border.BorderManager;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.*;

/** Administracyjne wymuszenia, zwykli gracze widzą jedynie bossbar rozrostu. */
public final class BorderCommand implements BasicCommand {
    private final JavaPlugin plugin;
    private final BorderManager border;
    private final RankManager ranks;
    private final String permission;
    public BorderCommand(JavaPlugin plugin,BorderManager border,RankManager ranks,String permission){
        this.plugin=plugin;this.border=border;this.ranks=ranks;this.permission=permission;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){return ToolsAccess.permitted(sender,ranks,permission);}
    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        try{
            if(args.length==0||args.length==1&&args[0].equalsIgnoreCase("status")){
                Messages.info(sender,border.status());return;
            }
            String action=args[0].toLowerCase(Locale.ROOT);
            if(args.length==1&&(action.equals("pauza")||action.equals("wznow"))){
                border.pause(action.equals("pauza")).whenComplete((v,error)->{
                    if(!plugin.isEnabled())return;
                    Bukkit.getScheduler().runTask(plugin,()->{
                        if(error==null)Messages.success(sender,"Zapisano stan ekspansji.");
                        else Messages.error(sender,"Nie zapisano stanu ekspansji.");
                    });
                });return;
            }
            if(args.length==2&&action.equals("rozbuduj")){
                int size=Integer.parseInt(args[1]);
                border.expand(size);
                Messages.success(sender,"Zlecono powiększenie granicy o "+size+" bloków.");
                return;
            }
            Messages.info(sender,"/granica status | /granica rozbuduj <bloki> | /granica pauza | /granica wznow");
        }catch(NumberFormatException error){Messages.error(sender,"Niepoprawna liczba bloków.");}
        catch(IllegalArgumentException error){Messages.error(sender,error.getMessage());}
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        if(!canUse(source.getSender())||args.length==0)return List.of();
        List<String> options=new ArrayList<>();
        if(args.length==1)options.addAll(List.of("status","rozbuduj","pauza","wznow"));
        if(args.length==2&&args[0].equalsIgnoreCase("rozbuduj"))
            options.addAll(List.of("100","250","500","1000"));
        String typed=args[args.length-1].toLowerCase(Locale.ROOT);
        return options.stream().filter(s->s.startsWith(typed)).toList();
    }
}
