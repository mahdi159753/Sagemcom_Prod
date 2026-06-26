package com.alibou.security.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private Integer id;
    private Integer senderId;
    private Integer recipientId;
    private String content;
    private String attachmentUrl;
    private LocalDateTime timestamp;
    private MessageStatus status;
}
