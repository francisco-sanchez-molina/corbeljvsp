package io.corbel.iam.service;

/**
 * @author Alberto J. Rubios
 */
public interface CaptchaService {

    boolean verifyRequestCaptcha(String domainId, String clientId, String captcha);

}
