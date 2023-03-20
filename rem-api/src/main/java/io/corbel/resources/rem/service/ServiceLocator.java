package io.corbel.resources.rem.service;

import java.util.Optional;

/**
 * This basic service locator can be used for REM plugins to publish services that other REM plugins can use.
 * -- Use with care!
 *
 * @author Alexander De Leon (alex.deleon@devialab.com)
 */
public interface ServiceLocator {

    /**
     * Publish a service. Make sure you use the most generic interface as the publishClass
     * @param service The service instance
     * @param publishClass The interface (or class) under which the service is published
     * @param <T> The type of the service
     * @throws IllegalStateException if there is already a service of the same type published on the service locator
     */
    <T> void publish(Class<T> publishClass, T service);

    /**
     * Get an instance of the service of the specified type
     * @param type The type of the service
     * @param <T>
     * @return an Optional instance of the service
     */
    <T> Optional<T> resolve(Class<T> type);

}
