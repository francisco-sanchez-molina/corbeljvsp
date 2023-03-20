package io.corbel.notifications.cli.dsl

import io.corbel.lib.cli.console.Description
import io.corbel.lib.cli.console.Shell
import io.corbel.lib.mongo.IdGenerator
import io.corbel.notifications.model.Domain
import io.corbel.notifications.model.Notification
import io.corbel.notifications.model.NotificationTemplate
import io.corbel.notifications.repository.DomainRepository
import io.corbel.notifications.repository.NotificationRepository

/**
 * @author Alberto J. Rubio
 *
 */
@Shell("notifications")
class NotificationsShell {

    NotificationRepository notificationRepository
    DomainRepository domainRepository
    IdGenerator<NotificationTemplate> notificationIdGenerator

    public NotificationsShell(NotificationRepository notificationRepository,
                              DomainRepository domainRepository,
                              IdGenerator<NotificationTemplate> notificationIdGenerator) {
        this.notificationRepository = notificationRepository
        this.domainRepository = domainRepository
        this.notificationIdGenerator = notificationIdGenerator
    }

    @Description("Creates a new notification on the DB. The input parameter is a map containing the notification data.")
    def createNotification(notificationFields) {
        assert notificationFields.domain : 'Notification domain is required'
        assert notificationFields.name : 'Notification name is required'
        assert notificationFields.sender : 'Notification sender is required'
        assert notificationFields.type : 'Notification type is required'
        assert notificationFields.text : 'Notification text is required'
        NotificationTemplate notification = new NotificationTemplate()
        notification.domain = notificationFields.domain
        notification.name = notificationFields.name
        notification.sender = notificationFields.sender
        notification.type = notificationFields.type
        notification.text = notificationFields.text
        notification.title = notificationFields.title
        notification.replyTo = notificationFields.replyTo
        notification.id = notificationFields.id? notificationFields.id : notificationIdGenerator.generateId(notification)
        notificationRepository.save(notification)
    }

    @Description("Creates a new notification domain on the DB. The input parameter is a map containing the notification config data.")
    def createNotificationDomain(domainFields) {
        assert domainFields.id : 'Domain id is required'
        Domain domain = new Domain()
        domain.id = domainFields.id
        domain.templates = domainFields.templates
        domain.properties = domainFields.properties
        domain.multiLanguageTemplates = domainFields.multiLanguageTemplates
        domain.developmentMode = domainFields.developmentMode
        domain.iosNotificationsCertificate = domainFields.iosNotificationsCertificate
        domain.iosNotificationsPassword = domainFields.iosNotificationsPassword
        domainRepository.save(domain)
    }

}

