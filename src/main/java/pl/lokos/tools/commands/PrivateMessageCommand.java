package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.chat.ChatManager;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.msg.*;
import pl.lokos.tools.security.ToolsAccess;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Jedna ścieżka doręczenia dla /msg, /r, /reply i /replay, włącznie z rewalidacją uprawnień. */
public final class PrivateMessageCommand implements BasicCommand {
    private final JavaPlugin plugin;
    private final PrivateMessageManager messages;
    private final ChatManager chat;
    private final RankManager ranks;
    private final Set<String> administrativeNodes;
    private final boolean replyMode;
    private final String chatAdminPermission;

    public PrivateMessageCommand(JavaPlugin plugin,PrivateMessageManager messages,
                                 ChatManager chat,RankManager ranks,
                                 Set<String> administrativeNodes,boolean replyMode,
                                 String chatAdminPermission){
        this.plugin=plugin;this.messages=messages;this.chat=chat;this.ranks=ranks;
        this.administrativeNodes=Set.copyOf(administrativeNodes);this.replyMode=replyMode;
        this.chatAdminPermission=chatAdminPermission;
    }
    @Override public String permission(){return "tools.msg.use";}

    @Override public void execute(CommandSourceStack source,String[] args) {
        if(!(source.getSender() instanceof Player player)){
            Messages.error(source.getSender(),"Prywatne wiadomości są dostępne tylko w grze.");return;
        }
        if(!player.hasPermission(permission())){Messages.unknown(player);return;}
        if(replyMode){
            if(args.length==0){Messages.usage(player,"/r <wiadomość>");return;}
            UUID partner=messages.partner(player.getUniqueId());
            Player target=partner==null?null:Bukkit.getPlayer(partner);
            if(target==null){Messages.error(player,"Brak dostępnego rozmówcy do odpowiedzi.");return;}
            send(player,target,String.join(" ",args));
            return;
        }
        if(args.length==0 || args[0].equalsIgnoreCase("pomoc")){
            CommandTextRegistry.help(player,"msg");return;
        }
        String operation=args[0].toLowerCase(Locale.ROOT);
        switch(operation){
            case "wylacz","wyłącz","wlacz","włącz" -> {
                if(args.length!=1){Messages.usage(player,"/msg <wlacz|wylacz>");return;}
                boolean disabled=operation.startsWith("wy");
                save(player,messages.disable(player.getUniqueId(),disabled),disabled
                        ?"Wyłączono prywatne wiadomości.":"Włączono prywatne wiadomości.");
            }
            case "status" -> {
                if(args.length!=1){Messages.usage(player,"/msg status");return;}
                Messages.info(player,"Prywatne wiadomości: "+(messages.state().disabled(player.getUniqueId())
                        ?"&#FF727FWyłączone":"&#70D6E8Włączone"));
                Messages.info(player,"Ignorowanych graczy: &#FFD166"+
                        messages.state().ignoredBy(player.getUniqueId()).size());
            }
            case "wycisz" -> {
                if(args.length!=2){Messages.usage(player,"/msg wycisz <nick>");return;}
                Player target=Bukkit.getPlayerExact(args[1]);
                if(target==null){Messages.error(player,"Ten gracz musi być online.");return;}
                if(player.getUniqueId().equals(target.getUniqueId())){
                    Messages.error(player,"Nie możesz wyciszyć samego siebie.");return;
                }
                save(player,messages.ignore(player.getUniqueId(),target.getUniqueId(),
                        target.getName(),true),"Ignorujesz prywatne wiadomości od "+target.getName()+".");
            }
            case "odcisz" -> {
                if(args.length!=2){Messages.usage(player,"/msg odcisz <nick>");return;}
                UUID target=null;
                for(var entry:messages.state().ignoredBy(player.getUniqueId()).entrySet()){
                    if(entry.getValue().equalsIgnoreCase(args[1])){
                        target=UUID.fromString(entry.getKey());break;
                    }
                }
                if(target==null){Messages.error(player,"Nie ignorujesz tego gracza.");return;}
                save(player,messages.ignore(player.getUniqueId(),target,args[1],false),
                        "Możesz ponownie odbierać wiadomości od "+args[1]+".");
            }
            case "wyciszeni","ignorowani" -> {
                if(args.length!=1){Messages.usage(player,"/msg wyciszeni");return;}
                var ignored=messages.state().ignoredBy(player.getUniqueId());
                Messages.title(player,"IGNOROWANI GRACZE");
                if(ignored.isEmpty()){Messages.info(player,"Nikogo nie ignorujesz.");return;}
                ignored.values().stream().sorted(String.CASE_INSENSITIVE_ORDER).limit(30)
                        .forEach(nick->Messages.line(player,"&#A8A8B7» &#FFD166"+nick));
                if(ignored.size()>30)Messages.info(player,"Więcej ignorowanych: "+(ignored.size()-30)+".");
            }
            case "przeladuj","przeładuj" -> {
                if(!ToolsAccess.admin(player,ranks,chatAdminPermission)){
                    Messages.unknown(player);return;
                }
                save(player,messages.reloadConfig(),"Wczytano PrivateMessages.json.");
            }
            default -> {
                if(args.length<2){Messages.usage(player,"/msg <nick> <wiadomość>");return;}
                Player target=Bukkit.getPlayerExact(args[0]);
                if(target==null){Messages.error(player,"Ten gracz nie jest online.");return;}
                send(player,target,String.join(" ",Arrays.copyOfRange(args,1,args.length)));
            }
        }
    }

    private void send(Player sender,Player target,String message){
        var cfg=messages.config();
        if(message.isBlank() || message.length()>cfg.maxLength()){
            Messages.error(sender,"Wiadomość musi mieć od 1 do "+cfg.maxLength()+" znaków.");return;
        }
        long now=System.currentTimeMillis();
        RankSnapshot snapshot=ranks==null?RankSnapshot.empty():ranks.snapshot();
        // Fail closed: nie odsłaniamy skrzynki moderatora przed wczytaniem rang z SQL.
        if(ranks!=null && snapshot.ranks().isEmpty()){
            Messages.error(sender,"Trwa wczytywanie uprawnień. Spróbuj ponownie.");return;
        }
        boolean senderOp=sender.isOp();
        boolean recipientProtected=PrivateMessagePolicy.isStaff(
                snapshot,target.getUniqueId(),target.isOp(),administrativeNodes);
        boolean staffAccess=ToolsAccess.allowed(senderOp,snapshot,sender.getUniqueId(),
                "tools.msg.staff",false);
        var mute=chat.mute(sender.getUniqueId());
        boolean muted=mute!=null && mute.active(now) && !ToolsAccess.allowed(
                senderOp,snapshot,sender.getUniqueId(),"tools.chat.bypass.mute",false);
        var result=PrivateMessagePolicy.check(cfg,messages.state(),sender.getUniqueId(),
                target.getUniqueId(),staffAccess,recipientProtected,muted,now,
                ToolsAccess.allowed(senderOp,snapshot,sender.getUniqueId(),"tools.msg.bypass.cooldown",false)
                        ?0L:messages.lastSent(sender.getUniqueId()));
        if(result!=PrivateMessagePolicy.Result.ALLOW) {
            switch(result){
                case SELF -> Messages.error(sender,"Nie możesz pisać prywatnych wiadomości do siebie.");
                case DISABLED_GLOBALLY -> Messages.error(sender,"Prywatne wiadomości zostały wyłączone na serwerze.");
                case DISABLED_SENDER -> Messages.error(sender,"Masz wyłączone prywatne wiadomości. Użyj /msg wlacz.");
                case DISABLED_TARGET, IGNORED -> Messages.error(sender,
                        "Ten gracz nie przyjmuje od Ciebie prywatnych wiadomości.");
                case STAFF_PROTECTED -> Messages.error(sender,
                        "Nie możesz wysyłać prywatnych wiadomości do administracji.");
                case MUTED -> Messages.error(sender,"Masz wyciszony czat i nie możesz wysyłać wiadomości.");
                case COOLDOWN -> Messages.error(sender,"Poczekaj chwilę przed kolejną prywatną wiadomością.");
                default -> {}
            }
            return;
        }
        // Nie interpretujemy kolorów, komend ani MiniMessage podanych przez gracza.
        String clean=message.replace('\n',' ').replace('\r',' ');
        sender.sendMessage(format(cfg.senderFormat(),sender.getName(),target.getName(),clean));
        target.sendMessage(format(cfg.receiverFormat(),sender.getName(),target.getName(),clean));
        messages.delivered(sender.getUniqueId(),target.getUniqueId(),now);
    }
    private static Component format(String template,String sender,String target,String message){
        String[] halves=template.replace("{sender}",sender).replace("{target}",target)
                .split("\\{message\\}",-1);
        Component result=Colors.color(halves[0]).append(Component.text(message));
        for(int i=1;i<halves.length;i++){
            result=result.append(Colors.color(halves[i]));
            if(i<halves.length-1)result=result.append(Component.text(message));
        }
        return result;
    }
    private void save(Player player,CompletableFuture<Void> operation,String success) {
        operation.whenComplete((unused,error)->{
            if(!plugin.isEnabled())return;
            plugin.getServer().getScheduler().runTask(plugin,()->{
                if(!player.isOnline())return;
                if(error!=null) {
                    if(pl.lokos.tools.helpers.StateChanges.reportUnchanged(player,error))return;
                    plugin.getLogger().warning("Nie udało się zapisać MSG: "+error);
                    Messages.error(player,"Nie udało się zapisać ustawienia wiadomości.");
                } else Messages.success(player,success);
            });
        });
    }
    @Override public Collection<String> suggest(CommandSourceStack source,String[] args) {
        if(!(source.getSender() instanceof Player player) || !player.hasPermission(permission()))
            return List.of();
        if(replyMode || args.length!=1)return List.of();
        List<String> candidates=new ArrayList<>(List.of("wlacz","wylacz","status",
                "wycisz","odcisz","wyciszeni","pomoc"));
        RankSnapshot snapshot=ranks==null?RankSnapshot.empty():ranks.snapshot();
        boolean canStaff=ToolsAccess.allowed(player.isOp(),snapshot,player.getUniqueId(),
                "tools.msg.staff",false);
        for(Player target:Bukkit.getOnlinePlayers()){
            if(!target.equals(player) && (canStaff || !PrivateMessagePolicy.isStaff(snapshot,
                    target.getUniqueId(),target.isOp(),administrativeNodes)))
                candidates.add(target.getName());
        }
        return candidates.stream().filter(v->v.toLowerCase(Locale.ROOT)
                .startsWith(args[0].toLowerCase(Locale.ROOT))).limit(50).toList();
    }
}
