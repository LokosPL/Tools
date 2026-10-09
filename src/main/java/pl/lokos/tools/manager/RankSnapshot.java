package pl.lokos.tools.manager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Niezmienny odczyt dla chatu async i systemu TAB. Widok jest podmieniany
 * atomowo, bez ryzyka odczytu map edytowanych przez inne watki.
 */
public record RankSnapshot(
        Map<String, Rank> ranks,
        Map<String, Set<String>> permissions,
        Map<UUID, Grant> grants,
        Map<UUID, Boolean> opRestores) {

    public record Rank(String name, String prefix, String suffix, Integer position, String joinMessage) {
        public boolean assignable() { return position != null; }
    }

    public record Grant(String rank, Long expiresAt) {
        public boolean active(long now) { return expiresAt == null || expiresAt > now; }
    }

    public static RankSnapshot empty() {
        return new RankSnapshot(Map.of(), Map.of(), Map.of(), Map.of());
    }

    public Rank forPlayer(UUID uuid) {
        Grant grant = grants.get(uuid);
        Rank assigned=grant == null || !grant.active(System.currentTimeMillis())
                ? null : ranks.get(grant.rank());
        return assigned!=null ? assigned : ranks.get("gracz");
    }

    public Set<String> permissionsFor(UUID uuid) {
        Rank rank = forPlayer(uuid);
        return rank == null ? permissions.getOrDefault("gracz",Set.of()) : permissions.getOrDefault(rank.name(), Set.of());
    }
}
