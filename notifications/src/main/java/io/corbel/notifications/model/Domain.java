package io.corbel.notifications.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class Domain {

    @Id
    private String id;

    @NotEmpty
    private Map<String, String> properties;

    @NotNull
    private Map<String, String> templates;

    @NotNull
    private List<String> multiLanguageTemplates;

    private Boolean developmentMode;

    private String iosNotificationsCertificate;

    private String iosNotificationsPassword;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }

    public List<String> getMultiLanguageTemplates() {
        return multiLanguageTemplates;
    }

    public void setMultiLanguageTemplates(List<String> multiLanguageTemplates) {
        this.multiLanguageTemplates = multiLanguageTemplates;
    }

    public Boolean getDevelopmentMode() {
        return developmentMode;
    }

    public Boolean isDevelopmentMode() {
        return developmentMode;
    }

    public void setDevelopmentMode(Boolean developmentMode) {
        this.developmentMode = developmentMode;
    }

    public String getIosNotificationsCertificate() {
        return iosNotificationsCertificate;
    }

    public void setIosNotificationsCertificate(String iosNotificationsCertificate) {
        this.iosNotificationsCertificate = iosNotificationsCertificate;
    }

    public String getIosNotificationsPassword() {
        return iosNotificationsPassword;
    }

    public void setIosNotificationsPassword(String iosNotificationsPassword) {
        this.iosNotificationsPassword = iosNotificationsPassword;
    }

    public void updateDomain(Domain domain) {
        if(domain.getProperties() != null && !domain.getProperties().isEmpty()) {
            setProperties(domain.getProperties());
        }
        if(domain.getTemplates()!= null && !domain.getTemplates().isEmpty()) {
            setTemplates(domain.getTemplates());
        }
        if(domain.getMultiLanguageTemplates() != null && !domain.getMultiLanguageTemplates().isEmpty()) {
            setMultiLanguageTemplates(domain.getMultiLanguageTemplates());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Domain domain = (Domain) o;

        if (id != null ? !id.equals(domain.id) : domain.id != null) return false;
        if (properties != null ? !properties.equals(domain.properties) : domain.properties != null) return false;
        if (templates != null ? !templates.equals(domain.templates) : domain.templates != null) return false;
        if (multiLanguageTemplates != null ? !multiLanguageTemplates.equals(domain.multiLanguageTemplates) : domain.multiLanguageTemplates != null)
            return false;
        if (developmentMode != null ? !developmentMode.equals(domain.developmentMode) : domain.developmentMode != null)
            return false;
        if (iosNotificationsCertificate != null ? !iosNotificationsCertificate.equals(domain.iosNotificationsCertificate) : domain.iosNotificationsCertificate != null)
            return false;
        return iosNotificationsPassword != null ? iosNotificationsPassword.equals(domain.iosNotificationsPassword) : domain.iosNotificationsPassword == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (templates != null ? templates.hashCode() : 0);
        result = 31 * result + (multiLanguageTemplates != null ? multiLanguageTemplates.hashCode() : 0);
        result = 31 * result + (developmentMode != null ? developmentMode.hashCode() : 0);
        result = 31 * result + (iosNotificationsCertificate != null ? iosNotificationsCertificate.hashCode() : 0);
        result = 31 * result + (iosNotificationsPassword != null ? iosNotificationsPassword.hashCode() : 0);
        return result;
    }
}