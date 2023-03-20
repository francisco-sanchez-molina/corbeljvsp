package io.corbel.iam.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;

import io.corbel.iam.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.corbel.iam.model.Client;
import io.corbel.iam.repository.ClientRepository;
import io.corbel.lib.token.TokenGrant;
import io.corbel.lib.token.factory.TokenFactory;

@RunWith(MockitoJUnitRunner.class) public class DefaultMailServiceTest {

    private static final String CLIENT_ID = "jasdjklrjaskl";
    private static final String USER_ID = "asdfbieubizr";
    private static final String EMAIL = "sruiaesrhi@nsjfneira.sdf";
    private static final String USER_FIRSTNAME = "Pepe";
    private static final String USER_LASTNAME = "Jones";
    private static final String RESET_NOTIFICATION_ID = "nszduirnewaijnfaj";
    private static final String ACCESS_TOKEN = "nsuirneianrea";
    private static final long RESET_PASSWORD_TOKEN_DURATION = 300;
    private static final String RESET_PASSWORD_TOKEN_SCOPE = "iam:user:me";
    private static final String DOMAIN_ID = "domain_id";
    private static final String RESET_URL = "resetUrlTest";
    private static final long EMAIL_VALIDATION_TOKEN_DURATION = 100;
    private static final String EMAIL_VALIDATION_TOKEN_SCOPE = "iam:user:validate";
    private static final String EMAIL_VALIDATION_URL = "emailValidationUrlTest";
    private static final String EMAIL_VALIDATION_NOTIFICATION_ID = "dkjfalsjdfañsdfa";
    private static User testUser;

    @Mock private EventsService eventsService;
    @Mock private ScopeService scopeService;
    @Mock private UserService userService;
    @Mock private TokenFactory tokenFactory;
    @Mock private ClientRepository clientRepository;

    private DefaultMailService defaultMailService;

    @Before
    public void setup() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setFirstName(USER_FIRSTNAME);
        testUser.setLastName(USER_LASTNAME);
        testUser.setEmail(EMAIL);
        testUser.setDomain(DOMAIN_ID);
        when(userService.findById(USER_ID)).thenReturn(testUser);

        defaultMailService = new DefaultMailService(eventsService, scopeService, tokenFactory, clientRepository,
                Clock.systemUTC(), RESET_PASSWORD_TOKEN_SCOPE, RESET_PASSWORD_TOKEN_DURATION, RESET_NOTIFICATION_ID, RESET_URL,
                EMAIL_VALIDATION_TOKEN_SCOPE, EMAIL_VALIDATION_TOKEN_DURATION, EMAIL_VALIDATION_NOTIFICATION_ID, EMAIL_VALIDATION_URL);
    }

    @Test
    public void testSendMailResetPassword() {
        Client testClient = mock(Client.class);
        TokenGrant tokenGrant = mock(TokenGrant.class);

        when(clientRepository.findOne(CLIENT_ID)).thenReturn(testClient);
        when(testClient.getResetNotificationId()).thenReturn(RESET_NOTIFICATION_ID);
        when(tokenFactory.createToken(any(), eq(RESET_PASSWORD_TOKEN_DURATION))).thenReturn(tokenGrant);
        when(tokenGrant.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(testClient.getResetUrl()).thenReturn(RESET_URL);
        when(testClient.getId()).thenReturn(CLIENT_ID);


        defaultMailService.sendMailResetPassword(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verify(testClient).getResetNotificationId();
        verify(tokenFactory).createToken(any(), eq(RESET_PASSWORD_TOKEN_DURATION));
        verify(tokenGrant).getAccessToken();

    }

    @Test
    public void testSendMailResetPasswordWithoutResetNotificationId() {
        Client testClient = mock(Client.class);
        TokenGrant tokenGrant = mock(TokenGrant.class);

        when(clientRepository.findOne(CLIENT_ID)).thenReturn(testClient);
        when(testClient.getResetNotificationId()).thenReturn(null);
        when(tokenFactory.createToken(any(), eq(RESET_PASSWORD_TOKEN_DURATION))).thenReturn(tokenGrant);
        when(tokenGrant.getAccessToken()).thenReturn(ACCESS_TOKEN);

        when(testClient.getResetUrl()).thenReturn(RESET_URL);
        when(testClient.getId()).thenReturn(CLIENT_ID);


        defaultMailService.sendMailResetPassword(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verify(testClient).getResetNotificationId();
    }

    @Test
    public void testSendMailResetPasswordWithoutClientId() {
        when(clientRepository.findOne(CLIENT_ID)).thenReturn(null);

        defaultMailService.sendMailResetPassword(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verifyNoMoreInteractions(clientRepository, tokenFactory);
    }

    @Test
    public void testEmailValidation() {
        Client testClient = mock(Client.class);
        TokenGrant tokenGrant = mock(TokenGrant.class);

        when(clientRepository.findOne(CLIENT_ID)).thenReturn(testClient);
        when(testClient.getEmailValidationEnabled()).thenReturn(true);
        when(testClient.getEmailValidationNotificationId()).thenReturn(EMAIL_VALIDATION_NOTIFICATION_ID);
        when(tokenFactory.createToken(any(), eq(EMAIL_VALIDATION_TOKEN_DURATION))).thenReturn(tokenGrant);
        when(tokenGrant.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(testClient.getResetUrl()).thenReturn(EMAIL_VALIDATION_URL);
        when(testClient.getId()).thenReturn(CLIENT_ID);

        defaultMailService.sendMailValidation(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verify(testClient).getEmailValidationUrl();
        verify(tokenFactory).createToken(any(), eq(EMAIL_VALIDATION_TOKEN_DURATION));
        verify(tokenGrant).getAccessToken();

    }

    @Test
    public void testSendEmailValidationWithoutEmailValidationNotificationId() {
        Client testClient = mock(Client.class);
        TokenGrant tokenGrant = mock(TokenGrant.class);

        when(clientRepository.findOne(CLIENT_ID)).thenReturn(testClient);
        when(testClient.getEmailValidationEnabled()).thenReturn(true);
        when(testClient.getEmailValidationNotificationId()).thenReturn(null);
        when(tokenFactory.createToken(any(), eq(EMAIL_VALIDATION_TOKEN_DURATION))).thenReturn(tokenGrant);
        when(tokenGrant.getAccessToken()).thenReturn(ACCESS_TOKEN);

        when(testClient.getResetUrl()).thenReturn(EMAIL_VALIDATION_URL);
        when(testClient.getId()).thenReturn(CLIENT_ID);


        defaultMailService.sendMailValidation(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verify(testClient).getEmailValidationUrl();
    }

    @Test
    public void testEmailValidationWithClientEmailValidationDisabled() {
        Client testClient = mock(Client.class);
        TokenGrant tokenGrant = mock(TokenGrant.class);

        when(clientRepository.findOne(CLIENT_ID)).thenReturn(testClient);
        when(testClient.getEmailValidationEnabled()).thenReturn(false);

        defaultMailService.sendMailValidation(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verifyNoMoreInteractions(clientRepository, tokenFactory);

    }

    @Test
    public void testEmailValidationWithoutClientId() {
        when(clientRepository.findOne(CLIENT_ID)).thenReturn(null);

        defaultMailService.sendMailValidation(CLIENT_ID, testUser);

        verify(clientRepository).findOne(CLIENT_ID);
        verifyNoMoreInteractions(clientRepository, tokenFactory);
    }
}
