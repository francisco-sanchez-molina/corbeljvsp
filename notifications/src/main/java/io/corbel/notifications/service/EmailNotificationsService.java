package io.corbel.notifications.service;

import com.google.common.base.Charsets;
import io.corbel.notifications.model.Domain;
import io.corbel.notifications.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by Alberto J. Rubio
 */
public class EmailNotificationsService implements NotificationsService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationsService.class);
    private String host;
    private String port;

    public EmailNotificationsService(String host, String port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void send(Domain domain, NotificationTemplate notificationTemplate, String... recipients) {
        send(domain, notificationTemplate, null, recipients);
    }

    @Override
    public void send(Domain domain, NotificationTemplate notificationTemplate, String replyTo, String... recipients) {
        try {
            LOG.info("Sending email to: {}" + Arrays.toString(recipients));
            Properties props = new Properties();
            props.setProperty("mail.smtp.host", host);
            props.setProperty("mail.smtp.port", port);
            MimeMessage message = new MimeMessage(Session.getDefaultInstance(props, null));
            InternetAddress[] from = InternetAddress.parse(notificationTemplate.getSender());
            if(from.length != 1){
                throw new IllegalStateException("Only one FROM address is accepted. Got: "+notificationTemplate.getSender());
            }
            message.setFrom(from[0]);
            for(InternetAddress recipient : Arrays.asList(recipients).stream()
                    .map(str -> {
                        try {
                            return InternetAddress.parse(str)[0];
                        }
                        catch (AddressException e) {
                            LOG.warn("Invalid recipient address: "+str);
                            return null;
                        }
                    })
                    .filter(r -> r != null)
                    .collect(Collectors.toList())){
                message.addRecipient(Message.RecipientType.BCC,recipient);
            }
            if(replyTo != null) {
                InternetAddress[] replyToAddresses = InternetAddress.parse(replyTo);
                message.setReplyTo(replyToAddresses);
            }


            message.setSubject(notificationTemplate.getTitle(), Charsets.UTF_8.toString());
            message.setContent(notificationTemplate.getText(), "text/html; charset=utf-8");
            Transport.send(message);
            LOG.info("Email was sent to: {}" + Arrays.toString(recipients));
        } catch (MessagingException e) {
            LOG.error("Sending mail error: {}", e.getMessage(), e);
        }
    }
}