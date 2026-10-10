package pl.lokos.tools.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.helpers.StateChanges;

import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

class ChatManagerIdempotencyTest {
    @TempDir Path folder;

    @Test void repeatedToggleIsNotSuccessAndDoesNotRewriteFile() throws Exception {
        try(ChatManager manager=new ChatManager(null,null,folder)){
            Path file=folder.resolve("ChatState.json");
            String original=Files.readString(file);
            Throwable unchanged=assertThrows(CompletionException.class,
                    ()->manager.setEnabled(true).join());
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(unchanged));
            assertEquals(original,Files.readString(file));

            manager.setEnabled(false).join();
            String disabled=Files.readString(file);
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(
                    assertThrows(CompletionException.class,()->manager.setEnabled(false).join())));
            assertEquals(disabled,Files.readString(file));
            manager.setAnnouncements(false).join();
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(
                    assertThrows(CompletionException.class,()->manager.setAnnouncements(false).join())));
            manager.setRank("vip").join();
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(
                    assertThrows(CompletionException.class,()->manager.setRank("vip").join())));
        }
    }

    @Test void aRepeatedMuteIsReportedAsAlreadyMuted() throws Exception {
        UUID player=UUID.randomUUID();
        try(ChatManager manager=new ChatManager(null,null,folder)){
            manager.silence(player,"Tester",0,"spam").join();
            String stored=Files.readString(folder.resolve("ChatState.json"));
            Throwable repeated=assertThrows(CompletionException.class,
                    ()->manager.silence(player,"Tester",System.currentTimeMillis()+30000,"spam").join());
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(repeated));
            assertEquals(stored,Files.readString(folder.resolve("ChatState.json")));
            manager.unsilence(player).join();
            assertInstanceOf(StateChanges.Unchanged.class,StateChanges.root(
                    assertThrows(CompletionException.class,()->manager.unsilence(player).join())));
        }
    }
}
