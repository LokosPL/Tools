package pl.lokos.tools.security;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.commands.StaffCommand;
import pl.lokos.tools.manager.RankManager;
import java.lang.reflect.Proxy;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StaffCommandPermissionTest {
    private final RankManager ranks=new RankManager(null,null,null);
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> type,boolean op){
        UUID id=UUID.randomUUID();
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->switch(m.getName()){
            case "isOp"->op;
            case "getUniqueId"->id;
            case "hasPermission"->false;
            case "hashCode"->System.identityHashCode(p);
            case "equals"->p==a[0];
            default -> {
                if(m.getReturnType()==boolean.class)yield false;
                if(m.getReturnType()==int.class)yield 0;
                yield null;
            }
        });
    }
    @Test void operatorAllowedEvenIfBukkitAttachmentDenies(){
        Player op=mock(Player.class,true);
        Player ordinary=mock(Player.class,false);
        for(StaffCommand.Kind kind:StaffCommand.Kind.values()){
            var command=new StaffCommand(null,ranks,null,null,kind,
                    "tools."+kind.name().toLowerCase());
            if(kind==StaffCommand.Kind.HELPOP){
                assertTrue(command.canUse(op));
                assertTrue(command.canUse(ordinary));
            }else{
                assertTrue(command.canUse(op),kind.name());
                assertFalse(command.canUse(ordinary),kind.name());
            }
        }
    }
    @Test void consoleCanUseAdminCommandsButNotPlayerHelpop(){
        ConsoleCommandSender console=mock(ConsoleCommandSender.class,true);
        for(StaffCommand.Kind kind:StaffCommand.Kind.values()){
            var command=new StaffCommand(null,ranks,null,null,kind,
                    "tools."+kind.name().toLowerCase());
            assertEquals(kind!=StaffCommand.Kind.HELPOP,command.canUse(console));
        }
    }
}
