package pl.lokos.tools.region;

import java.util.*;

/** Wspolna logika pierwszenstwa podregionow i dziedziczenia flag. Bez Bukkit API. */
public final class RegionIndex {
    private final Map<String, Region> regions;
    private final Map<UUID, List<Region>> worlds;

    public RegionIndex(Collection<Region> all) {
        Map<String, Region> byName = new HashMap<>();
        for (Region region : all) {
            if (byName.putIfAbsent(region.name(), region) != null)
                throw new IllegalArgumentException("Powtórzona nazwa regionu.");
        }
        Map<UUID, List<Region>> byWorld = new HashMap<>();
        for (Region region : all) {
            if (region.parent() != null) {
                Region parent = byName.get(region.parent());
                if (parent == null || !parent.encloses(region) || region.equals(parent))
                    throw new IllegalArgumentException("Nieprawidłowy rodzic regionu " + region.name());
            }
            byWorld.computeIfAbsent(region.world(), unused -> new ArrayList<>()).add(region);
        }
        for (List<Region> list : byWorld.values()) {
            list.sort(Comparator.comparingInt((Region r) -> depth(r, byName)).reversed()
                    .thenComparingLong(Region::area).thenComparing(Region::name));
        }
        this.regions = Map.copyOf(byName);
        Map<UUID, List<Region>> temp = new HashMap<>();
        byWorld.forEach((uuid, list) -> temp.put(uuid, List.copyOf(list)));
        this.worlds = Map.copyOf(temp);
    }

    private static int depth(Region r, Map<String, Region> all) {
        int depth = 0;
        Set<String> seen = new HashSet<>();
        while (r.parent() != null) {
            if (!seen.add(r.name()) || depth++ > 32)
                throw new IllegalArgumentException("Cykliczna hierarchia regionów.");
            r = Objects.requireNonNull(all.get(r.parent()), "Nie ma nadrzędnego regionu.");
        }
        return depth;
    }

    public static RegionIndex empty() { return new RegionIndex(List.of()); }
    public Map<String, Region> all() { return regions; }
    public Region byName(String name) { return regions.get(name); }

    public Region at(UUID world, int x, int z) {
        for (Region r : worlds.getOrDefault(world, List.of()))
            if (r.contains(world, x, z)) return r;
        return null;
    }

    public boolean enabled(Region region, RegionFlag flag) {
        Region original=region;
        Set<String> seen = new HashSet<>();
        while (region != null) {
            if (!seen.add(region.name())) return false;
            Boolean setting = region.flags().get(flag);
            if (setting != null) return setting;
            region = region.parent() == null ? null : regions.get(region.parent());
        }
        // Starsza flaga interakcje pozostaje domyslna dla nowych, szczegolowych typow.
        if(flag.category().equals("Interakcje") && flag!=RegionFlag.INTERACT)
            return enabled(original,RegionFlag.INTERACT);
        return false;
    }

    public String requiredRank(Region region) {
        while (region != null) {
            if (region.entryRank() != null) return region.entryRank();
            region = region.parent() == null ? null : regions.get(region.parent());
        }
        return null;
    }

    public void validateNew(Region candidate) {
        if (regions.containsKey(candidate.name())) throw new IllegalArgumentException("Region już istnieje.");
        if (candidate.parent() != null) {
            Region parent = regions.get(candidate.parent());
            if (parent == null || !parent.encloses(candidate))
                throw new IllegalArgumentException("Podregion musi mieścić się w regionie nadrzędnym.");
        } else {
            for (Region existing : regions.values()) {
                if (existing.parent() == null && existing.overlaps(candidate))
                    throw new IllegalArgumentException("Nowy region nachodzi na istniejący region " + existing.name());
            }
        }
    }
}
