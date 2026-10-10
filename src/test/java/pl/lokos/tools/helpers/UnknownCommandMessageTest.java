package pl.lokos.tools.helpers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnknownCommandMessageTest {
    @Test void commandErrorKeepsConcisePolishText(){
        String text=net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(Messages.unknownComponent());
        assertEquals("✘ » Nieznana komenda.",text);
    }
}
