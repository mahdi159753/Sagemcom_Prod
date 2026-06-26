package com.alibou.security.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer userId;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead;

    private LocalDateTime createdAt;
}
