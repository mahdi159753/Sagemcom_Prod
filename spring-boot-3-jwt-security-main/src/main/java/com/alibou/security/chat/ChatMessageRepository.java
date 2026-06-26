package com.alibou.security.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender.id = :user1Id AND m.recipient.id = :user2Id) OR (m.sender.id = :user2Id AND m.recipient.id = :user1Id) ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatMessages(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.sender.id = :senderId AND m.recipient.id = :recipientId AND m.status = 'SENT'")
    long countUnreadMessages(@Param("senderId") Integer senderId, @Param("recipientId") Integer recipientId);
}
