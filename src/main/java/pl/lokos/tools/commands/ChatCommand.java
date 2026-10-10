package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.chat.*;
import pl.lokos.tools.config.CommandTextRegistry;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.security.ToolsAccess;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Moderacja czatu: każda zmiana wymaga własnej permisji admina, a zapis jest asynchroniczny. */
public final class ChatCommand implements BasicCommand {
    private static final List<String> ACTIONS=List.of("status","wlacz","wylacz","wyczysc",
            "ranga","wycisz","odcisz","wyciszeni","slow","ogloszenia","przeladuj","pomoc");
    private final JavaPlugin plugin;
    private final ChatManager chats;
    private final RankManager ranks;
    private final String permission;

    public ChatCommand(JavaPlugin plugin,ChatManager chats,RankManager ranks,String permission){
        this.plugin=plugin;this.chats=chats;this.ranks=ranks;this.permission=permission;
    }
    @Override public String permission(){return permission;}

    @Override public void execute(CommandSourceStack source,String[] args) {
        CommandSender sender=source.getSender();
        if(!ToolsAccess.admin(sender,ranks,permission)){Messages.unknown(sender);return;}
        if(args.length==0 || args[0].equalsIgnoreCase("pomoc")){
            CommandTextRegistry.help(sender,"chat");return;
        }
        try {
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "status" -> {
                    if(args.length!=1){usage(sender);return;}
                    var state=chats.state();
                    Messages.title(sender,"CZAT SERWERA");
                    Messages.info(sender,"Stan: "+(state.enabled()?"&#70D6E8Włączony":"&#FF727FWyłączony"));
                    Messages.info(sender,"Dostęp: &#FFD166"+(state.minimumRank()==null?"Wszyscy":state.minimumRank()));
                    Messages.info(sender,"Slow: &#FFD166"+chats.config().slowMaxMessages()
                            +" &7wiadomości na &#FFD166"+chats.config().slowWindowSeconds()+" s");
                    Messages.info(sender,"Wyciszeni: &#FFD166"+chats.activeMutes(System.currentTimeMillis()).size());
                    Messages.info(sender,"Ogłoszenia co: &#FFD166"+
                            chats.config().announcementIntervalSeconds()+" &7sekund");
                    Messages.info(sender,"Ogłoszenia: "+(state.announcementsEnabled()
                            && chats.config().announcementsEnabled()?"&#70D6E8Włączone":"&#A8A8B7Wyłączone"));
                }
                case "wlacz","włącz" -> {
                    requireCount(args,1);save(sender,()->chats.setEnabled(true),"Włączono czat.");
                }
                case "wylacz","wyłącz" -> {
                    requireCount(args,1);save(sender,()->chats.setEnabled(false),"Wyłączono czat.");
                }
                case "wyczysc","wyczyść" -> {
                    requireCount(args,1);
                    if(!chats.markChatCleared(System.currentTimeMillis())){
                        Messages.unchanged(sender,"Czat został już niedawno wyczyszczony.");
                        return;
                    }
                    for(Player target:Bukkit.getOnlinePlayers()){
                        for(int line=0;line<75;line++)target.sendMessage(net.kyori.adventure.text.Component.empty());
                        target.sendMessage(Colors.color("&#FFD166✦ &7Czat został wyczyszczony przez administrację."));
                    }
                    Messages.success(sender,"Wyczyszczono czat wszystkim graczom.");
                }
                case "ranga" -> {
                    requireCount(args,2);
                    String rank=args[1].equalsIgnoreCase("wszyscy")?null:args[1].toLowerCase(Locale.ROOT);
                    if(rank!=null) {
                        RankSnapshot.Rank chosen=ranks==null?null:ranks.snapshot().ranks().get(rank);
                        if(chosen==null || chosen.position()==null){
                            Messages.error(sender,CommandTextRegistry.text("chat","noRank"));return;
                        }
                    }
                    save(sender,()->chats.setRank(rank),"Dostęp do czatu: "+(rank==null?"wszyscy":rank)+" i wyższe rangi.");
                }
                case "wycisz" -> {
                    if(args.length<3){usage(sender);return;}
                    Player target=Bukkit.getPlayerExact(args[1]);
                    if(target==null){Messages.error(sender,CommandTextRegistry.text("chat","offline"));return;}
                    long until=ChatDuration.until(args[2],System.currentTimeMillis());
                    String reason=args.length>3?String.join(" ",Arrays.copyOfRange(args,3,args.length)):"Decyzja administracji";
                    reason=reason.replace('&',' ').replace('§',' ').replace('\n',' ').replace('\r',' ').trim();
                    if(reason.isBlank() || reason.length()>200){
                        Messages.error(sender,"Powód musi mieć od 1 do 200 znaków.");return;
                    }
                    String nick=target.getName();
                    UUID id=target.getUniqueId();
                    String finalReason=reason;
                    save(sender,()->chats.silence(id,nick,until,finalReason),"Wyciszono gracza "+nick+".");
                    Messages.error(target,"Zostałeś wyciszony. &7Powód: &f"+finalReason);
                }
                case "odcisz" -> {
                    requireCount(args,2);
                    var match=chats.findMute(args[1],System.currentTimeMillis());
                    if(match.isEmpty()){
                        Player player=Bukkit.getPlayerExact(args[1]);
                        if(player!=null) {
                            UUID id=player.getUniqueId();
                            match=chats.activeMutes(System.currentTimeMillis()).stream()
                                    .filter(e->e.uuid().equals(id)).findFirst();
                        }
                    }
                    if(match.isEmpty()){
                        Messages.unchanged(sender,"Ten gracz nie jest już wyciszony na czacie.");return;
                    }
                    var entry=match.get();
                    save(sender,()->chats.unsilence(entry.uuid()),"Cofnięto wyciszenie "+entry.mute().name()+".");
                    Player online=Bukkit.getPlayer(entry.uuid());
                    if(online!=null)Messages.success(online,"Możesz ponownie pisać na czacie.");
                }
                case "wyciszeni" -> {
                    if(args.length>2){usage(sender);return;}
                    int page=args.length==1?1:Integer.parseInt(args[1]);
                    if(page<1){Messages.error(sender,"Strona musi być większa od zera.");return;}
                    var entries=chats.activeMutes(System.currentTimeMillis());
                    int pages=Math.max(1,(entries.size()+6)/7);
                    if(page>pages){Messages.error(sender,"Ostatnia strona: "+pages+".");return;}
                    Messages.title(sender,"WYCISZENI &8• &7"+page+"/"+pages);
                    if(entries.isEmpty()){Messages.info(sender,"Nie ma wyciszonych graczy.");return;}
                    for(var entry:entries.subList((page-1)*7,Math.min(page*7,entries.size()))) {
                        String until=entry.mute().untilMillis()==0?"na stałe":
                                DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault())
                                        .format(Instant.ofEpochMilli(entry.mute().untilMillis()));
                        Messages.line(sender,"&#A8A8B7» &#FFD166"+entry.mute().name()
                                +" &8• &7"+until+" &8• &7"+entry.mute().reason());
                    }
                }
                case "slow" -> {
                    requireCount(args,3);
                    int amount=Integer.parseInt(args[1]);
                    int window=Integer.parseInt(args[2]);
                    save(sender,()->chats.setSlow(amount,window),
                            "Ustawiono limit: "+amount+" wiadomości na "+window+" sekund.");
                }
                case "ogloszenia","ogłoszenia" -> {
                    if(args.length<2){usage(sender);return;}
                    String option=args[1].toLowerCase(Locale.ROOT);
                    if(option.equals("interwal") || option.equals("odstep")){
                        requireCount(args,3);
                        int seconds=Integer.parseInt(args[2]);
                        save(sender,()->chats.setAnnouncementInterval(seconds),
                                "Automatyczne wiadomości będą wysyłane co "+seconds+" sekund.");
                    }else if(option.equals("lista")){
                        requireCount(args,2);
                        Messages.title(sender,"AUTOMATYCZNE WIADOMOŚCI");
                        Messages.info(sender,"Odstęp: &#FFD166"+
                                chats.config().announcementIntervalSeconds()+" &7sekund");
                        int index=0;
                        for(String line:chats.config().announcements())
                            Messages.line(sender,"&#FFD166"+(++index)+". &#A8A8B7"+line);
                    }else {
                        requireCount(args,2);
                        boolean enabled=switch(option){
                            case "wlacz","włącz" -> true;
                            case "wylacz","wyłącz" -> false;
                            default -> throw new IllegalArgumentException("Wpisz wlacz, wylacz, lista lub interwal.");
                        };
                        save(sender,()->chats.setAnnouncements(enabled),
                                enabled?"Włączono automatyczne wiadomości.":"Wyłączono automatyczne wiadomości.");
                    }
                }
                case "przeladuj","przeładuj" -> {
                    requireCount(args,1);
                    save(sender,chats::reloadConfig,"Odświeżono Chat.json.");
                }
                default -> Messages.unknown(sender);
            }
        } catch(NumberFormatException ex) {
            Messages.error(sender,"Podaj poprawny numer strony.");
        } catch(IllegalArgumentException ex){
            Messages.error(sender,ex.getMessage());
        }
    }
    private static void requireCount(String[] args,int length){
        if(args.length!=length)throw new IllegalArgumentException("Niepoprawna liczba argumentów. Użyj /chat pomoc.");
    }
    private static void usage(CommandSender sender){CommandTextRegistry.help(sender,"chat");}
    private void save(CommandSender sender,Supplier<CompletableFuture<Void>> update,String success){
        update.get().whenComplete((v,error)->{
            if(!plugin.isEnabled())return;
            plugin.getServer().getScheduler().runTask(plugin,()->{
                if(error==null)Messages.success(sender,success);
                else {
                    if(pl.lokos.tools.helpers.StateChanges.reportUnchanged(sender,error))return;
                    plugin.getLogger().warning("Nie zapisano czatu: "+error);
                    Messages.error(sender,"Zmiana działa w pamięci, lecz nie udało się zapisać ChatState.json.");
                }
            });
        });
    }

    @Override public Collection<String> suggest(CommandSourceStack source,String[] args){
        if(!ToolsAccess.admin(source.getSender(),ranks,permission))return List.of();
        if(args.length==1)return prefix(ACTIONS,args[0]);
        if(args.length==2){
            String action=args[0].toLowerCase(Locale.ROOT);
            if(action.equals("ogloszenia") || action.equals("ogłoszenia"))
                return prefix(List.of("wlacz","wylacz","lista","interwal"),args[1]);
            if(action.equals("ranga")){
                List<String> options=new ArrayList<>(List.of("wszyscy"));
                if(ranks!=null)options.addAll(ranks.snapshot().ranks().keySet());
                return prefix(options,args[1]);
            }
            if(action.equals("wycisz"))return prefix(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),args[1]);
            if(action.equals("odcisz"))return prefix(
                    chats.activeMutes(System.currentTimeMillis()).stream().map(e->e.mute().name()).toList(),args[1]);
        }
        if(args.length==3 && args[0].equalsIgnoreCase("wycisz"))
            return prefix(List.of("30s","5m","30m","2h","1d","*"),args[2]);
        return List.of();
    }
    private static List<String> prefix(Collection<String> choices,String value){
        return choices.stream().filter(s->s.toLowerCase(Locale.ROOT)
                .startsWith(value.toLowerCase(Locale.ROOT))).limit(40).toList();
    }
}
