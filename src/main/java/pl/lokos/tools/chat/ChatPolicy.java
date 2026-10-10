package pl.lokos.tools.chat;

import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.security.ToolsAccess;
import java.util.UUID;

/** Czysta autoryzacja czatu: również podczas AsyncChatEvent, bez dostępu do Bukkit API. */
public final class ChatPolicy {
    public enum Decision { ALLOW, MUTED, CLOSED, RANK, SLOW }
    private ChatPolicy(){}

    public static Decision evaluate(ChatStateFile state,ChatConfig config,
                                    RankSnapshot ranks,UUID uuid,boolean operator,long now,
                                    SlowModeLimiter limiter) {
        var mute=state.muted().get(uuid.toString());
        if(mute!=null && mute.active(now) && !has(operator,ranks,uuid,"tools.chat.bypass.mute"))
            return Decision.MUTED;

        boolean bypassLock=has(operator,ranks,uuid,"tools.chat.bypass.lock");
        if(!state.enabled() && !bypassLock)return Decision.CLOSED;
        if(state.minimumRank()!=null && !bypassLock) {
            RankSnapshot.Rank minimum=ranks.ranks().get(state.minimumRank());
            RankSnapshot.Rank current=ranks.forPlayer(uuid);
            if(minimum==null || minimum.position()==null || current==null
                    || current.position()==null || current.position()>minimum.position())
                return Decision.RANK;
        }
        if(!has(operator,ranks,uuid,"tools.chat.bypass.slow")
                && !limiter.allow(uuid,now,config.slowWindowSeconds(),config.slowMaxMessages()))
            return Decision.SLOW;
        return Decision.ALLOW;
    }

    private static boolean has(boolean op,RankSnapshot snapshot,UUID uuid,String node){
        return ToolsAccess.allowed(op,snapshot,uuid,node,false);
    }
}
