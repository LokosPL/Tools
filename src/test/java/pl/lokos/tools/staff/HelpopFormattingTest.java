package pl.lokos.tools.staff;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.commands.StaffCommand;
import static org.junit.jupiter.api.Assertions.*;

class HelpopFormattingTest {
    @Test void untrustedColorsRemainLiteralInHelpop(){
        String body="&cAdministrator &#FF0000 Test";
        var rendered=StaffCommand.safeHelpop("&#FFD166✦ &7{player}: &f{message}","Steve",body);
        String plain=PlainTextComponentSerializer.plainText().serialize(rendered);
        assertTrue(plain.contains(body));
        assertTrue(plain.startsWith("✦ Steve: "));
    }
}
