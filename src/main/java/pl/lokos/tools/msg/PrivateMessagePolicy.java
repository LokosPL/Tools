package pl.lokos.tools.msg;

import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.security.ToolsAccess;
import java.util.*;

/** Czysta, jednostkowo testowalna autoryzacja prywatnych wiadomości. */
public final class PrivateMessagePolicy {
    public enum Result { ALLOW, SELF, DISABLED_GLOBALLY, DISABLED_SENDER,
        DISABLED_TARGET, IGNORED, STAFF_PROTECTED, MUTED, COOLDOWN }
    private PrivateMessagePolicy(){}

    public static boolean isStaff(RankSnapshot snapshot,UUID player,boolean op,
                                   Set<String> administrativeNodes){
        if(op)return true;
        for(String node:administrativeNodes)
            if(ToolsAccess.allowed(false,snapshot,player,node,true))return true;
        return ToolsAccess.allowed(false,snapshot,player,"tools.msg.protected",false);
    }

    public static Result check(PrivateMessageConfig config,PrivateMessageState state,
                               UUID sender,UUID receiver,boolean senderCanContactStaff,
                               boolean receiverStaff,boolean senderMuted,long now,long lastSent) {
        if(sender.equals(receiver))return Result.SELF;
        if(!config.enabled())return Result.DISABLED_GLOBALLY;
        if(state.disabled(sender))return Result.DISABLED_SENDER;
        if(state.disabled(receiver))return Result.DISABLED_TARGET;
        if(state.ignores(receiver,sender))return Result.IGNORED;
        if(config.protectAdminRanks() && receiverStaff && !senderCanContactStaff)
            return Result.STAFF_PROTECTED;
        if(senderMuted)return Result.MUTED;
        if(config.cooldownMillis()>0 && lastSent>0 && now-lastSent<config.cooldownMillis())
            return Result.COOLDOWN;
        return Result.ALLOW;
    }
}
