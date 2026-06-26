package com.alibou.security.notification;

import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendAlert(String message, NotificationType type) {
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            Notification notification = Notification.builder()
                    .userId(user.getId())
                    .message(message)
                    .type(type)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
        }
        
        // Broadcast payload so frontend can display toast instantly
        Map<String, String> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("type", type.name());
        
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }

    public List<Notification> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }
    
    public List<Notification> getAllNotifications(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long id, Integer userId) {
        Notification notif = notificationRepository.findById(id).orElseThrow();
        if (notif.getUserId().equals(userId)) {
            notif.setRead(true);
            notificationRepository.save(notif);
        }
    }

    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for(Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
