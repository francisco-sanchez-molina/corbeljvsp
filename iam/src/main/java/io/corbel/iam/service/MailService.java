package io.corbel.iam.service;

import io.corbel.iam.model.User;

public interface MailService {

    void sendMailResetPassword(String clientId, User user);

    void sendMailValidation(String clientId, User user);
}
