package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.config.ToolsConfig;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.inventorys.RegionMenuFactory;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RegionManager;
import pl.lokos.tools.region.*;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/** Paper BasicCommand: rejestracja w Javie, bez commands w plugin.yml. */
public final class RegionCommand implements BasicCommand {
    private static final List<String> ACTIONS=List.of("stworz","stwórz","edytuj","usun","lista",
            "info","spawn","różdżka","rozdzka","podregion","ochrona","pomoc");
    private final JavaPlugin plugin;
    private final RegionManager regions;
    private final RegionSelection selection;
    private final RegionMenuFactory menus;
    private final RankManager ranks;
    private final ToolsConfig.Regions settings;
    private final NamespacedKey wandKey;
    private final String commandPermission;

    public RegionCommand(JavaPlugin plugin,RegionManager regions,RegionSelection selection,
                         RegionMenuFactory menus,RankManager ranks,ToolsConfig.Regions settings,NamespacedKey wandKey,String permission) {
        this.plugin=plugin;this.regions=regions;this.selection=selection;this.menus=menus;
        this.ranks=ranks;this.settings=settings;this.wandKey=wandKey;this.commandPermission=permission;
    }

    @Override public String permission() {return commandPermission;}

    @Override public void execute(CommandSourceStack source,String[] args) {
        CommandSender sender=source.getSender();
        if(args.length==0 || args[0].equalsIgnoreCase("pomoc")) {help(sender);return;}
        if(!regions.ready()) {
            Messages.error(sender,"Regiony nie są jeszcze gotowe. Sprawdź połączenie MySQL.");return;
        }
        try {
            String action=args[0].toLowerCase(Locale.ROOT);
            switch(action) {
                case "stworz","stwórz" -> create(sender,args);
                case "podregion" -> nested(sender,args);
                case "ochrona" -> protection(sender,args);
                case "spawn" -> spawn(sender,args);
                case "różdżka","rozdzka" -> wand(sender,args);
                case "edytuj" -> edit(sender,args);
                case "usun" -> delete(sender,args);
                case "lista" -> list(sender);
                case "info" -> info(sender,args);
                default -> {
                    Messages.error(sender,"Nieznana komenda: &c"+args[0]);
                    help(sender);
                }
            }
        } catch(IllegalArgumentException error) {
            Messages.error(sender,error.getMessage());
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if(sender instanceof Player player)return player;
        throw new IllegalArgumentException("Tę komendę możesz wykonać tylko w grze.");
    }
    public static String name(String input) {
        String text=Normalizer.normalize(input,Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        if(!text.matches("[\\p{L}0-9_-]{1,24}"))
            throw new IllegalArgumentException("Nazwa: od 1 do 24 liter, cyfr, _ lub -.");
        return text;
    }
    private int positiveRadius(String value) {
        int radius;
        try {radius=Integer.parseInt(value);}
        catch(NumberFormatException e){throw new IllegalArgumentException("Promień regionu musi być liczbą.");}
        if(radius<1 || radius>settings.maxRadius())
            throw new IllegalArgumentException("Promień musi wynosić od 1 do "+settings.maxRadius()+".");
        return radius;
    }
    private void create(CommandSender sender,String[] args) {
        if(args.length!=3) {
            failSyntax(sender,"Podaj nazwę i promień regionu.","/region stworz <nazwa> <promień>");return;
        }
        Player p=requirePlayer(sender);
        String name=name(args[1]);
        int radius=positiveRadius(args[2]);
        Location loc=p.getLocation();
        int x=loc.getBlockX(), z=loc.getBlockZ();
        Region r=new Region(name,loc.getWorld().getUID(),x-radius,x+radius,z-radius,z+radius,
                null,null,Map.of(),null);
        regions.index().validateNew(r);
        perform(sender,()->regions.create(r),"Utworzono region &a"+name+"&7 o promieniu &a"
                +radius+"&7 bloków (&a"+(radius*2+1)+" × "+(radius*2+1)+"&7).");
    }
    private void nested(CommandSender sender,String[] args) {
        if(args.length!=3) {
            failSyntax(sender,"Podaj region nadrzędny i nazwę podregionu.",
                    "/region podregion <rodzic> <nazwa>");return;
        }
        requirePlayer(sender);
        Region parent=region(args[1]);
        String name=name(args[2]);
        RegionSelection.Pair pair=selection.get(((Player)sender).getUniqueId());
        if(!pair.complete()) {
            Messages.error(sender,"Wybierz najpierw dwa narożniki różdżką.");
            Messages.hint(sender,"Użyj &a/region rozdzka&7, potem lewy i prawy przycisk myszy.");
            return;
        }
        Location one=pair.first(),two=pair.second();
        Region child=new Region(name,one.getWorld().getUID(),
                Math.min(one.getBlockX(),two.getBlockX()),Math.max(one.getBlockX(),two.getBlockX()),
                Math.min(one.getBlockZ(),two.getBlockZ()),Math.max(one.getBlockZ(),two.getBlockZ()),
                parent.name(),null,Map.of(),null);
        regions.index().validateNew(child);
        perform(sender,()->regions.create(child),"Utworzono podregion &a"+name+
                "&7 w regionie &a"+parent.name()+"&7.",()-> {
                    Player player=(Player)sender;
                    selection.clear(player.getUniqueId());
                    for(int slot=0;slot<player.getInventory().getSize();slot++){
                        ItemStack item=player.getInventory().getItem(slot);
                        if(item!=null && item.hasItemMeta()
                                && Byte.valueOf((byte)1).equals(item.getItemMeta().getPersistentDataContainer()
                                    .get(wandKey,PersistentDataType.BYTE))){
                            player.getInventory().setItem(slot,null);
                            break;
                        }
                    }
                });
    }
    private void protection(CommandSender sender,String[] args) {
        if(args.length!=3) {
            failSyntax(sender,"Podaj nazwę regionu i promień ochrony.",
                    "/region ochrona <region> <promień>");return;
        }
        Region parent=region(args[1]);
        if(parent.spawn()==null)throw new IllegalArgumentException(
                "Najpierw ustaw spawn regionu stojąc w nim: /region spawn.");
        int radius=positiveRadius(args[2]);
        String name=parent.name()+"_ochrona";
        if(name.length()>24)throw new IllegalArgumentException("Nazwa podregionu ochrony jest za długa.");
        int x=(int)Math.floor(parent.spawn().x()),z=(int)Math.floor(parent.spawn().z());
        Region child=new Region(name,parent.world(),x-radius,x+radius,z-radius,z+radius,
                parent.name(),null,Map.of(RegionFlag.BUILD,false,RegionFlag.BREAK,false,
                        RegionFlag.PVP,false,RegionFlag.DAMAGE,false),null);
        regions.index().validateNew(child);
        perform(sender,()->regions.create(child),
                "Utworzono bezpieczną strefę &a"+name+"&7 wokół spawnu.");
    }

    private void spawn(CommandSender sender,String[] args) {
        if(args.length!=1) {failSyntax(sender,"Ustaw spawn stojąc w chronionym regionie.","/region spawn");return;}
        Player player=requirePlayer(sender);
        Region r=regions.at(player.getLocation());
        if(r==null)throw new IllegalArgumentException("Stań w regionie, którego spawn chcesz ustawić.");
        Location l=player.getLocation();
        Region.Spawn point=new Region.Spawn(l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());
        perform(sender,()->regions.setSpawn(r.name(),point),
                "Ustawiono spawn regionu &a"+r.name()+"&7 oraz startową lokalizację nowych graczy.");
    }
    private void wand(CommandSender sender,String[] args) {
        if(args.length!=1) {failSyntax(sender,"Użyj samego polecenia.","/region rozdzka");return;}
        Player p=requirePlayer(sender);
        ItemStack stick=new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta=stick.getItemMeta();
        meta.displayName(Colors.color("&a&lRÓŻDŻKA REGIONÓW"));
        meta.lore(List.of(Colors.color("&7Lewy przycisk: &aPierwszy punkt"),
                Colors.color("&7Prawy przycisk: &aDrugi punkt"),
                Colors.color("&8Następnie: /region podregion <rodzic> <nazwa>")));
        meta.getPersistentDataContainer().set(wandKey,PersistentDataType.BYTE,(byte)1);
        stick.setItemMeta(meta);
        p.getInventory().addItem(stick).values().forEach(overflow->p.getWorld().dropItemNaturally(p.getLocation(),overflow));
        Messages.success(sender,"Otrzymano różdżkę do zaznaczania podregionów.");
    }
    private void edit(CommandSender sender,String[] args) {
        if(args.length<2) {failSyntax(sender,"Wybierz region.","/region edytuj <nazwa> [flaga|wejscie]");return;}
        Region r=region(args[1]);
        if(args.length==2) {
            menus.edit(requirePlayer(sender),r.name());return;
        }
        if(args.length==5 && args[2].equalsIgnoreCase("flaga")) {
            RegionFlag flag=RegionFlag.parse(args[3]);
            Boolean setting=switch(args[4].toLowerCase(Locale.ROOT)) {
                case "tak","true","on" -> true;
                case "nie","false","off" -> false;
                case "dziedzicz" -> null;
                default -> throw new IllegalArgumentException("Wybierz: tak, nie lub dziedzicz.");
            };
            perform(sender,()->regions.flag(r.name(),flag,setting),
                    "Zmieniono flagę &a"+flag.label()+"&7 w regionie &a"+r.name()+"&7.");
        } else if(args.length==4 && args[2].equalsIgnoreCase("wejscie")) {
            String rank=args[3].equalsIgnoreCase("wszyscy")?null:
                    name(args[3]);
            if(rank!=null && (ranks.snapshot().ranks().get(rank)==null ||
                    ranks.snapshot().ranks().get(rank).position()==null))
                throw new IllegalArgumentException("Ta ranga nie istnieje lub nie ma pozycji.");
            perform(sender,()->regions.entryRank(r.name(),rank),
                    "Dostęp do &a"+r.name()+"&7: &a"+(rank==null?"wszyscy":rank)+"&7 i wyższe rangi.");
        } else {
            failSyntax(sender,"Niepoprawne parametry edycji.",
                    "/region edytuj <nazwa> flaga <flaga> <tak|nie|dziedzicz>");
            Messages.hint(sender,"Ogranicz wejście: &a/region edytuj "+r.name()+" wejscie <ranga|wszyscy>");
        }
    }
    private void delete(CommandSender sender,String[] args) {
        if(args.length!=2) {failSyntax(sender,"Podaj nazwę regionu.","/region usun <nazwa>");return;}
        Region r=region(args[1]);
        perform(sender,()->regions.remove(r.name()),"Usunięto region &a"+r.name()+
                "&7 oraz wszystkie jego podregiony.");
    }
    private void list(CommandSender sender) {
        Messages.line(sender," ");
        Messages.title(sender,"REGIONY");
        Messages.line(sender,"&7  Nazwa &8• &7Rodzaj &8• &7Teleportacja");
        Messages.line(sender," ");
        regions.index().all().values().stream().sorted(Comparator.comparing(Region::name))
                .forEach(r->Messages.info(sender,"&a"+r.name()+" &8• &7"+(r.parent()==null?"główny":"podregion "+r.parent())+
                        " &8• &7"+(r.spawn()==null?"brak teleportu":"lokalizacja dostępna")));
    }
    private void info(CommandSender sender,String[] args) {
        if(args.length!=2) {failSyntax(sender,"Podaj nazwę regionu.","/region info <nazwa>");return;}
        Region r=region(args[1]);
        Messages.title(sender,"REGION "+r.name().toUpperCase(Locale.ROOT));
        Messages.info(sender,"Świat: &a"+Optional.ofNullable(Bukkit.getWorld(r.world())).map(w->w.getName()).orElse("niedostępny"));
        Messages.info(sender,"Obszar: &a"+(r.maxX()-r.minX()+1)+" × "+(r.maxZ()-r.minZ()+1)+" &7bloków");
        Messages.info(sender,"Rodzic: &a"+(r.parent()==null?"brak":r.parent()));
        Messages.info(sender,"Wejście od rangi: &a"+(r.entryRank()==null?"wszyscy":r.entryRank()));
        Messages.info(sender,"Teleportacja: &a"+(r.spawn()==null?"nie ustawiono":"ustawiona"));
        for(RegionFlag f:RegionFlag.values()) {
            Messages.info(sender,f.label()+" &8» "+(regions.index().enabled(r,f)?"&aDozwolone":"&cZabronione")
                    +(r.flags().containsKey(f)?"":" &8(dziedziczenie)"));
        }
    }
    private Region region(String input) {
        Region r=regions.index().byName(name(input));
        if(r==null)throw new IllegalArgumentException("Nie znaleziono regionu "+input+".");
        return r;
    }
    private void failSyntax(CommandSender sender,String explanation,String usage) {
        Messages.error(sender,explanation);Messages.usage(sender,usage);
    }
    private void help(CommandSender sender) {
        Messages.line(sender," ");
        Messages.title(sender,"ZARZĄDZANIE REGIONAMI");
        Messages.line(sender," ");
        Messages.line(sender,"&a/region stworz &7<nazwa> <promień>");
        Messages.line(sender,"&a/region rozdzka &8• &a/region podregion &7<rodzic> <nazwa>");
        Messages.line(sender,"&a/region ochrona &7<region> <promień>");
        Messages.line(sender,"&a/region edytuj &7<nazwa> &8• &a/region spawn");
        Messages.line(sender,"&a/region usun &7<nazwa> &8• &a/region lista");
        Messages.line(sender," ");
    }
    private void perform(CommandSender sender,Supplier<CompletableFuture<Void>> action,String success) {
        perform(sender,action,success,()->{});
    }
    private void perform(CommandSender sender,Supplier<CompletableFuture<Void>> action,String success,Runnable onSuccess) {
        try {
            action.get().whenComplete((v,error)->{
                if(!plugin.isEnabled())return;
                Bukkit.getScheduler().runTask(plugin,()-> {
                    if(error==null){Messages.success(sender,success);onSuccess.run();}
                    else {
                        Throwable cause=error;
                        while(cause instanceof CompletionException && cause.getCause()!=null)cause=cause.getCause();
                        Messages.error(sender,"Nie zapisano zmiany. "+(cause instanceof IllegalArgumentException?
                                cause.getMessage():"Sprawdź połączenie MySQL i konsolę."));
                        plugin.getLogger().warning("Błąd operacji regionu: "+cause);
                    }
                });
            });
        } catch(RuntimeException e) {Messages.error(sender,e.getMessage());}
    }

    @Override public Collection<String> suggest(CommandSourceStack source,String[] args) {
        if (!source.getSender().hasPermission(commandPermission)) return List.of();
        if(args.length<=1) return match(ACTIONS,args.length==0?"":args[0]);
        String command=args[0].toLowerCase(Locale.ROOT);
        if(args.length==2) {
            if(List.of("edytuj","usun","info","ochrona","podregion").contains(command))
                return match(regions.index().all().keySet(),args[1]);
        }
        if(command.equals("edytuj")) {
            if(args.length==3)return match(List.of("flaga","wejscie"),args[2]);
            if(args.length==4 && args[2].equalsIgnoreCase("flaga"))
                return match(Arrays.stream(RegionFlag.values()).map(RegionFlag::label).toList(),args[3]);
            if(args.length==4 && args[2].equalsIgnoreCase("wejscie")) {
                List<String> values=new ArrayList<>(List.of("wszyscy"));
                values.addAll(ranks.snapshot().ranks().keySet());
                return match(values,args[3]);
            }
            if(args.length==5 && args[2].equalsIgnoreCase("flaga"))
                return match(List.of("tak","nie","dziedzicz"),args[4]);
        }
        if((command.equals("stworz")||command.equals("stwórz"))&&args.length==3)
            return match(List.of("10","25","50","100","200","500"),args[2]);
        return List.of();
    }
    private static List<String> match(Collection<String> source,String typed) {
        return source.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT)))
                .sorted(String.CASE_INSENSITIVE_ORDER).limit(70).toList();
    }
}
