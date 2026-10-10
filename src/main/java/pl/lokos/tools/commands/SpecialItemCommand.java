package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.items.SpecialItemService;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.*;

/** Komenda administracyjna bez plugin.yml, bez SQL i bez ujawniania nicków ukrytych graczy. */
public final class SpecialItemCommand implements BasicCommand {
    private final SpecialItemService items;
    private final RankManager ranks;
    private final String permission;
    public SpecialItemCommand(SpecialItemService items,RankManager ranks,String permission){
        this.items=items;this.ranks=ranks;this.permission=permission;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){
        return ToolsAccess.permitted(sender,ranks,permission);
    }
    @Override public void execute(CommandSourceStack source,String[] args) {
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        if(args.length==0){CommandTextRegistry.help(sender,"przedmiot");return;}
        try{
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "lista" -> {
                    if(args.length!=1){CommandTextRegistry.help(sender,"przedmiot");return;}
                    Messages.title(sender,"PRZEDMIOTY SPECJALNE");
                    items.config().items().forEach((id,item)->{
                        if(item.enabled())Messages.info(sender,"&#FFD166"+id+" &#A8A8B7» "+item.name());
                    });
                }
                case "info" -> {
                    if(args.length!=2){CommandTextRegistry.help(sender,"przedmiot");return;}
                    var item=items.config().get(args[1]);
                    if(item==null){Messages.error(sender,"Nie ma przedmiotu o tym identyfikatorze.");return;}
                    Messages.info(sender,"&#FFD166"+args[1]+" &#A8A8B7» "+item.name());
                    Messages.info(sender,"Źródło: &#70D6E8"+item.eventName());
                }
                case "daj" -> {
                    if(args.length<3||args.length>4){CommandTextRegistry.help(sender,"przedmiot");return;}
                    Player target=Bukkit.getPlayerExact(args[1]);
                    if(target==null){Messages.error(sender,"Gracz musi być online.");return;}
                    String id=args[2].toLowerCase(Locale.ROOT);
                    int count=args.length==4?Integer.parseInt(args[3]):1;
                    if(count<1||count>16)throw new IllegalArgumentException("Liczba przedmiotów: 1-16.");
                    ItemStack template=items.create(id);
                    int free=0;
                    for(ItemStack slot:target.getInventory().getStorageContents())if(slot==null||slot.getType().isAir())free++;
                    if(free<count){
                        Messages.error(sender,"Gracz potrzebuje "+count+" wolnych miejsc w ekwipunku.");
                        return;
                    }
                    for(int i=0;i<count;i++)target.getInventory().addItem(template.clone());
                    Messages.success(sender,"Przekazano "+count+" przedmiotów: "+id+" graczowi "+target.getName()+".");
                    if(sender!=target)Messages.info(target,"Otrzymano przedmiot eventowy: "+id+".");
                }
                default -> CommandTextRegistry.help(sender,"przedmiot");
            }
        }catch(NumberFormatException e){Messages.error(sender,"Podaj poprawną liczbę przedmiotów.");}
        catch(IllegalArgumentException e){Messages.error(sender,e.getMessage());}
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args) {
        if(!canUse(source.getSender())||args.length==0)return List.of();
        List<String> options=new ArrayList<>();
        if(args.length==1)options.addAll(List.of("daj","lista","info"));
        else if(args.length==2){
            if(args[0].equalsIgnoreCase("daj")){
                for(Player p:Bukkit.getOnlinePlayers()){
                    if(!(source.getSender() instanceof Player viewer)||viewer.canSee(p))
                        options.add(p.getName());
                }
            }else if(args[0].equalsIgnoreCase("info"))options.addAll(items.config().items().keySet());
        }else if(args.length==3&&args[0].equalsIgnoreCase("daj"))
            options.addAll(items.config().items().keySet());
        else if(args.length==4&&args[0].equalsIgnoreCase("daj"))
            options.addAll(List.of("1","2","4","8","16"));
        String typed=args[args.length-1].toLowerCase(Locale.ROOT);
        return options.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(typed))
                .sorted().limit(60).toList();
    }
}
