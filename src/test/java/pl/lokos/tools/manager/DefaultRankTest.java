package pl.lokos.tools.manager;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DefaultRankTest {
    private final RankSnapshot.Rank base=new RankSnapshot.Rank("gracz","&7[Gracz]","",9999,"");
    private final RankSnapshot.Rank vip=new RankSnapshot.Rank("vip","&a[VIP]","",10,"");
    @Test void unassignedPlayerInheritsDefaultRankAndPermissions(){
        RankSnapshot snapshot=new RankSnapshot(Map.of("gracz",base,"vip",vip),
                Map.of("gracz",Set.of("tools.lokalizacje")),Map.of(),Map.of());
        UUID player=UUID.randomUUID();
        assertEquals("gracz",snapshot.forPlayer(player).name());
        assertEquals(Set.of("tools.lokalizacje"),snapshot.permissionsFor(player));
    }
    @Test void expiredGrantFallsBackToDefault(){
        UUID player=UUID.randomUUID();
        RankSnapshot snapshot=new RankSnapshot(Map.of("gracz",base,"vip",vip),Map.of(),
                Map.of(player,new RankSnapshot.Grant("vip",System.currentTimeMillis()-10)),Map.of());
        assertEquals("gracz",snapshot.forPlayer(player).name());
    }
    @Test void activeGrantKeepsAssignedRank(){
        UUID player=UUID.randomUUID();
        RankSnapshot snapshot=new RankSnapshot(Map.of("gracz",base,"vip",vip),Map.of(),
                Map.of(player,new RankSnapshot.Grant("vip",null)),Map.of());
        assertEquals("vip",snapshot.forPlayer(player).name());
    }
}
