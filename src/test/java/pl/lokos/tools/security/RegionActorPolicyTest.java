package pl.lokos.tools.security;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.manager.RankSnapshot;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegionActorPolicyTest {
    private final UUID uuid=UUID.randomUUID();
    private RankSnapshot ranks(Set<String> perms, boolean assigned) {
        var groups=Map.of(
                "gracz",new RankSnapshot.Rank("gracz","","",9999,""),
                "moderator",new RankSnapshot.Rank("moderator","","",2,""));
        var grants=assigned?Map.of(uuid,new RankSnapshot.Grant("moderator",null)):
                Map.<UUID,RankSnapshot.Grant>of();
        return new RankSnapshot(groups,Map.of("gracz",Set.of(),"moderator",perms),
                grants,Map.of());
    }
    private boolean managed(boolean op, Set<String> perms, boolean assigned) {
        return ToolsAccess.allowed(op,ranks(perms,assigned),uuid,"tools.region.admin",true);
    }
    @Test void regionManagerCanBuildAndBreakWithProtectedFlags() {
        boolean manager=managed(false,Set.of("tools.region.admin"),true);
        assertFalse(RegionActorPolicy.denied(manager,true));
        assertFalse(RegionActorPolicy.denied(manager,false));
    }
    @Test void regularPlayerWithNoAdminRightIsBlocked() {
        assertTrue(RegionActorPolicy.denied(managed(false,Set.of(),false),true));
        assertTrue(RegionActorPolicy.denied(managed(false,Set.of("tools.lokalizacje"),true),true));
    }
    @Test void wildcardRankCanManageAndExpiredRankCannot() {
        assertFalse(RegionActorPolicy.denied(managed(false,Set.of("*"),true),true));
        var expired=new RankSnapshot(
                Map.of("gracz",new RankSnapshot.Rank("gracz","","",9999,""),
                       "mod",new RankSnapshot.Rank("mod","","",3,"")),
                Map.of("gracz",Set.of(),"mod",Set.of("*")),
                Map.of(uuid,new RankSnapshot.Grant("mod",System.currentTimeMillis()-100)),Map.of());
        assertTrue(RegionActorPolicy.denied(
                ToolsAccess.allowed(false,expired,uuid,"tools.region.admin",true),true));
    }
}
