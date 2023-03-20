package io.corbel.notifications.service;

import io.corbel.notifications.model.Domain;
import io.corbel.notifications.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.raudi.pushraven.Pushraven;
import java.util.Arrays;

/**
 * Created by Alberto J. Rubio
 */
public class FCMPushNotificationsService implements NotificationsService {

    private static final Logger LOG = LoggerFactory.getLogger(FCMPushNotificationsService.class);

    @Override
    public void send(Domain domain, NotificationTemplate notificationTemplate, String... recipients) {
        Pushraven.setKey(notificationTemplate.getSender());
        Pushraven.notification.title(notificationTemplate.getTitle()).text(notificationTemplate.getText());
        Pushraven.notification.addAllMulticasts(Arrays.asList(recipients));
        try {
            Pushraven.push();
            LOG.info("Android push notification sent to: " + Arrays.toString(recipients));
        } catch (Exception e) {
            LOG.error("Sending android FCM push notification error: {}", e.getMessage(), e);
        } finally {
            Pushraven.notification.clear();
        }
    }

    @Override
    public void send(Domain domain, NotificationTemplate notificationTemplate, String replyTo, String... recipient) {
        send(domain, notificationTemplate, recipient);
    }
}
