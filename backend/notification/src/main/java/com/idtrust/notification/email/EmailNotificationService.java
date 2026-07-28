package com.idtrust.notification.email;

import com.idtrust.notification.config.NotificationProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * FR-NOT-001 (Email Notification) - a genuine SMTP sender (JavaMail, not a stub), built directly
 * from {@link NotificationProperties} rather than relying on Spring Boot's {@code spring.mail.*}
 * autoconfiguration, so a fresh {@link JavaMailSender} only needs to be constructed (and can only
 * fail) when {@link NotificationProperties#isEnabled()} is {@code true} - no {@code MailSender}
 * bean exists in the context at all when email is disabled, mirroring
 * {@code hedera.HcsHederaAnchorPublisher}'s "no client, no attempted connection" precedent for a
 * disabled integration.
 */
@Component
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final NotificationProperties properties;

    public EmailNotificationService(NotificationProperties properties) {
        this.properties = properties;
    }

    /** Never throws - a failed or disabled send is logged and swallowed, never surfaced to the caller. */
    public void send(String to, String subject, String body) {
        if (!properties.isEnabled()) {
            log.debug("Email notification disabled (idtrust.notification.email.enabled=false); skipping send to={} subject={}",
                    to, subject);
            return;
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(properties.getHost());
            sender.setPort(properties.getPort());
            sender.setUsername(properties.getUsername());
            sender.setPassword(properties.getPassword());

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setTo(to);
            helper.setFrom(properties.getFrom());
            helper.setSubject(subject);
            helper.setText(body);
            sender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email notification to={} subject={}", to, subject, e);
        }
    }
}
