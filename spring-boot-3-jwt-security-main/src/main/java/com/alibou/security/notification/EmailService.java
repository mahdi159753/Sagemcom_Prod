package com.alibou.security.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:sagemcom.noreply@gmail.com}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        log.info("============== ENVOI DE REEL EMAIL ==============");
        log.info("TO: {}", to);
        log.info("SUBJECT: {}", subject);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom(fromEmail);
            
            mailSender.send(message); 
            log.info("Email envoyé avec succès.");
        } catch (Exception e) {
            log.warn("Erreur lors de l'envoi de l'email : {}", e.getMessage());
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("============== ENVOI DE REEL EMAIL (HTML) ==============");
        log.info("TO: {}", to);
        log.info("SUBJECT: {}", subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML
            helper.setFrom(fromEmail);
            
            mailSender.send(message); 
            log.info("Email HTML envoyé avec succès.");
        } catch (Exception e) {
            log.warn("Erreur lors de l'envoi de l'email HTML : {}", e.getMessage());
        }
    }
}
