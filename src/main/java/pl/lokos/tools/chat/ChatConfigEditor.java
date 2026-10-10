package pl.lokos.tools.chat;

import com.google.gson.*;
import pl.lokos.tools.helpers.StateChanges;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;

/** Transakcyjna edycja ustawień z walidacją przed atomowym zapisem JSON. */
public final class ChatConfigEditor {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private ChatConfigEditor(){}

    public static ChatConfig apply(Path file,Consumer<JsonObject> edit) throws IOException {
        JsonObject object;
        try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){
            object=JsonParser.parseReader(reader).getAsJsonObject();
        }catch(RuntimeException failure){
            throw new IOException("Nieprawidłowy Chat.json.",failure);
        }
        JsonObject before=object.deepCopy();
        edit.accept(object);
        StateChanges.requireChange(object.equals(before),
                "Te ustawienia czatu mają już podane wartości.");
        ChatConfig updated;
        try {
            updated=GSON.fromJson(object,ChatConfig.class);
            updated.validate();
        }catch(RuntimeException failure){
            throw new IOException("Zmiana Chat.json jest niepoprawna: "+failure.getMessage(),failure);
        }
        Path temporary=Files.createTempFile(file.getParent(),".tools-chat-config-",".tmp");
        try {
            Files.writeString(temporary,GSON.toJson(object)+"\n",StandardCharsets.UTF_8);
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){
                Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{Files.deleteIfExists(temporary);}
        return updated;
    }
}
