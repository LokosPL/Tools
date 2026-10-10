package pl.lokos.tools.combat;

import org.bukkit.event.entity.EntityDamageEvent;
import java.util.*;

/** Deterministyczne reguły; testy nie wymagają serwera. */
public final class CombatRules {
    private CombatRules(){}
    public static boolean shouldTag(boolean cancelled,boolean positiveDamage,boolean other,
                                    boolean attackerLiving,boolean attackerPlayer,
                                    boolean playerTags,boolean mobTags) {
        return !cancelled&&positiveDamage&&other&&attackerLiving&&
                (attackerPlayer?playerTags:mobTags);
    }
    /**
     * Atak na pasywne zwierzę nie oznacza wejścia w walkę.
     * W walce PvP i z wrogimi mobami tag obowiązuje obie strony.
     */
    public static boolean shouldTagAttacker(boolean targetPlayer,boolean hostileMob){
        return targetPlayer||hostileMob;
    }
    public static String reason(EntityDamageEvent.DamageCause cause){
        if(cause==null)return "zginął.";
        return switch(cause){
            case LAVA -> "próbował pływać w lawie!";
            case FIRE,FIRE_TICK -> "spłonął!";
            case FALL -> "nie przeżył upadku!";
            case VOID -> "wpadł w pustkę!";
            case DROWNING -> "zapomniał, jak się oddycha!";
            case SUFFOCATION -> "utknął w ścianie!";
            case ENTITY_ATTACK,ENTITY_SWEEP_ATTACK,PROJECTILE -> "poległ w walce!";
            case BLOCK_EXPLOSION,ENTITY_EXPLOSION -> "nie przetrwał eksplozji!";
            case LIGHTNING -> "został trafiony piorunem!";
            case HOT_FLOOR -> "stanął na rozgrzanej powierzchni!";
            case STARVATION -> "umarł z głodu!";
            case POISON,WITHER,MAGIC -> "nie wytrzymał obrażeń magicznych!";
            case FLY_INTO_WALL -> "uderzył w ścianę!";
            case CONTACT -> "natknął się na niebezpieczną roślinę!";
            default -> "zginął.";
        };
    }
}
