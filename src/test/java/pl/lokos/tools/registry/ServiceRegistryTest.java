package pl.lokos.tools.registry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServiceRegistryTest {
    @Test void refusesDuplicatesAndAllowsOptionalLookup() {
        try (ServiceRegistry registry = new ServiceRegistry()) {
            registry.register(String.class, "test");
            assertEquals("test", registry.require(String.class));
            assertTrue(registry.find(Integer.class).isEmpty());
            assertThrows(IllegalStateException.class, () -> registry.register(String.class, "again"));
        }
    }
    @Test void refusesWritesAfterShutdown() {
        ServiceRegistry registry = new ServiceRegistry();
        registry.close();
        assertThrows(IllegalStateException.class, () -> registry.register(Integer.class, 1));
    }
}
