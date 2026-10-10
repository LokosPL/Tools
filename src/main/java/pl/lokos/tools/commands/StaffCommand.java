package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.security.ToolsAccess;
import pl.lokos.tools.staff.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Jedna autoryzacja BasicCommand/Paper oraz backend współdzielony przez aliasy. */
public final class StaffCommand implements BasicCommand {
    public enum Kind { TP, VANISH, HELPOP, GAMEMODE, FLY, BROADCAST, INVENTORYOPEN, SPEED }
    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final StaffManager service;
    private final InventoryAudit inventoryAudit;
    private final Kind kind;
    private final String node;

    public StaffCommand(JavaPlugin plugin,RankManager ranks,StaffManager service,
                        InventoryAudit inventoryAudit,Kind kind,String permission){
        this.plugin=plugin;this.ranks=ranks;this.service=service;this.inventoryAudit=inventoryAudit;
        this.kind=kind;this.node=permission;
    }
    @Override public String permission(){return node;}
    @Override public boolean canUse(CommandSender sender){
        if(kind==Kind.HELPOP)return sender instanceof Player;
        return ToolsAccess.permitted(sender,ranks,node);
    }
    private boolean sub(CommandSender sender,String permission){
        return ToolsAccess.permitted(sender,ranks,permission);
    }
    private static Player online(String name){
        return Bukkit.getPlayerExact(name);
    }
    private static Player mustOnline(String name){
        Player player=online(name);
        if(player==null)throw new IllegalArgumentException("Gracz "+name+" nie jest online.");
        return player;
    }
    private static Player self(CommandSender sender){
        if(!(sender instanceof Player player))
            throw new IllegalArgumentException("Z konsoli podaj nick gracza.");
        return player;
    }
    /** Wspólny formatter chatowych zgłoszeń: gracz nie może podszyć się pod kolory administracji. */
    public static net.kyori.adventure.text.Component safeHelpop(String template,String sender,String text){
        String format=template.replace("{player}",sender);
        String[] halves=format.split("\\{message\\}",-1);
        net.kyori.adventure.text.Component result=pl.lokos.tools.helpers.Colors.color(halves[0]);
        for(int i=1;i<halves.length;i++){
            result=result.append(net.kyori.adventure.text.Component.text(text));
            result=result.append(pl.lokos.tools.helpers.Colors.color(halves[i]));
        }
        return result;
    }
    private static boolean enable(String value) {
        return switch(value.toLowerCase(Locale.ROOT)){
            case "on","wlacz","włącz","true"->true;
            case "off","wylacz","wyłącz","false"->false;
            default -> throw new IllegalArgumentException("Użyj wlacz albo wylacz.");
        };
    }
    private static boolean isToggle(String text){
        return List.of("on","off","wlacz","włącz","wylacz","wyłącz").contains(text.toLowerCase(Locale.ROOT));
    }

    @Override public void execute(CommandSourceStack source,String[] args){
        CommandSender sender=source.getSender();
        if(!canUse(sender)){Messages.unknown(sender);return;}
        try{
            switch(kind){
                case TP -> tp(sender,args);
                case VANISH -> vanish(sender,args);
                case HELPOP -> helpop(sender,args);
                case GAMEMODE -> gamemode(sender,args);
                case FLY -> fly(sender,args);
                case BROADCAST -> broadcast(sender,args);
                case INVENTORYOPEN -> inventory(sender,args);
                case SPEED -> speed(sender,args);
            }
        }catch(IllegalArgumentException ex){Messages.error(sender,ex.getMessage());}
    }

    private void tp(CommandSender sender,String[] args){
        if(args.length==0){CommandTextRegistry.help(sender,"tp");return;}
        boolean group=args[0].equals("*");
        if(group&&!sub(sender,"tools.tp.all")){Messages.unknown(sender);return;}
        if(args.length==1){
            if(group){
                Player actor=self(sender);
                Location destination=actor.getLocation().clone();
                int count=0;
                for(Player target:Bukkit.getOnlinePlayers()) {
                    if(target.equals(actor))continue;
                    teleport(target,destination);
                    count++;
                }
                Messages.success(sender,"Rozpoczęto teleportację "+count+" graczy do Ciebie.");
                return;
            }
            Player destination=mustOnline(args[0]);
            if(sender instanceof Player actor){
                if(actor.equals(destination)){
                    Messages.unchanged(sender,"Jesteś już tą osobą.");return;
                }
                teleport(actor,destination.getLocation());
                Messages.info(sender,"Przenoszę do "+destination.getName()+".");
            }else throw new IllegalArgumentException("Z konsoli użyj /tp <nick|*> <x> <y> <z>.");
            return;
        }
        if(args.length!=3 && args.length!=4){
            CommandTextRegistry.help(sender,"tp");return;
        }
        Player actor;
        String[] coords;
        if(args.length==3){
            actor=self(sender);
            coords=args;
        }else{
            if(!group && !sub(sender,"tools.tp.others")){Messages.unknown(sender);return;}
            actor=group?null:mustOnline(args[0]);
            coords=Arrays.copyOfRange(args,1,4);
        }
        Collection<? extends Player> targets=group?Bukkit.getOnlinePlayers():List.of(actor);
        if(targets.isEmpty()){Messages.unchanged(sender,"Brak graczy online.");return;}
        Map<Player,Location> planned=new LinkedHashMap<>();
        for(Player target:targets) {
            Location current=target.getLocation();
            // Standardowo współrzędne i świat są względem wykonującego komendę,
            // a z konsoli (bez pozycji) względem wskazanego celu.
            Location origin=sender instanceof Player issuer?issuer.getLocation():current;
            // Wszystkie cele walidujemy PRZED pierwszą teleportacją.
            double x=StaffParsers.coordinate(coords[0],origin.getX(),service.settings().maxTeleportCoordinate());
            double y=StaffParsers.coordinate(coords[1],origin.getY(),service.settings().maxTeleportCoordinate());
            double z=StaffParsers.coordinate(coords[2],origin.getZ(),service.settings().maxTeleportCoordinate());
            if(y<origin.getWorld().getMinHeight()||y>=origin.getWorld().getMaxHeight())
                throw new IllegalArgumentException("Y poza zakresem wysokości świata "+origin.getWorld().getName()+".");
            Location destination=new Location(origin.getWorld(),x,y,z,current.getYaw(),current.getPitch());
            if(current.getWorld().equals(destination.getWorld())&&current.distanceSquared(destination)<0.01)continue;
            planned.put(target,destination);
        }
        if(planned.isEmpty()){
            Messages.unchanged(sender,"Wszyscy wskazani gracze są już na tych współrzędnych.");return;
        }
        planned.forEach(this::teleport);
        Messages.success(sender,"Rozpoczęto teleportację "+planned.size()+" graczy.");
    }
    private void teleport(Player target,Location destination){
        target.teleportAsync(destination).whenComplete((ok,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(!target.isOnline())return;
                if(error!=null||!Boolean.TRUE.equals(ok))
                    Messages.error(target,"Teleportacja nie powiodła się.");
                else Messages.success(target,"Teleportowano pomyślnie.");
            });
        });
    }

    private void vanish(CommandSender sender,String[] args){
        if(args.length>2){CommandTextRegistry.help(sender,"vanish");return;}
        Player target;
        Boolean selected=null;
        if(args.length==0)target=self(sender);
        else if(isToggle(args[0])){
            target=self(sender);selected=enable(args[0]);
        }else{
            if(!sub(sender,"tools.vanish.others")){Messages.unknown(sender);return;}
            target=mustOnline(args[0]);
            if(args.length==2)selected=enable(args[1]);
        }
        boolean next=selected!=null?selected:!service.vanished(target);
        if(next==service.vanished(target)){
            Messages.unchanged(sender,next?"Vanish jest już włączony.":"Vanish jest już wyłączony.");return;
        }
        boolean result=next;
        complete(sender,service.vanish(target,next,sender.getName()),
                result?"Włączono vanish.":"Wyłączono vanish.");
        if(sender!=target)Messages.info(target,result?"Vanish został włączony przez administrację.":
                "Vanish został wyłączony przez administrację.");
    }

    private void helpop(CommandSender sender,String[] args){
        Player player=self(sender);
        if(args.length==0){CommandTextRegistry.help(sender,"helpop");return;}
        String text=String.join(" ",args).trim();
        if(text.isBlank()||text.length()>service.settings().helpopMaxLength())
            throw new IllegalArgumentException("Zgłoszenie musi mieć 1-"+service.settings().helpopMaxLength()+" znaków.");
        List<? extends Player> receivers=Bukkit.getOnlinePlayers().stream()
                .filter(service::helpopStaff).toList();
        if(receivers.isEmpty()){
            Messages.error(sender,"Brak administracji online mogącej odebrać zgłoszenie.");return;
        }
        if(!service.helpopAllowed(player.getUniqueId(),System.currentTimeMillis())
                && !sub(player,"tools.helpop.bypass.cooldown")){
            Messages.unchanged(sender,"Poczekaj przed następnym zgłoszeniem do administracji.");return;
        }
        net.kyori.adventure.text.Component rendered=
                safeHelpop(service.settings().helpopFormat(),player.getName(),text);
        for(Player viewer:receivers)viewer.sendMessage(rendered);
        Messages.success(sender,"Wysłano zgłoszenie do administracji.");
    }

    private void gamemode(CommandSender sender,String[] args){
        if(args.length<1||args.length>2){CommandTextRegistry.help(sender,"gamemode");return;}
        Player target=args.length==1?self(sender):mustOnline(args[0]);
        if(args.length==2 && !target.equals(sender) && !sub(sender,"tools.gamemode.others")){
            Messages.unknown(sender);return;
        }
        GameMode mode=StaffParsers.gameMode(args[args.length-1]);
        if(target.getGameMode()==mode){Messages.unchanged(sender,"Ten gracz ma już tryb "+mode.name()+".");return;}
        target.setGameMode(mode);
        Messages.success(sender,"Ustawiono "+mode.name()+" dla "+target.getName()+".");
        if(sender!=target)Messages.info(target,"Twój tryb gry: "+mode.name()+".");
    }

    private void fly(CommandSender sender,String[] args){
        if(args.length>2){CommandTextRegistry.help(sender,"fly");return;}
        Player target;
        Boolean desired=null;
        if(args.length==0)target=self(sender);
        else if(isToggle(args[0])) {
            target=self(sender);desired=enable(args[0]);
        }else{
            target=mustOnline(args[0]);
            if(target!=sender && !sub(sender,"tools.fly.others")){Messages.unknown(sender);return;}
            if(args.length==2)desired=enable(args[1]);
        }
        boolean enabled=desired!=null?desired:!target.getAllowFlight();
        if(target.getAllowFlight()==enabled){
            Messages.unchanged(sender,enabled?"Latanie jest już włączone.":"Latanie jest już wyłączone.");return;
        }
        if(!enabled && (target.getGameMode()==GameMode.CREATIVE||target.getGameMode()==GameMode.SPECTATOR))
            throw new IllegalArgumentException("Nie można wyłączyć latania w tym trybie gry.");
        if(!enabled)target.setFlying(false);
        target.setAllowFlight(enabled);
        Messages.success(sender,(enabled?"Włączono":"Wyłączono")+" latanie dla "+target.getName()+".");
        if(target!=sender)Messages.info(target,(enabled?"Włączono":"Wyłączono")+" Ci latanie.");
        service.audit(sender instanceof Player p?p:target,
                (sender==target?"":("ustawił "+target.getName()+": "))
                        +(enabled?"włączył fly":"wyłączył fly"),"tools.fly.monitor");
    }

    private void broadcast(CommandSender sender,String[] args){
        if(args.length==1 && (args[0].equalsIgnoreCase("wylacz")||args[0].equalsIgnoreCase("wyłącz"))){
            complete(sender,service.clearBroadcast(),"Wyłączono ogłoszenie na bossbarze.");return;
        }
        if(args.length<2){CommandTextRegistry.help(sender,"broadcast");return;}
        int seconds=StaffParsers.duration(args[0],service.settings().broadcastMaxSeconds());
        String text=String.join(" ",Arrays.copyOfRange(args,1,args.length)).trim();
        complete(sender,service.broadcast(text,seconds),
                "Ogłoszenie na bossbarze aktywne przez "+seconds+" sekund.");
    }

    private void inventory(CommandSender sender,String[] args){
        if(args.length!=2){CommandTextRegistry.help(sender,"inventoryopen");return;}
        Player viewer=self(sender);
        Player target=mustOnline(args[1]);
        String type=args[0].toLowerCase(Locale.ROOT);
        Inventory opened;
        switch(type){
            case "eq","inventory","ekwipunek" -> opened=target.getInventory();
            case "enderchest","ender","ec" ->{
                if(!sub(sender,"tools.inventoryopen.enderchest")){Messages.unknown(sender);return;}
                opened=target.getEnderChest();
            }
            default -> {CommandTextRegistry.help(sender,"inventoryopen");return;}
        }
        viewer.openInventory(opened);
        inventoryAudit.track(viewer,opened,type.startsWith("end")||type.equals("ec"));
        Messages.info(sender,"Otworzono "+type+" gracza "+target.getName()+".");
    }

    private void speed(CommandSender sender,String[] args){
        if(args.length<1||args.length>3){CommandTextRegistry.help(sender,"speed");return;}
        Player target;
        String mode;
        String value;
        if(args.length==1){
            target=self(sender);mode=target.isFlying()?"fly":"walk";value=args[0];
        }else if(args[0].equalsIgnoreCase("walk")||args[0].equalsIgnoreCase("fly")){
            mode=args[0].toLowerCase(Locale.ROOT);
            value=args[1];
            target=args.length==3?mustOnline(args[2]):self(sender);
        }else if(args.length==2){
            target=mustOnline(args[0]);mode=target.isFlying()?"fly":"walk";value=args[1];
        }else{
            CommandTextRegistry.help(sender,"speed");return;
        }
        if(target!=sender&&!sub(sender,"tools.speed.others")){Messages.unknown(sender);return;}
        float speed=StaffParsers.speed(value);
        float current=mode.equals("fly")?target.getFlySpeed():target.getWalkSpeed();
        if(Math.abs(current-speed)<0.0001f){Messages.unchanged(sender,"Prędkość jest już tak ustawiona.");return;}
        if(mode.equals("fly"))target.setFlySpeed(speed);else target.setWalkSpeed(speed);
        Messages.success(sender,"Ustawiono prędkość "+mode+" na "+value+" dla "+target.getName()+".");
    }

    private void complete(CommandSender sender,CompletableFuture<Void> future,String text){
        future.whenComplete((v,error)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(error==null)Messages.success(sender,text);
                else{
                    Throwable cause=pl.lokos.tools.helpers.StateChanges.root(error);
                    if(cause instanceof IllegalArgumentException)
                        Messages.unchanged(sender,cause.getMessage());
                    else Messages.error(sender,"Nie udało się zapisać danych: "+cause.getMessage());
                }
            });
        });
    }

    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        if(!canUse(source.getSender()))return List.of();
        if(args.length!=1)return List.of();
        List<String> suggestions=new ArrayList<>();
        switch(kind){
            case TP -> {suggestions.add("*");suggestions.add("~");}
            case VANISH,FLY -> suggestions.addAll(List.of("wlacz","wylacz"));
            case BROADCAST -> suggestions.addAll(List.of("30s","5m","2h","1d","wylacz"));
            case GAMEMODE -> suggestions.addAll(List.of("1","2","3","4","survival","creative","adventure","spectator"));
            case INVENTORYOPEN -> suggestions.addAll(List.of("eq","enderchest"));
            case SPEED -> suggestions.addAll(List.of("1","2","5","10","walk","fly"));
            default -> {}
        }
        if(kind==Kind.TP||kind==Kind.VANISH||kind==Kind.FLY||kind==Kind.GAMEMODE||kind==Kind.SPEED)
            for(Player player:Bukkit.getOnlinePlayers())suggestions.add(player.getName());
        return suggestions.stream().distinct().filter(s->s.toLowerCase(Locale.ROOT)
                .startsWith(args[0].toLowerCase(Locale.ROOT))).limit(60).toList();
    }
}
