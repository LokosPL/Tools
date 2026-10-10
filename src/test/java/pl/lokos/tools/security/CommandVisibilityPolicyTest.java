package pl.lokos.tools.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandVisibilityPolicyTest {
    @Test void technicalNamespacedCommandsStayInvisibleToOrdinaryPlayers() {
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("bukkit:about"));
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("bukkit:plugins"));
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("minecraft:whitelist"));
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("bukkit:help"));
        for (String root : new String[]{
                "?","help","icanhasbukkit",
                "bukkit:?","bukkit:help","bukkit:icanhasbukkit",
                "minecraft:help","paper:help","HELP","IcanHasBukkit"
        }) {
            assertTrue(CommandVisibilityPolicy.hideFromUnprivileged(root),root+" should be hidden from TAB");
            assertTrue(CommandVisibilityPolicy.nativeAdministrative(root),root+" must be blocked on execution");
        }
        assertFalse(CommandVisibilityPolicy.hideFromUnprivileged("msg"));
        assertFalse(CommandVisibilityPolicy.hideFromUnprivileged("reply"));
        assertFalse(CommandVisibilityPolicy.hideFromUnprivileged("r"));
        assertFalse(CommandVisibilityPolicy.hideFromUnprivileged("spawn"));
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("plugins"));
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("reload"));
        assertFalse(CommandVisibilityPolicy.nativeAdministrative("spawn"));
        assertFalse(CommandVisibilityPolicy.nativeAdministrative("lokalizacje"));
        assertFalse(CommandVisibilityPolicy.nativeAdministrative("minecraft:msg"));
        assertTrue(CommandVisibilityPolicy.nativeAdministrative("minecraft:tp"));
        assertTrue(CommandVisibilityPolicy.nativeAdministrative("gamemode"));
        assertTrue(CommandVisibilityPolicy.hideFromUnprivileged("minecraft:msg"));
    }
}
