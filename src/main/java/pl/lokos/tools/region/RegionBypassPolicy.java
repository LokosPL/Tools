package pl.lokos.tools.region;

/** Ominięcie ochrony wymaga zarówno autoryzacji, jak i aktywnego przełącznika. */
public final class RegionBypassPolicy {
    private RegionBypassPolicy() {}
    public static boolean active(boolean authorized,boolean explicitToggle) {
        return authorized && explicitToggle;
    }
}