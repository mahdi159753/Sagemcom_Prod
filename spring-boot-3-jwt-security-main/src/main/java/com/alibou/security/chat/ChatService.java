package com.alibou.security.chat;

import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository repository;
    private final UserRepository userRepository;

    public ChatMessageDto saveMessage(ChatMessageDto chatMessageDto) {
        User sender = userRepository.findById(chatMessageDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User recipient = userRepository.findById(chatMessageDto.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(chatMessageDto.getContent())
                .attachmentUrl(chatMessageDto.getAttachmentUrl())
                .timestamp(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();

        ChatMessage savedMessage = repository.save(message);

        return mapToDto(savedMessage);
    }

    public List<ChatMessageDto> findChatMessages(Integer senderId, Integer recipientId) {
        return repository.findChatMessages(senderId, recipientId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    private ChatMessageDto mapToDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .recipientId(message.getRecipient().getId())
                .content(message.getContent())
                .attachmentUrl(message.getAttachmentUrl())
                .timestamp(message.getTimestamp())
                .status(message.getStatus())
                .build();
    }
}
