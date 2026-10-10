package pl.lokos.tools.security;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.manager.RankSnapshot;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TeleportPermissionTest {
    private static final String INSTANT = "tools.lokalizacje.instant";
    private static final UUID ID = UUID.randomUUID();

    private RankSnapshot ranked(Set<String> permissions, Long expiresAt) {
        Map<String,RankSnapshot.Rank> roles = Map.of(
                "gracz", new RankSnapshot.Rank("gracz","&7Gracz","",9999,""),
                "vip", new RankSnapshot.Rank("vip","&aVIP","",4,""));
        return new RankSnapshot(roles,Map.of("gracz",Set.of(),"vip",permissions),
                Map.of(ID,new RankSnapshot.Grant("vip",expiresAt)),Map.of());
    }

    @Test void anyAssignedRankDoesNotGrantInstantTeleport() {
        assertFalse(ToolsAccess.allowed(false,ranked(Set.of("tools.spawn"),null),ID,INSTANT,false));
        assertFalse(ToolsAccess.allowed(false,ranked(Set.of("tools.lokalizacje"),null),ID,INSTANT,false));
    }

    @Test void instantTeleportRequiresSpecificPermissionOrWildcard() {
        assertTrue(ToolsAccess.allowed(false,ranked(Set.of(INSTANT),null),ID,INSTANT,false));
        assertTrue(ToolsAccess.allowed(false,ranked(Set.of("*"),null),ID,INSTANT,false));
        assertTrue(ToolsAccess.allowed(true,ranked(Set.of(),null),ID,INSTANT,false));
    }

    @Test void expiredRankDoesNotGrantInstantTeleport() {
        assertFalse(ToolsAccess.allowed(false,
                ranked(Set.of(INSTANT),System.currentTimeMillis()-1000),ID,INSTANT,false));
        assertFalse(ToolsAccess.allowed(false,
                ranked(Set.of("*"),System.currentTimeMillis()-1000),ID,INSTANT,false));
    }
}
