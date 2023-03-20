package io.corbel.iam.service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;

import io.corbel.iam.model.Scope;
import io.corbel.iam.model.User;
import io.corbel.iam.repository.ClientRepository;
import io.corbel.lib.token.TokenInfo;
import io.corbel.lib.token.factory.TokenFactory;
import io.corbel.lib.token.model.TokenType;

public class DefaultMailService implements MailService {

    private final EventsService eventsService;
    private final ScopeService scopeService;
    private final TokenFactory tokenFactory;
    private final ClientRepository clientRepository;
    private final Clock clock;
    //Reset Password configuration
    private final String resetPasswordTokenScope;
    private final long resetTokenDurationInSeconds;
    private final String defaultResetNotificationId;
    private final String defaultResetUrl;
    //Email Validation configuration
    private final String emailValidationTokenScope;
    private final long emailValidationTokenDurationInSeconds;
    private final String defaultEmailValidationNotificationId;
    private final String defaultEmailValidationUrl;


    public DefaultMailService(EventsService eventsService, ScopeService scopeService, TokenFactory tokenFactory,
                              ClientRepository clientRepository, Clock clock,
                              String resetPasswordTokenScope, long resetTokenDurationInSeconds,
                              String defaultResetNotificationId, String defaultResetUrl,
                              String emailValidationTokenScope, long emailValidationTokenDurationInSeconds,
                              String defaultEmailValidationNotificationId, String defaultEmailValidationUrl) {
        this.eventsService = eventsService;
        this.scopeService = scopeService;
        this.tokenFactory = tokenFactory;
        this.clientRepository = clientRepository;
        this.resetPasswordTokenScope = resetPasswordTokenScope;
        this.clock = clock;
        this.resetTokenDurationInSeconds = resetTokenDurationInSeconds;
        this.defaultResetNotificationId = defaultResetNotificationId;
        this.defaultResetUrl = defaultResetUrl;
        this.emailValidationTokenScope = emailValidationTokenScope;
        this.emailValidationTokenDurationInSeconds = emailValidationTokenDurationInSeconds;
        this.defaultEmailValidationNotificationId = defaultEmailValidationNotificationId;
        this.defaultEmailValidationUrl = defaultEmailValidationUrl;
    }

    @Override
    public void sendMailResetPassword(String clientId, User user) {
        Optional.ofNullable(clientRepository.findOne(clientId)).ifPresent(client -> {
            String notificationId = Optional.ofNullable(client.getResetNotificationId()).orElse(defaultResetNotificationId);
            String resetUrl = Optional.ofNullable(client.getResetUrl()).orElse(defaultResetUrl);
            sendResetPasswordMail(notificationId, clientId, resetUrl, user, resetPasswordTokenScope, resetTokenDurationInSeconds);
        });
    }

    @Override
    public void sendMailValidation(String clientId, User user) {
        Optional.ofNullable(clientRepository.findOne(clientId)).ifPresent(client -> {
            if(client.getEmailValidationEnabled() != null && client.getEmailValidationEnabled()) {
                String notificationId = Optional.ofNullable(client.getEmailValidationNotificationId()).orElse(defaultEmailValidationNotificationId);
                String mailValidationUrl = Optional.ofNullable(client.getEmailValidationUrl()).orElse(defaultEmailValidationUrl);
                sendMailValidationMail(notificationId, clientId, mailValidationUrl, user, emailValidationTokenScope, emailValidationTokenDurationInSeconds);
            }
        });
    }

    private void sendResetPasswordMail(String notificationId, String clientId, String url, User user, String scopeId, long tokenDurationInSeconds) {
        String token = tokenFactory.createToken(TokenInfo.newBuilder().setType(TokenType.TOKEN).setOneUseToken(true).setUserId(user.getId())
                .setClientId(clientId).setDomainId(user.getDomain()).build(), tokenDurationInSeconds).getAccessToken();
        sendMail(token, notificationId, clientId, url, user, scopeId, tokenDurationInSeconds);
    }

    private void sendMailValidationMail(String notificationId, String clientId, String url, User user, String scopeId, long tokenDurationInSeconds) {
        String token = tokenFactory.createToken(TokenInfo.newBuilder().setType(TokenType.TOKEN).setOneUseToken(true).setUserId(user.getId())
                .setState(user.getEmail()).setClientId(clientId).setDomainId(user.getDomain()).build(), tokenDurationInSeconds).getAccessToken();
        sendMail(token, notificationId, clientId, url, user, scopeId, tokenDurationInSeconds);
    }

    private void sendMail(String token, String notificationId, String clientId, String url, User user, String scopeId, long tokenDurationInSeconds) {
        setTokenScope(token, clientId, user.getId(), user.getDomain(), scopeId, tokenDurationInSeconds);
        String clientUrl = url.replace("{token}", token);
        Map<String, String> properties = new HashMap<>();
        properties.put("clientUrl", clientUrl);
        properties.put("email", user.getEmail());
        properties.put("firstName", Optional.ofNullable(user.getFirstName()).orElse(""));
        properties.put("lastName", Optional.ofNullable(user.getLastName()).orElse(""));
        user.getProperties().entrySet().parallelStream().filter(p -> p.getValue() instanceof String)
                                       .forEach(p -> properties.put(p.getKey(), p.getValue().toString()));

        eventsService.sendNotificationEvent(user.getDomain(), notificationId, user.getEmail(), properties);
    }

    private void setTokenScope(String token, String clientId, String userId, String domainId, String scope, long tokenDurationInSeconds) {
        long expireAt = clock.instant().plus(tokenDurationInSeconds, ChronoUnit.SECONDS).toEpochMilli();
        Set<String> scopes = new HashSet<>();
        scopes.add(scope);
        Set<Scope> filledScopes = scopeService.fillScopes(scopeService.expandScopes(scopes), userId, clientId, domainId);
        scopeService.publishAuthorizationRules(token, expireAt, filledScopes);
    }

}