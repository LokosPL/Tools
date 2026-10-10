package pl.lokos.tools.commands;

import java.util.*;
import java.util.function.Predicate;

/**
 * Przewidywalny TAB per argument i uprawnienie. Bez Bukkit API; kandydatów
 * graczy dostarcza warstwa wykonania, po odfiltrowaniu ukrytych graczy.
 */
public final class StaffSuggestions {
    private StaffSuggestions(){}

    public static List<String> forCommand(StaffCommand.Kind kind,String[] args,
            Collection<String> visiblePlayers,Predicate<String> permitted,boolean playerSender) {
        if(args.length==0 || args.length>4)return List.of();
        String root=args[0].toLowerCase(Locale.ROOT);
        List<String> players=List.copyOf(visiblePlayers);
        List<String> options=new ArrayList<>();
        switch(kind){
            case TP -> {
                if(args.length==1){
                    options.addAll(players);
                    if(permitted.test("tools.tp.all")&&playerSender)options.add("*");
                    if(playerSender)options.addAll(List.of("~","0","100","-100"));
                }else if(args.length==2){
                    if(root.equals("*") && permitted.test("tools.tp.all") ||
                            players.stream().anyMatch(v->v.equalsIgnoreCase(root)) &&
                            permitted.test("tools.tp.others"))
                        options.addAll(List.of("~","0","100","-100"));
                    else if(playerSender)options.addAll(List.of("~","64","100","0"));
                }else if(args.length<=4){
                    if(!root.equals("*")||permitted.test("tools.tp.all"))
                        options.addAll(List.of("~",args.length==3?"64":"0","100","-100"));
                }
            }
            case INVENTORYOPEN -> {
                if(args.length==1){
                    options.add("eq");
                    if(permitted.test("tools.inventoryopen.enderchest"))options.add("enderchest");
                }else if(args.length==2 && (
                        root.equals("eq")||root.equals("inventory")||root.equals("ekwipunek")
                        || (root.equals("enderchest")||root.equals("ender")||root.equals("ec"))
                        && permitted.test("tools.inventoryopen.enderchest")))
                    options.addAll(players);
            }
            case GAMEMODE -> {
                if(args.length==1){
                    if(playerSender)options.addAll(List.of("1","2","3","4","survival","creative","adventure","spectator"));
                    if(permitted.test("tools.gamemode.others"))options.addAll(players);
                }else if(args.length==2 && permitted.test("tools.gamemode.others"))
                    options.addAll(List.of("1","2","3","4","survival","creative","adventure","spectator"));
            }
            case VANISH, FLY -> {
                String other=kind==StaffCommand.Kind.VANISH?"tools.vanish.others":"tools.fly.others";
                if(args.length==1){
                    if(playerSender)options.addAll(List.of("wlacz","wylacz"));
                    if(permitted.test(other))options.addAll(players);
                }else if(args.length==2&&permitted.test(other))options.addAll(List.of("wlacz","wylacz"));
            }
            case SPEED -> {
                if(args.length==1){
                    if(playerSender)options.addAll(List.of("1","2","3","4","5","6","7","8","9","10","walk","fly"));
                    if(permitted.test("tools.speed.others"))options.addAll(players);
                }else if(args.length==2){
                    if(root.equals("walk")||root.equals("fly")||permitted.test("tools.speed.others"))
                        options.addAll(List.of("1","2","3","4","5","6","7","8","9","10"));
                }else if(args.length==3 && (root.equals("walk")||root.equals("fly"))
                        && permitted.test("tools.speed.others"))options.addAll(players);
            }
            case BROADCAST -> {
                if(args.length==1)options.addAll(List.of("30s","1m","5m","30m","1h","2h","1d","7d","wylacz"));
            }
            case HELPOP -> { /* dowolny tekst, nigdy lista nicków administracji */ }
        }
        String typed=args[args.length-1].toLowerCase(Locale.ROOT);
        return options.stream().distinct().filter(s->s.toLowerCase(Locale.ROOT).startsWith(typed))
                .sorted(String.CASE_INSENSITIVE_ORDER).limit(70).toList();
    }
}
