package com.businessprocess.core.registry;

import com.businessprocess.core.delegate.TaskDelegate;
import org.springframework.stereotype.Component;

import java.nio.file.ProviderNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderRegistry {
    private final Map<String, TaskDelegate> registry = new ConcurrentHashMap<>();

    public void register(String name, TaskDelegate provider) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        registry.put(name, provider);
    }

    public TaskDelegate getProvider(String name) {
        TaskDelegate provider = registry.get(name);
        if (provider == null) {
            throw new ProviderNotFoundException("No provider registered for: " + name);
        }
        return provider;
    }

    public boolean hasProvider(String name) {
        return registry.containsKey(name);
    }
}
