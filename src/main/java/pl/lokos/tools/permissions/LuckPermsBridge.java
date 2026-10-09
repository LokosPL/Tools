package pl.lokos.tools.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Dobrowolna integracja z API LuckPerms — nie zastępuje autorskich rang Tools
 * i nigdy nie nadpisuje danych grup bez żądania administratora.
 * Zapis użytkownika wykonywany asynchronicznie przez LuckPerms UserManager.
 */
public final class LuckPermsBridge {
    private final LuckPerms api;

    private LuckPermsBridge(LuckPerms api) { this.api = api; }

    public static Optional<LuckPermsBridge> discover() {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return Optional.empty();
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        return provider == null ? Optional.empty() : Optional.of(new LuckPermsBridge(provider.getProvider()));
    }

    public CompletableFuture<Void> grantPermission(UUID uuid, String permission, Long expiresAt, String world) {
        if (permission == null || !permission.matches("[a-zA-Z0-9_.*:-]{1,128}"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Niepoprawne uprawnienie."));
        PermissionNode.Builder node = PermissionNode.builder(permission);
        applyContextAndExpiry(node, expiresAt, world);
        return api.getUserManager().modifyUser(uuid, user -> user.data().add(node.build()))
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> inheritGroup(UUID uuid, String group, Long expiresAt, String world) {
        if (group == null || !group.matches("[a-zA-Z0-9_-]{1,64}"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Niepoprawna nazwa grupy."));
        // LuckPerms przechowuje relację dziedziczenia jako InheritanceNode.
        InheritanceNode.Builder node = InheritanceNode.builder(group);
        applyContextAndExpiry(node, expiresAt, world);
        return api.getUserManager().modifyUser(uuid, user -> user.data().add(node.build()))
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Boolean> hasPermission(UUID uuid, String permission, String world) {
        return api.getUserManager().loadUser(uuid).thenApply(user -> {
            if (world == null) {
                return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            }
            ImmutableContextSet set = ImmutableContextSet.builder().add("world", world).build();
            return user.getCachedData().getPermissionData(QueryOptions.contextual(set))
                    .checkPermission(permission).asBoolean();
        });
    }

    private static void applyContextAndExpiry(NodeBuilder<?, ?> node, Long expiresAt, String world) {
        if (expiresAt != null) {
            if (expiresAt <= System.currentTimeMillis())
                throw new IllegalArgumentException("Czas ważności już upłynął.");
            node.expiry(Instant.ofEpochMilli(expiresAt));
        }
        if (world != null && !world.isBlank()) {
            if (!world.matches("[a-zA-Z0-9_-]{1,80}"))
                throw new IllegalArgumentException("Niepoprawna nazwa świata.");
            node.withContext("world", world);
        }
    }
}
