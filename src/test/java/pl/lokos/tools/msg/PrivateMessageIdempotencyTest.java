package pl.lokos.tools.msg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.helpers.StateChanges;

import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

class PrivateMessageIdempotencyTest {
    @TempDir Path folder;

    @Test void repeatingEnableDoesNotSaveOrReportSuccess() throws Exception {
        UUID owner=UUID.randomUUID();
        try(PrivateMessageManager manager=new PrivateMessageManager(null,folder)){
            Path file=folder.resolve("PrivateMessagesState.json");
            String before=Files.readString(file);
            Throwable error=assertThrows(CompletionException.class,
                    ()->manager.disable(owner,false).join());
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(error));
            assertEquals("Prywatne wiadomości są już włączone.",StateChanges.root(error).getMessage());
            assertEquals(before,Files.readString(file));

            manager.disable(owner,true).join();
            String disabled=Files.readString(file);
            assertTrue(manager.state().disabled(owner));
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(
                    assertThrows(CompletionException.class,()->manager.disable(owner,true).join())));
            assertEquals(disabled,Files.readString(file));
            manager.disable(owner,false).join();
            assertFalse(manager.state().disabled(owner));
        }
    }

    @Test void ignoreCanBeAppliedOnlyOnceUntilUnmuted() throws Exception {
        UUID owner=UUID.randomUUID(),other=UUID.randomUUID();
        try(PrivateMessageManager manager=new PrivateMessageManager(null,folder)){
            manager.ignore(owner,other,"Tester",true).join();
            String current=Files.readString(folder.resolve("PrivateMessagesState.json"));
            Throwable repeated=assertThrows(CompletionException.class,
                    ()->manager.ignore(owner,other,"Tester",true).join());
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(repeated));
            assertEquals(current,Files.readString(folder.resolve("PrivateMessagesState.json")));
            manager.ignore(owner,other,"Tester",false).join();
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(
                    assertThrows(CompletionException.class,
                            ()->manager.ignore(owner,other,"Tester",false).join())));
        }
    }
}
