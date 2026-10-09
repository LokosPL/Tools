package pl.lokos.tools.region;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Niezmienny prostopadloscian XZ rozciagniety na cala wysokosc swiata. */
public record Region(String name, UUID world, int minX, int maxX, int minZ, int maxZ,
                     String parent, String entryRank, Map<RegionFlag, Boolean> flags, Spawn spawn) {
    public Region {
        Objects.requireNonNull(name);
        Objects.requireNonNull(world);
        flags = Map.copyOf(flags);
        if (minX > maxX || minZ > maxZ) throw new IllegalArgumentException("Błędne granice regionu.");
    }

    public record Spawn(double x, double y, double z, float yaw, float pitch) { }

    public boolean contains(UUID worldId, int x, int z) {
        return world.equals(worldId) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean encloses(Region inner) {
        return world.equals(inner.world) && minX <= inner.minX && maxX >= inner.maxX
                && minZ <= inner.minZ && maxZ >= inner.maxZ;
    }

    public boolean overlaps(Region other) {
        return world.equals(other.world) && minX <= other.maxX && maxX >= other.minX
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public long area() {
        return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
    }
}
