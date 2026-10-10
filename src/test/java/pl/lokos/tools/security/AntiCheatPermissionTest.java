package pl.lokos.tools.security;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.commands.AntiCheatCommand;
import pl.lokos.tools.manager.RankManager;
import java.lang.reflect.Proxy;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AntiCheatPermissionTest {
    @SuppressWarnings("unchecked")
    private static Player sender(boolean op) {
        UUID id=UUID.randomUUID();
        return (Player)Proxy.newProxyInstance(Player.class.getClassLoader(),
                new Class[]{Player.class},(proxy,method,args)->switch(method.getName()){
                    case "isOp" -> op;
                    case "getUniqueId" -> id;
                    case "hasPermission" -> false; // sprzeczny PermissionAttachment
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy==args[0];
                    default -> {
                        if(method.getReturnType()==boolean.class)yield false;
                        if(method.getReturnType()==int.class)yield 0;
                        yield null;
                    }
                });
    }
    @Test void paperGateCannotDenyOpWhenAttachmentIsFalse(){
        var cmd=new AntiCheatCommand(null,null,new RankManager(null,null,null),"tools.antycheat.admin");
        assertTrue(cmd.canUse(sender(true)));
    }
    @Test void ordinaryPlayerCannotToggleGlobalDetection(){
        var cmd=new AntiCheatCommand(null,null,new RankManager(null,null,null),"tools.antycheat.admin");
        assertFalse(cmd.canUse(sender(false)));
    }
}
