package io.corbel.resources.rem.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alexander De Leon (alex.deleon@devialab.com)
 */
public class DefaultServiceLocator implements ServiceLocator {

    private Map<Class<?>, Object> registry = new ConcurrentHashMap<>();

    @Override
    public <T> void publish(Class<T> publishClass, T service) {
        if(registry.containsKey(publishClass)){
            throw new IllegalStateException("Service of type "+publishClass+" is already published in service locator");
        }
        registry.put(publishClass, service);
    }

    @Override
    public <T> Optional<T> resolve(Class<T> type) {
        return Optional.ofNullable((T) registry.get(type));
    }

}
