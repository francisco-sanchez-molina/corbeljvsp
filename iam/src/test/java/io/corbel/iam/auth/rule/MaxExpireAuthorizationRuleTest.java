package io.corbel.iam.auth.rule;

import io.corbel.iam.auth.AuthorizationRequestContext;
import io.corbel.iam.exception.UnauthorizedException;
import io.corbel.iam.model.Domain;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.Collections;

import static org.mockito.Mockito.*;


/**
 * @author Alexander De Leon
 * 
 */
public class MaxExpireAuthorizationRuleTest {

    private static final int TEST_MAX_EXPIRATION = 36000000;

    private MaxExpireAuthorizationRule rule;
    private AuthorizationRequestContext contextMock;
    private Domain domainMock;

    @Before
    public void setup() {
        domainMock = mock(Domain.class);
        contextMock = mock(AuthorizationRequestContext.class);
        when(contextMock.getRequestedDomain()).thenReturn(domainMock);
        rule = new MaxExpireAuthorizationRule(TEST_MAX_EXPIRATION);
    }

    @Test
    public void testOk() throws UnauthorizedException {
        when(contextMock.getAuthorizationExpiration()).thenReturn(System.currentTimeMillis() + TEST_MAX_EXPIRATION);
        rule.process(contextMock);
    }

    @Test(expected = UnauthorizedException.class)
    public void testUnauthorized() throws UnauthorizedException {
        when(contextMock.getAuthorizationExpiration()).thenReturn(System.currentTimeMillis() + (TEST_MAX_EXPIRATION * 2));
        when(domainMock.getCapabilities()).thenReturn(Collections.singletonMap("allowTokensWithInvalidExpirationTimes", false));
        rule.process(contextMock);
    }

    @Test
    public void testWrongExpirationTimeWithoutCapabilityDefined() throws UnauthorizedException {
        when(contextMock.getAuthorizationExpiration()).thenReturn(System.currentTimeMillis() + (TEST_MAX_EXPIRATION * 2));
        when(domainMock.getCapabilities()).thenReturn(Collections.emptyMap());
        rule.process(contextMock);
        verify(contextMock).setAuthorizationExpiration(anyLong());
    }

    @Test
    public void testWrongExpirationTimeWithCapabilityDefined() throws UnauthorizedException {
        when(contextMock.getAuthorizationExpiration()).thenReturn(System.currentTimeMillis() + (TEST_MAX_EXPIRATION * 2));
        when(domainMock.getCapabilities()).thenReturn(Collections.singletonMap("allowTokensWithInvalidExpirationTimes", true));
        rule.process(contextMock);
        verify(contextMock).setAuthorizationExpiration(anyLong());
    }

}
