package pl.lokos.tools.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EventChallengesTest {
    @TempDir Path dir;

    @Test void allNineEventsHaveIndependentProgressGoalsAndTitles(){
        assertEquals(9,EventType.values().length);
        for(EventType type:EventType.values()){
            int[] goals=EventChallenges.goals(type);
            assertEquals(3,goals.length);
            assertTrue(goals[0]>0&&goals[0]<goals[1]&&goals[1]<goals[2]);
            assertEquals(3,EventChallenges.names(type).size());
            assertFalse(EventChallenges.action(type).isBlank());
        }
        assertEquals(1,EventChallenges.goals(EventType.METEORY)[0]);
        assertNotEquals(EventChallenges.goals(EventType.ZIMA)[0],
                EventChallenges.goals(EventType.METEORY)[0]);
    }
    @Test void milestonesAwardOnlyOnceEvenAfterRestartOrManyActions() throws Exception{
        UUID player=UUID.randomUUID();
        long now=System.currentTimeMillis();
        var goals=EventChallenges.goals(EventType.METEORY);
        EventState state=new EventState().started(EventType.METEORY,now,now+3600000);
        state=state.challengeActions(player,1,EventType.METEORY,now,goals,List.of(1,2,3));
        assertEquals(1,state.challengePoints(player));
        assertEquals(1,state.challengeAwarded(player));
        assertEquals(1,state.pendingKeys(player));
        state=state.challengeActions(player,2,EventType.METEORY,now,goals,List.of(1,2,3));
        assertEquals(3,state.challengePoints(player));
        assertEquals(3,state.challengeAwarded(player));
        assertEquals(3,state.pendingKeys(player));
        state=state.challengeActions(player,3,EventType.METEORY,now,goals,List.of(1,2,3));
        assertEquals(6,state.challengePoints(player));
        assertEquals(7,state.challengeAwarded(player));
        assertEquals(6,state.pendingKeys(player));
        state=state.challengeActions(player,999,EventType.METEORY,now,goals,List.of(1,2,3));
        assertEquals(6,state.pendingKeys(player));
        state=state.keysDelivered(player,3);
        assertEquals(3,state.pendingKeys(player));
        state=state.ended();
        assertEquals(3,state.pendingKeys(player));
        state=state.started(EventType.ZIMA,now+1,now+7200000);
        assertEquals(3,state.pendingKeys(player));
        assertEquals(0,state.challengePoints(player));
        assertEquals(0,state.challengeAwarded(player));
        state.validate();
    }
    @Test void durableLedgerSurvivesRestartAndOldJsonIsCompatible() throws Exception{
        UUID player=UUID.randomUUID();
        long now=System.currentTimeMillis();
        try(var file=new StateFile<>(dir,"EventState.json",EventState.class,
                EventState::new,EventState::validate)){
            file.update(s->s.started(EventType.METEORY,now,now+3600000)).join();
            file.update(s->s.challengeActions(player,3,EventType.METEORY,now,
                    EventChallenges.goals(EventType.METEORY),List.of(1,2,3))).join();
        }
        try(var file=new StateFile<>(dir,"EventState.json",EventState.class,
                EventState::new,EventState::validate)){
            assertEquals(3,file.get().challengePoints(player));
            assertEquals(3,file.get().pendingKeys(player));
            file.update(s->s.keysDelivered(player,3)).join();
        }
        assertEquals(0,new Gson().fromJson("{\"type\":\"zima\",\"startedAt\":1,"
                +"\"endsAt\":100,\"progress\":{},\"pvpCooldowns\":{}}",
                EventState.class).pendingKeys(player));
    }
    @Test void badChallengesDoNotGrantMoreKeys(){
        long now=System.currentTimeMillis();
        UUID id=UUID.randomUUID();
        EventState state=new EventState().started(EventType.METEORY,now,now+100000);
        EventState ignored=state.challengeActions(id,1,EventType.METEORY,now+1,
                EventChallenges.goals(EventType.METEORY),List.of(1,2,3));
        assertEquals(state,ignored);
        assertThrows(IllegalArgumentException.class,()->state.keysDelivered(id,1));
        EventConfig c=new EventConfig();c.validate();
        assertEquals(List.of(1,2,3),c.challengeKeyRewards());
        assertThrows(IllegalArgumentException.class,
                ()->new Gson().fromJson("{\"challengeKeyRewards\":[1,5,65]}",
                        EventConfig.class).validate());
    }
}
