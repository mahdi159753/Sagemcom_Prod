package com.alibou.security.notification;

import com.alibou.security.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getUnreadNotifications(user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getAllNotifications(user.getId()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        service.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        service.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
