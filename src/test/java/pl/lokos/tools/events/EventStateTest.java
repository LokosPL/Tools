package pl.lokos.tools.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EventStateTest {
    @TempDir Path directory;
    @Test void onlyOneTypeInStateAndRestartPreservesProgress() throws Exception{
        var file=new StateFile<>(directory,"EventState.json",EventState.class,
                EventState::new,EventState::validate);
        UUID uuid=UUID.randomUUID();
        long now=System.currentTimeMillis();
        file.update(s->s.started(EventType.ZIMA,now,now+3600000)).join();
        file.update(s->s.progress(uuid,9)).join();
        assertEquals(EventType.ZIMA,file.get().active(now));
        assertEquals(9,file.get().progress(uuid));
        file.close();
        try(var loaded=new StateFile<>(directory,"EventState.json",EventState.class,
                EventState::new,EventState::validate)){
            assertEquals(EventType.ZIMA,loaded.get().active(now));
            assertEquals(9,loaded.get().progress(uuid));
            assertNull(loaded.get().active(now+3600000));
            loaded.update(EventState::ended).join();
            assertNull(loaded.get().active(now));
            loaded.update(s->s.started(EventType.HALLOWEEN,now+100,now+7200000)).join();
            assertEquals(EventType.HALLOWEEN,loaded.get().active(now+200));
        }
        assertEquals(8,EventType.names().size());
        assertEquals("zima",EventType.ZIMA.id());
        assertNull(EventType.parse("nieistniejący"));
    }
    @Test void eventConfigurationRejectsUnsafeValues(){
        EventConfig config=new EventConfig();
        config.validate();
        assertEquals(0.02,config.tokenChance(),0.00001);
        assertEquals(10,config.tokensForKey());
        var gson=new com.google.gson.Gson();
        assertThrows(IllegalArgumentException.class,()->
                gson.fromJson("{\"tokenChance\":1}",EventConfig.class).validate());
    }
}
