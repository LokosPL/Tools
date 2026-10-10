package pl.lokos.tools.msg;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrivateMessageAliasTest {
    @Test void vanillaNamespacedWhispersCannotBypassToolsPolicies(){
        assertEquals("/msg Steve cześć",PrivateMessageListener.canonical("/minecraft:msg Steve cześć"));
        assertEquals("/msg Steve test",PrivateMessageListener.canonical("/minecraft:tell Steve test"));
        assertEquals("/msg Steve test",PrivateMessageListener.canonical("/minecraft:w Steve test"));
        assertEquals("/msg Steve test",PrivateMessageListener.canonical("/tell Steve test"));
        assertEquals("/msg Steve test",PrivateMessageListener.canonical("/w Steve test"));
        assertEquals("/msg",PrivateMessageListener.canonical("/tell"));
    }
    @Test void otherCommandsAndReplyRemainUntouched(){
        assertEquals("/minecraft:op Steve",PrivateMessageListener.canonical("/minecraft:op Steve"));
        assertEquals("/reply hej",PrivateMessageListener.canonical("/reply hej"));
        assertEquals("/r hej",PrivateMessageListener.canonical("/r hej"));
        assertEquals("/msg Steve hej",PrivateMessageListener.canonical("/msg Steve hej"));
    }
}
