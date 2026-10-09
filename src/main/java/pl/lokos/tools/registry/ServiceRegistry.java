package pl.lokos.tools.registry;

import java.util.*;

/**
 * Lekki rejestr usług i jawne constructor injection bez refleksji/Guice.
 * Zwraca usługę tylko, jeśli została już zarejestrowana i zweryfikowana.
 */
public final class ServiceRegistry implements AutoCloseable {
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();
    private boolean closed;

    public synchronized <T> T register(Class<T> type, T service) {
        if (closed) throw new IllegalStateException("Rejestr został zamknięty.");
        Objects.requireNonNull(type);
        Objects.requireNonNull(service);
        if (!type.isInstance(service)) throw new IllegalArgumentException("Niezgodny typ usługi.");
        if (services.putIfAbsent(type, service) != null)
            throw new IllegalStateException("Usługa została już zarejestrowana: " + type.getName());
        return service;
    }

    public synchronized <T> T require(Class<T> type) {
        Object service = services.get(type);
        if (service == null) throw new IllegalStateException("Brak usługi: " + type.getName());
        return type.cast(service);
    }

    public synchronized <T> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(services.get(type)).map(type::cast);
    }

    @Override public synchronized void close() {
        closed = true;
        services.clear();
    }
}
