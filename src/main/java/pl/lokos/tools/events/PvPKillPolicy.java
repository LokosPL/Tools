package pl.lokos.tools.events;

import java.util.UUID;

/** Warunki naliczenia zabójstwa PvP, niezależne od API serwera. */
public final class PvPKillPolicy {
    private PvPKillPolicy(){}
    public static boolean eligible(UUID killer,UUID victim,boolean killerSurvival,
                                   boolean victimSurvival,boolean killerProtected,
                                   boolean victimProtected){
        return killer!=null&&victim!=null&&!killer.equals(victim)
                &&killerSurvival&&victimSurvival&&!killerProtected&&!victimProtected;
    }
}
