package pl.lokos.tools.region;

/**
 * Trzy stany flagi: dziedziczenie (null) -> dozwolone -> zabronione -> dziedziczenie.
 * Jawny typ Boolean eliminuje automatyczne rozpakowywanie null w operatorze ?:.
 */
public final class RegionFlagCycle {
    private RegionFlagCycle() {}

    public static Boolean next(Boolean previous) {
        if (previous == null) return Boolean.TRUE;
        if (previous.booleanValue()) return Boolean.FALSE;
        return null;
    }
}
