package pl.lokos.tools.security;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.manager.RankSnapshot;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ToolsAccessTest {
    private static final RankSnapshot.Rank GRACZ=
            new RankSnapshot.Rank("gracz","&7Gracz","",9999,"");
    private static final RankSnapshot.Rank MODERATOR=
            new RankSnapshot.Rank("moderator","&bMod","",2,"");

    private static RankSnapshot snapshot(UUID id,Set<String> base,Set<String> mod,Long expiry) {
        Map<UUID,RankSnapshot.Grant> grants=expiry==null?
                Map.of(id,new RankSnapshot.Grant("moderator",null)):
                Map.of(id,new RankSnapshot.Grant("moderator",expiry));
        return new RankSnapshot(Map.of("gracz",GRACZ,"moderator",MODERATOR),
                Map.of("gracz",base,"moderator",mod),grants,Map.of());
    }

    @Test void nonOpWithoutAssignedRankCannotRunAdminEvenWithAccidentalWildcard() {
        UUID id=UUID.randomUUID();
        var rank=new RankSnapshot(Map.of("gracz",GRACZ),
                Map.of("gracz",Set.of("*","tools.region.admin","tools.admin")),
                Map.of(),Map.of());
        assertFalse(ToolsAccess.allowed(false,rank,id,"tools.region.admin",true));
        assertFalse(ToolsAccess.allowed(false,rank,id,"tools.admin",true));
        assertFalse(ToolsAccess.allowed(false,rank,id,"tools.whitelist.admin",true));
        assertFalse(ToolsAccess.allowed(false,rank,id,"bukkit.command.op",false));
    }

    @Test void rankRequiresActualNodeOrExplicitStar() {
        UUID id=UUID.randomUUID();
        var simple=snapshot(id,Set.of(),Set.of("tools.lokalizacje"),null);
        assertFalse(ToolsAccess.allowed(false,simple,id,"tools.region.admin",true));
        var moderator=snapshot(id,Set.of(),Set.of("tools.region.admin"),null);
        assertTrue(ToolsAccess.allowed(false,moderator,id,"tools.region.admin",true));
        var wildcard=snapshot(id,Set.of(),Set.of("*"),null);
        assertTrue(ToolsAccess.allowed(false,wildcard,id,"tools.region.admin",true));
    }

    @Test void expiredOrUnloadedRankCannotElevatePlayer() {
        UUID id=UUID.randomUUID();
        var expired=snapshot(id,Set.of(),Set.of("*"),System.currentTimeMillis()-5000);
        assertFalse(ToolsAccess.allowed(false,expired,id,"tools.admin",true));
        assertFalse(ToolsAccess.allowed(false,RankSnapshot.empty(),id,"tools.region.admin",true));
        assertTrue(ToolsAccess.allowed(true,RankSnapshot.empty(),id,"tools.region.admin",true));
        assertTrue(ToolsAccess.allowed(false,RankSnapshot.empty(),id,"tools.spawn",false));
    }
}
