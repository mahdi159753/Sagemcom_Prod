package com.alibou.security.chat;

import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final AICopilotClient aiCopilotClient;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<CopilotResponse> chat(@RequestBody CopilotRequest request, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName()).orElseThrow();
        String aiResponse = aiCopilotClient.askCopilot(request.getMessage(), currentUser.getId());
        return ResponseEntity.ok(new CopilotResponse(aiResponse));
    }
}
