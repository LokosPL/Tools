package pl.lokos.tools.chat;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.manager.RankSnapshot;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatPolicyTest {
    private final UUID id=UUID.randomUUID();
    private final RankSnapshot.Rank gracz=new RankSnapshot.Rank("gracz","","",9999,"");
    private final RankSnapshot.Rank mod=new RankSnapshot.Rank("mod","","",2,"");
    private final ChatConfig cfg=new ChatConfig();

    private RankSnapshot snapshot(Set<String> grants,boolean moderator) {
        return new RankSnapshot(Map.of("gracz",gracz,"mod",mod),
                Map.of("gracz",Set.of(),"mod",grants),
                moderator?Map.of(id,new RankSnapshot.Grant("mod",null)):Map.of(),Map.of());
    }
    private ChatPolicy.Decision check(ChatStateFile state,RankSnapshot rank,boolean op,long at){
        return ChatPolicy.evaluate(state,cfg,rank,id,op,at,new SlowModeLimiter());
    }

    @Test void closedChatDeniesOrdinaryPlayerButPermissionBypasses() {
        var state=new ChatStateFile().withEnabled(false);
        assertEquals(ChatPolicy.Decision.CLOSED,check(state,snapshot(Set.of(),false),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(state,snapshot(Set.of("tools.chat.bypass.lock"),true),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(state,snapshot(Set.of(),false),true,1000));
    }

    @Test void rankRestrictionIsOrderedByRankPosition(){
        var state=new ChatStateFile().withRank("mod");
        assertEquals(ChatPolicy.Decision.RANK,check(state,snapshot(Set.of(),false),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(state,snapshot(Set.of(),true),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(state,snapshot(Set.of("tools.chat.bypass.lock"),true),false,1000));
    }

    @Test void mutedUsersAreDeniedBeforeSlowModeAndCanBeReleased(){
        var state=new ChatStateFile().withMute(id,new ChatStateFile.Mute("Tester",0,"Spam"));
        assertEquals(ChatPolicy.Decision.MUTED,check(state,snapshot(Set.of(),false),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(state,snapshot(Set.of("tools.chat.bypass.mute"),true),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(state.withoutMute(id),snapshot(Set.of(),false),false,1000));
        var temporary=new ChatStateFile().withMute(id,new ChatStateFile.Mute("Tester",1500,"Spam"));
        assertEquals(ChatPolicy.Decision.MUTED,check(temporary,snapshot(Set.of(),false),false,1000));
        assertEquals(ChatPolicy.Decision.ALLOW,check(temporary,snapshot(Set.of(),false),false,1500));
    }

    @Test void explicitBypassSlowDoesNotBypassMuteOrClosedChat() {
        var rank=snapshot(Set.of("tools.chat.bypass.slow"),true);
        var limiter=new SlowModeLimiter();
        var state=new ChatStateFile();
        for(int i=0;i<10;i++)
            assertEquals(ChatPolicy.Decision.ALLOW,ChatPolicy.evaluate(state,cfg,rank,id,false,2000,limiter));
        assertEquals(ChatPolicy.Decision.CLOSED,check(state.withEnabled(false),rank,false,2000));
        assertEquals(ChatPolicy.Decision.MUTED,check(
                state.withMute(id,new ChatStateFile.Mute("Tester",0,"Test")),rank,false,2000));
    }

    @Test void noPrivilegeWhenRanksNotLoaded(){
        assertEquals(ChatPolicy.Decision.RANK,check(
                new ChatStateFile().withRank("missing"),RankSnapshot.empty(),false,1000));
    }
}
