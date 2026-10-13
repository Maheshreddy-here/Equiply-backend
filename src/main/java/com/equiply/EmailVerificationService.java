package com.equiply;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;
    private final String baseUrl;

    public EmailVerificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${equiply.email.enabled:false}") boolean enabled,
            @Value("${spring.mail.username:}") String from,
            @Value("${equiply.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    public boolean sendVerification(User user) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!enabled || sender == null || from == null || from.isBlank()) return false;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Verify your Equiply email");
        message.setText("Hi " + user.getFullName() + ",\n\nPlease verify your Equiply email by opening this link within 24 hours:\n"
                + baseUrl + "/verify-email?token=" + user.getEmailVerificationToken()
                + "\n\nIf you did not create this account, you can ignore this email.");
        sender.send(message);
        return true;
    }
}
