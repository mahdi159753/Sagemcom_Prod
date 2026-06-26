package com.alibou.security.chat;

import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ChatFileStorageService fileStorageService;
    private final AICopilotClient aiCopilotClient;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageDto chatMessageDto) {
        ChatMessageDto savedMessage = chatService.saveMessage(chatMessageDto);
        
        // Push message to recipient's topic
        messagingTemplate.convertAndSend(
                "/topic/messages/" + chatMessageDto.getRecipientId(),
                savedMessage
        );
    }

    @GetMapping("/api/v1/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessageDto>> findChatMessages(
            @PathVariable Integer senderId,
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(chatService.findChatMessages(senderId, recipientId));
    }
    
    @GetMapping("/api/v1/chat/users")
    public ResponseEntity<List<UserContactDto>> getContacts(Principal principal) {
        // Find current user id based on email in principal
        User currentUser = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        // Return all users except the current one and the AI
        List<UserContactDto> contacts = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUser.getId()) && !"ai@sagemcom.com".equals(u.getEmail()))
                .map(u -> new UserContactDto(
                        u.getId(), 
                        u.getFirstname(), 
                        u.getLastname(), 
                        u.getEmail(), 
                        u.getLastActive()
                ))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(contacts);
    }

    @PostMapping("/api/v1/chat/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/chat/files/")
                .path(fileName)
                .toUriString();
        return ResponseEntity.ok(fileDownloadUri);
    }

    @GetMapping("/api/v1/chat/files/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
