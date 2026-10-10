package pl.lokos.tools.security;

import io.papermc.paper.command.brigadier.BasicCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.commands.*;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprawdza faktyczne BasicCommand.canUse() uruchamiane przez Paper/Brigadier.
 * Samo ToolsAccess.admin() wcześniej przechodziło testy, a Brigadier i tak
 * odrzucał gracza z OP przez false w Bukkit PermissionAttachment.
 */
class AdminCommandOpAccessTest {
    private final RankManager unloadedRanks=new RankManager(null,null,null);

    private static <T> T sender(Class<T> type,boolean op,boolean hasPermission){
        UUID id=UUID.randomUUID();
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},
                (proxy,method,args)->switch(method.getName()){
                    case "isOp" -> op;
                    case "hasPermission" -> hasPermission;
                    case "getUniqueId" -> id;
                    case "getName" -> "Test";
                    case "toString" -> "TestSender";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy==args[0];
                    default -> {
                        Class<?> result=method.getReturnType();
                        if(result==boolean.class)yield false;
                        if(result==int.class)yield 0;
                        if(result==long.class)yield 0L;
                        yield null;
                    }
                }));
    }

    private List<BasicCommand> commands(){
        return List.of(
                new RankCommand(null,unloadedRanks,"tools.ranga.admin",null),
                new RegionCommand(null,null,null,null,unloadedRanks,null,null,"tools.region.admin"),
                new ToolsCommand(null,null,null,null,"tools.admin",null,null,unloadedRanks),
                new ChatCommand(null,null,unloadedRanks,"tools.chat.admin"),
                new CrateCommand(null,unloadedRanks,"tools.skrzynia.admin"),
                new WhitelistCommand(null,null,null,()->unloadedRanks)
        );
    }

    @Test void actualOperatorCanUseEveryAdminCommandEvenWhenAttachmentSaysFalse(){
        Player operator=sender(Player.class,true,false);
        assertFalse(operator.hasPermission("tools.ranga.admin"),
                "Test musi odtworzyć konflikt OP z fałszywym PermissionAttachment");
        for(BasicCommand command:commands())
            assertTrue(command.canUse(operator),command.permission());
    }

    @Test void ordinaryPlayerCannotSeeOrExecuteAdminCommandsEvenIfBukkitPermissionSaysTrue(){
        Player ordinary=sender(Player.class,false,true);
        for(BasicCommand command:commands())
            assertFalse(command.canUse(ordinary),command.permission());
    }

    @Test void consoleAlwaysRetainsAdministrativeCommands(){
        ConsoleCommandSender console=sender(ConsoleCommandSender.class,true,false);
        for(BasicCommand command:commands())
            assertTrue(command.canUse(console),command.permission());
    }

    @Test void opHasAccessWhileRankSnapshotIsStillLoading(){
        UUID uuid=UUID.randomUUID();
        assertTrue(ToolsAccess.allowed(true,RankSnapshot.empty(),uuid,"tools.ranga.admin",true));
        assertFalse(ToolsAccess.allowed(false,RankSnapshot.empty(),uuid,"tools.ranga.admin",true));
    }
}
