package io.corbel.notifications.service;

import io.corbel.notifications.model.Domain;
import io.corbel.notifications.model.NotificationTemplate;
import io.corbel.notifications.repository.DomainRepository;
import io.corbel.notifications.repository.NotificationRepository;
import io.corbel.notifications.template.NotificationFiller;
import io.corbel.notifications.utils.DomainNameIdGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Cristian del Cerro
 */
public class DefaultSenderNotificationsService implements SenderNotificationsService {

    private static final String LANGUAGE_SEPARATOR = ":";

    private final NotificationFiller notificationFiller;
    private final NotificationsDispatcher notificationsDispatcher;
    private final NotificationRepository notificationRepository;
    private final DomainRepository domainRepository;
    private final String languageProperty;
    private final String defaultLanguage;

    public DefaultSenderNotificationsService(NotificationFiller notificationFiller,
                                             NotificationsDispatcher notificationsDispatcher,
                                             NotificationRepository notificationRepository,
                                             DomainRepository domainRepository,
                                             String languageProperty,
                                             String defaultLanguage) {
        this.notificationFiller = notificationFiller;
        this.notificationsDispatcher = notificationsDispatcher;
        this.notificationRepository = notificationRepository;
        this.domainRepository = domainRepository;
        this.languageProperty = languageProperty;
        this.defaultLanguage = defaultLanguage;
    }

    @Override
    public void sendNotification(String domainId, String notificationId, Map<String, String> customProperties, String ... recipients) {

        Domain domain = domainRepository.findOne(domainId);

        String notificationTemplateId = DomainNameIdGenerator.generateNotificationTemplateId(domainId,
                Optional.ofNullable(domain).map(Domain::getTemplates)
                .map(currentTemplate -> currentTemplate.get(notificationId))
                .orElse(notificationId));

        Map<String, String> properties = Optional.ofNullable(domain)
                .map(currentDomain -> getProperties(currentDomain, customProperties))
                .orElse(customProperties);

        boolean multiLanguageTemplate = Optional.ofNullable(domain).map(Domain::getMultiLanguageTemplates)
                .map(t -> t.contains(notificationId)).orElse(false);

        if(multiLanguageTemplate) {
            String lang = properties.getOrDefault(languageProperty, defaultLanguage);
            notificationTemplateId = notificationTemplateId + LANGUAGE_SEPARATOR + lang;
        }

        NotificationTemplate notificationTemplate = notificationRepository.findOne(notificationTemplateId);
        if (notificationTemplate != null) {
            NotificationTemplate notificationTemplateFilled = notificationFiller.fill(notificationTemplate, properties);
            notificationsDispatcher.send(domain, notificationTemplateFilled, notificationTemplate.getReplyTo(), recipients);
        }
    }

    private Map<String, String> getProperties(Domain domain, Map<String, String> customProperties) {
        Map<String, String> propertiesByDomain = domain.getProperties() != null ? domain.getProperties() : Collections.emptyMap();

        Map<String, String> properties = new HashMap<>();
        properties.putAll(propertiesByDomain);

        if(customProperties != null) {
            properties.putAll(customProperties);
        }

        return  properties;
    }


}
