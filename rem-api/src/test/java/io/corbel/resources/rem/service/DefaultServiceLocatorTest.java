package io.corbel.resources.rem.service;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.fest.assertions.api.Assertions.*;

/**
 * @author Alexander De Leon (alex.deleon@devialab.com)
 */
public class DefaultServiceLocatorTest {

    private ServiceLocator serviceLocator;

    @Before
    public void setup() {
        serviceLocator = new DefaultServiceLocator();
    }

    @Test
    public void testPublishResolve() {
        TestService service = new TestService() {};
        serviceLocator.publish(TestService.class, service);
        assertThat(serviceLocator.resolve(TestService.class)).isEqualTo(Optional.of(service));
    }

    @Test(expected = IllegalStateException.class)
    public void testErrorWhenAlreadyPublished() {
        TestService service = new TestService() {};
        serviceLocator.publish(TestService.class, service);
        serviceLocator.publish(TestService.class, service);
    }

    interface TestService {}

}