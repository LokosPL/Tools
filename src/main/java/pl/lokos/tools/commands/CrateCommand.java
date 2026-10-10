package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.lokos.tools.crates.*;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import java.util.*;

/** Komenda admina; gracze bez permisji tylko klikają postawione skrzynie. */
public final class CrateCommand implements BasicCommand {
    private final CrateManager manager;
    private final RankManager ranks;
    private final String permission;
    public CrateCommand(CrateManager manager,RankManager ranks,String permission){
        this.manager=manager;this.ranks=ranks;this.permission=permission;
    }
    @Override public String permission(){return permission;}
    @Override public boolean canUse(CommandSender sender){
        // Uprawnienie komendy może zostać dostosowane w JSON.
        // Operacje tworzące klucze i usuwające skrzynie zawsze wymagają
        // osobnego administracyjnego node'a Tools, również przy TAB i aliasach.
        return ToolsAccess.permitted(sender,ranks,permission)
                && ToolsAccess.admin(sender,ranks,"tools.skrzynia.admin");
    }
    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        try{
            if(args.length==0 || args.length==1&&args[0].equalsIgnoreCase("gui")){
                if(!(sender instanceof Player player)){
                    Messages.error(sender,"GUI dostępne tylko w grze.");return;
                }
                manager.adminMenu(player);return;
            }
            if(args.length==1&&args[0].equalsIgnoreCase("lista")){
                Messages.info(sender,"Postawione skrzynie: "+manager.state().crates().size());
                return;
            }
            if(args.length==1&&args[0].equalsIgnoreCase("usun")){
                if(!(sender instanceof Player player)){
                    Messages.error(sender,"Wskazanie skrzyni wymaga gracza.");return;
                }
                Block target=player.getTargetBlockExact(6);
                if(target==null)throw new IllegalArgumentException("Spójrz na skrzynię w odległości 6 bloków.");
                // Wynik operacji zostanie potwierdzony dopiero po zapisie na dysku.
                manager.remove(player,target);
                return;
            }
            if(args.length>=3&&args.length<=4&&args[0].equalsIgnoreCase("klucz")){
                CrateType kind=CrateType.parse(args[1]);
                if(kind==null)throw new IllegalArgumentException("Nieznany typ skrzyni.");
                Player target=Bukkit.getPlayerExact(args[2]);
                if(target==null)throw new IllegalArgumentException("Gracz musi być online.");
                int amount=args.length==4?Integer.parseInt(args[3]):1;
                manager.giveKey(target,kind,amount);
                Messages.success(sender,"Przekazano "+amount+" kluczy "+kind.title()+" dla "+target.getName()+".");
                return;
            }
            Messages.info(sender,"/skrzynia [gui|lista|usun]");
            Messages.info(sender,"/skrzynia klucz <typ> <nick> [ilość]");
        }catch(NumberFormatException ex){Messages.error(sender,"Podaj poprawną liczbę 1-64.");}
        catch(IllegalArgumentException ex){Messages.error(sender,ex.getMessage());}
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        if(!canUse(source.getSender())||args.length==0)return List.of();
        List<String> values=new ArrayList<>();
        if(args.length==1)values.addAll(List.of("gui","lista","usun","klucz"));
        else if(args.length==2&&args[0].equalsIgnoreCase("klucz"))
            values.addAll(CrateType.names());
        else if(args.length==3&&args[0].equalsIgnoreCase("klucz")){
            for(Player p:Bukkit.getOnlinePlayers())
                if(!(source.getSender() instanceof Player viewer)||viewer.canSee(p))
                    values.add(p.getName());
        }else if(args.length==4&&args[0].equalsIgnoreCase("klucz"))
            values.addAll(List.of("1","2","4","8","16","32","64"));
        String typed=args[args.length-1].toLowerCase(Locale.ROOT);
        return values.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }
}
