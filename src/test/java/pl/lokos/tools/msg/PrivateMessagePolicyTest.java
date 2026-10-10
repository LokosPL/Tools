package pl.lokos.tools.msg;

import org.junit.jupiter.api.Test;
import pl.lokos.tools.manager.RankSnapshot;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PrivateMessagePolicyTest {
    private static final UUID SENDER=UUID.randomUUID();
    private static final UUID RECEIVER=UUID.randomUUID();
    private final PrivateMessageConfig config=new PrivateMessageConfig();
    private final PrivateMessageState state=new PrivateMessageState();

    private PrivateMessagePolicy.Result decision(PrivateMessageState preferences,boolean staffAllowed,
                                                   boolean recipientStaff,boolean muted,long lastSent){
        return PrivateMessagePolicy.check(config,preferences,SENDER,RECEIVER,
                staffAllowed,recipientStaff,muted,10_000L,lastSent);
    }

    @Test void cannotSendPrivateMessagesToSelfEvenAsStaff() {
        assertEquals(PrivateMessagePolicy.Result.SELF,PrivateMessagePolicy.check(
                config,state,SENDER,SENDER,true,true,false,10_000L,0L));
    }

    @Test void disablingMessagesRejectsBothDirections() {
        assertEquals(PrivateMessagePolicy.Result.DISABLED_SENDER,
                decision(state.withDisabled(SENDER,true),false,false,false,0L));
        assertEquals(PrivateMessagePolicy.Result.DISABLED_TARGET,
                decision(state.withDisabled(RECEIVER,true),false,false,false,0L));
        assertEquals(PrivateMessagePolicy.Result.ALLOW,decision(state,false,false,false,0L));
    }

    @Test void ignoredSenderCannotWriteAndOtherPlayerCanStillSend(){
        PrivateMessageState saved=state.withIgnore(RECEIVER,SENDER,"Sender",true);
        assertEquals(PrivateMessagePolicy.Result.IGNORED,decision(saved,false,false,false,0L));
        assertEquals(PrivateMessagePolicy.Result.ALLOW,
                decision(saved.withIgnore(RECEIVER,SENDER,"Sender",false),false,false,false,0L));
    }

    @Test void staffInboxRejectsOrdinaryPlayersButAllowsExplicitPermission(){
        assertEquals(PrivateMessagePolicy.Result.STAFF_PROTECTED,decision(state,false,true,false,0));
        assertEquals(PrivateMessagePolicy.Result.ALLOW,decision(state,true,true,false,0));
        assertEquals(PrivateMessagePolicy.Result.ALLOW,decision(state,false,false,false,0));
    }

    @Test void timedCooldownAndGlobalMuteAlsoProtectPrivateMessages(){
        assertEquals(PrivateMessagePolicy.Result.MUTED,decision(state,false,false,true,0));
        assertEquals(PrivateMessagePolicy.Result.COOLDOWN,decision(state,false,false,false,9_000));
        assertEquals(PrivateMessagePolicy.Result.ALLOW,decision(state,false,false,false,8_000));
    }

    @Test void rankAdministrativeNodeAndExplicitProtectedRankProtectInbox(){
        RankSnapshot.Rank guest=new RankSnapshot.Rank("gracz","","",9999,"");
        RankSnapshot.Rank admin=new RankSnapshot.Rank("administrator","","",1,"");
        var permissions=Map.of("gracz",Set.<String>of(),"administrator",Set.of("tools.region.admin"));
        var snapshot=new RankSnapshot(Map.of("gracz",guest,"administrator",admin),permissions,
                Map.of(RECEIVER,new RankSnapshot.Grant("administrator",null)),Map.of());
        assertTrue(PrivateMessagePolicy.isStaff(snapshot,RECEIVER,false,Set.of("tools.region.admin")));
        assertFalse(PrivateMessagePolicy.isStaff(snapshot,SENDER,false,Set.of("tools.region.admin")));
        assertTrue(PrivateMessagePolicy.isStaff(snapshot,SENDER,true,Set.of("tools.region.admin")));

        var protectedRank=new RankSnapshot(Map.of("gracz",guest,"administrator",admin),
                Map.of("gracz",Set.of(),"administrator",Set.of("tools.msg.protected")),
                Map.of(RECEIVER,new RankSnapshot.Grant("administrator",null)),Map.of());
        assertTrue(PrivateMessagePolicy.isStaff(protectedRank,RECEIVER,false,Set.of("tools.region.admin")));
        assertFalse(PrivateMessagePolicy.isStaff(RankSnapshot.empty(),RECEIVER,false,Set.of("tools.region.admin")));
    }
}
