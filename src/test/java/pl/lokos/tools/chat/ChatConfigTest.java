package pl.lokos.tools.chat;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.lokos.tools.config.JsonConfigManager;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChatConfigTest {
    @TempDir Path folder;
    @Test void createsChatSettingsAndPersistsConfigurationWithNoOverrides() throws Exception {
        JsonConfigManager json=new JsonConfigManager(folder);
        ChatConfig config=json.load("Chat.json",ChatConfig.class,ChatConfig::new,ChatConfig::validate);
        assertEquals(2,config.slowMaxMessages());
        assertEquals(3,config.slowWindowSeconds());
        assertTrue(Files.exists(folder.resolve("Chat.json")));
        assertTrue(Files.readString(folder.resolve("Chat.json")).contains("Testowa automatyczna wiadomość 1"));
        assertEquals(2,config.announcements().size());
        String changed=Files.readString(folder.resolve("Chat.json")).replace(
                "Testowa automatyczna wiadomość 1","Wiadomość administratora");
        Files.writeString(folder.resolve("Chat.json"),changed);
        assertEquals("Wiadomość administratora",json.load("Chat.json",ChatConfig.class,
                ChatConfig::new,ChatConfig::validate).announcements().get(0));
    }

    @Test void createsAndValidatesStateWithoutErasingMutes() throws Exception {
        JsonConfigManager json=new JsonConfigManager(folder);
        ChatStateFile initial=json.load("ChatState.json",ChatStateFile.class,
                ChatStateFile::new,ChatStateFile::validate);
        UUID user=UUID.randomUUID();
        var restricted=initial.withEnabled(false).withMute(user,new ChatStateFile.Mute("Gracz",0,"Spam"));
        Files.writeString(folder.resolve("ChatState.json"),new Gson().toJson(restricted));
        var restored=json.load("ChatState.json",ChatStateFile.class,
                ChatStateFile::new,ChatStateFile::validate);
        assertFalse(restored.enabled());
        assertEquals("Spam",restored.muted().get(user.toString()).reason());
        assertEquals(1,restored.muted().size());
        assertEquals(0,restored.withoutExpired(System.currentTimeMillis()).muted()
                .get(user.toString()).untilMillis());
    }

    @Test void inGameChangesPreserveExistingUnknownJsonFields() throws Exception {
        JsonConfigManager json=new JsonConfigManager(folder);
        json.load("Chat.json",ChatConfig.class,ChatConfig::new,ChatConfig::validate);
        Path path=folder.resolve("Chat.json");
        var object=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        object.addProperty("administratorNote","Zachowaj ten tekst");
        Files.writeString(path,object.toString());
        var changed=ChatConfigEditor.apply(path,data->{
            data.addProperty("announcementIntervalSeconds",45);
            data.addProperty("slowMaxMessages",4);
            data.addProperty("slowWindowSeconds",8);
        });
        assertEquals(45,changed.announcementIntervalSeconds());
        assertEquals(4,changed.slowMaxMessages());
        assertEquals(8,changed.slowWindowSeconds());
        assertTrue(Files.readString(path).contains("Zachowaj ten tekst"));
        String before=Files.readString(path);
        assertThrows(IOException.class,()->ChatConfigEditor.apply(path,data->
                data.addProperty("announcementIntervalSeconds",1)));
        assertEquals(before,Files.readString(path));
    }

    @Test void invalidInputsAreRejected(){
        var bad=new Gson().fromJson("{\"slowWindowSeconds\":0}",ChatConfig.class);
        assertThrows(IllegalArgumentException.class,bad::validate);
        assertThrows(IllegalArgumentException.class,()->ChatDuration.until("0m",1000));
        assertThrows(IllegalArgumentException.class,()->ChatDuration.until("999999d",1000));
        assertEquals(0,ChatDuration.until("*",1000));
        assertEquals(301000,ChatDuration.until("5m",1000));
    }
}
