package pl.lokos.tools.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EventPvPCooldownTest {
    @TempDir Path folder;
    @Test void repeatedVictimCannotBeFarmedAcrossRestart() throws Exception {
        UUID killer=UUID.randomUUID(),victim=UUID.randomUUID();
        long start=1_000_000L,interval=300_000L;
        try(var state=new StateFile<>(folder,"EventState.json",EventState.class,
                EventState::new,EventState::validate)){
            state.update(s->s.started(EventType.ZABOJSTWA,start,start+3_600_000L)).join();
            assertTrue(state.get().canRewardPvPKill(killer,victim,start,interval));
            state.update(s->s.withPvPKill(killer,victim,start,interval)).join();
            assertFalse(state.get().canRewardPvPKill(killer,victim,start+interval-1,interval));
            assertThrows(IllegalArgumentException.class,()->
                    state.get().withPvPKill(killer,victim,start+1,interval));
        }
        try(var state=new StateFile<>(folder,"EventState.json",EventState.class,
                EventState::new,EventState::validate)){
            assertFalse(state.get().canRewardPvPKill(killer,victim,start+interval-1,interval));
            assertTrue(state.get().canRewardPvPKill(killer,victim,start+interval,interval));
            assertTrue(state.get().canRewardPvPKill(victim,killer,start+1,interval));
            state.update(s->s.withPvPKill(killer,victim,start+interval,interval)).join();
            state.update(s->s.progress(killer,13)).join();
            assertEquals(13,state.get().progress(killer));
            assertFalse(state.get().pvpCooldowns().isEmpty());
        }
    }
    @Test void oldJsonWithoutCooldownFieldLoadsAndRetainsProgress() throws Exception{
        UUID killer=UUID.randomUUID();
        Files.writeString(folder.resolve("EventState.json"),
                "{\"type\":\"zabojstwa\",\"startedAt\":100,\"endsAt\":200,\"progress\":{\""+
                killer+"\":7}}");
        try(var state=new StateFile<>(folder,"EventState.json",EventState.class,
                EventState::new,EventState::validate)){
            assertEquals(7,state.get().progress(killer));
            assertTrue(state.get().pvpCooldowns().isEmpty());
        }
    }
    @Test void newRoundClearsPairCooldownsWithoutAffectingStoredOldFile() {
        UUID killer=UUID.randomUUID(),victim=UUID.randomUUID();
        EventState state=new EventState().started(EventType.ZABOJSTWA,100,1000000)
                .withPvPKill(killer,victim,200,300000);
        assertFalse(state.canRewardPvPKill(killer,victim,201,300000));
        assertTrue(state.started(EventType.ZABOJSTWA,2_000_000,3_000_000)
                .canRewardPvPKill(killer,victim,2_000_001,300000));
    }
}
