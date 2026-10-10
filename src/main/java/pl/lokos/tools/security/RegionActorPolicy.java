package pl.lokos.tools.security;

/** Akcja gracza może ominąć flagę tylko, gdy posiada faktyczne zarządzanie regionami. */
public final class RegionActorPolicy {
    private RegionActorPolicy() {}
    public static boolean denied(boolean canManageRegion, boolean blockedByFlag) {
        return !canManageRegion && blockedByFlag;
    }
}
