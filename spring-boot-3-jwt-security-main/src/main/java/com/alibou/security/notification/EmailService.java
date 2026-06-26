package com.alibou.security.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        log.info("============== ENVOI DE REEL EMAIL ==============");
        log.info("TO: {}", to);
        log.info("SUBJECT: {}", subject);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("sagemcom.noreply@gmail.com");
            
            mailSender.send(message); 
            log.info("Email envoyé avec succès.");
        } catch (Exception e) {
            log.warn("Erreur lors de l'envoi de l'email : {}", e.getMessage());
        }
    }
}
