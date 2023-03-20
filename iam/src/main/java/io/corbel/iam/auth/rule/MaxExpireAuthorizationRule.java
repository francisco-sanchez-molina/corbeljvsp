package io.corbel.iam.auth.rule;

import io.corbel.iam.auth.AuthorizationRequestContext;
import io.corbel.iam.auth.AuthorizationRule;
import io.corbel.iam.exception.UnauthorizedException;
import io.corbel.iam.exception.UnauthorizedTimeException;

/**
 * Ensures that the requested expiration time does not exceeds the maximum allowed expiration.
 * 
 * @author Alexander De Leon
 * 
 */
public class MaxExpireAuthorizationRule implements AuthorizationRule {

    private final int maxExpirationInMillis;

    public MaxExpireAuthorizationRule(int maxExpirationInMillis) {
        this.maxExpirationInMillis = maxExpirationInMillis;
    }

    @Override
    public void process(AuthorizationRequestContext context) throws UnauthorizedException {
        long currentTime = System.currentTimeMillis();
        long maxExpirationTokenInMillis = currentTime + maxExpirationInMillis;
        if (context.getAuthorizationExpiration() > maxExpirationTokenInMillis) {
            if(context.getRequestedDomain().getCapabilities().getOrDefault("allowTokensWithInvalidExpirationTimes", true)) {
                context.setAuthorizationExpiration(maxExpirationTokenInMillis);
            } else {
                throw new UnauthorizedTimeException("Authorization request exceds maximum expiration. " +
                        "Maximum is " + maxExpirationInMillis + " milliseconds since its issued time");
            }
        }
    }
}
