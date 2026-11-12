package com.yep.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(EmailGateway.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String host;
    private final String from;

    public EmailGateway(ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String host, @Value("${yep.mail-from:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.host = host;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        if (host == null || host.isBlank()) {
            log.info("Email delivery skipped (MAIL_HOST not configured): to={}, subject={}", to, subject);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email delivery skipped (no JavaMailSender bean): to={}, subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.warn("Email delivery failed: to={}, subject={}", to, subject, exception);
        }
    }
}
