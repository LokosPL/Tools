package pl.lokos.tools.msg;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;

import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PrivateMessageStateTest {
    @TempDir Path folder;

    @Test void defaultSettingsAndPersistenceSurviveRestart() throws Exception {
        var json=new JsonConfigManager(folder);
        var config=json.load("PrivateMessages.json",PrivateMessageConfig.class,
                PrivateMessageConfig::new,PrivateMessageConfig::validate);
        assertEquals(1500,config.cooldownMillis());
        assertTrue(config.protectAdminRanks());
        assertEquals(240,config.maxLength());
        UUID owner=UUID.randomUUID(),target=UUID.randomUUID();
        var original=json.load("PrivateMessagesState.json",PrivateMessageState.class,
                PrivateMessageState::new,PrivateMessageState::validate);
        var changed=original.withDisabled(owner,true)
                .withIgnore(owner,target,"Steve",true);
        Files.writeString(folder.resolve("PrivateMessagesState.json"),new Gson().toJson(changed));
        var restored=json.load("PrivateMessagesState.json",PrivateMessageState.class,
                PrivateMessageState::new,PrivateMessageState::validate);
        assertTrue(restored.disabled(owner));
        assertTrue(restored.ignores(owner,target));
        assertEquals("Steve",restored.ignoredBy(owner).get(target.toString()));
        assertFalse(restored.ignores(target,owner));
        assertFalse(restored.withIgnore(owner,target,"Steve",false).ignores(owner,target));
        assertFalse(restored.withDisabled(owner,false).disabled(owner));
    }

    @Test void invalidConfigIsRejected(){
        PrivateMessageConfig invalid=new Gson().fromJson("{\"cooldownMillis\":-1}",PrivateMessageConfig.class);
        assertThrows(IllegalArgumentException.class,invalid::validate);
        PrivateMessageConfig invalidFormat=new Gson().fromJson(
                "{\"senderFormat\":\"tekst\"}",PrivateMessageConfig.class);
        assertThrows(IllegalArgumentException.class,invalidFormat::validate);
    }
}
