package com.alibou.security.figuide;

import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/fi-guide")
@RequiredArgsConstructor
public class FiGuideController {

    private final FiGuideService fiGuideService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<FiDocument> uploadFiche(
            @RequestParam("produitId") Integer produitId,
            @RequestParam("version") String version,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Connected user not found"));
        
        FiDocument doc = fiGuideService.uploadFiche(produitId, file, version, user);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/produit/{produitId}/active")
    public ResponseEntity<Map<String, Object>> getActiveInstruction(@PathVariable Integer produitId) {
        try {
            FiDocument doc = fiGuideService.getActiveDocument(produitId);
            List<InstructionStep> steps = fiGuideService.getStepsForDocument(doc.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("document", doc);
            response.put("steps", steps);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return empty or 404 response if no active FI
            Map<String, Object> response = new HashMap<>();
            response.put("document", null);
            response.put("steps", List.of());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/session/start")
    public ResponseEntity<ControlSession> startSession(
            @RequestParam("produitId") Integer produitId,
            Principal principal) {
        
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Connected user not found"));
        
        ControlSession session = fiGuideService.startSession(produitId, user);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/session/{sessionId}/validate-step")
    public ResponseEntity<ControlStepValidation> validateStep(
            @PathVariable Integer sessionId,
            @RequestParam("stepId") Integer stepId,
            @RequestParam("status") String status,
            @RequestParam(value = "comment", required = false) String comment,
            Principal principal) {
        
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Connected user not found"));
        
        ControlStepValidation validation = fiGuideService.validateStep(sessionId, stepId, status, comment, user);
        return ResponseEntity.ok(validation);
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<ControlSession> completeSession(
            @PathVariable Integer sessionId) {
        
        ControlSession session = fiGuideService.completeSession(sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/session/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getSessionReport(@PathVariable Integer sessionId) {
        return ResponseEntity.ok(fiGuideService.getSessionReport(sessionId));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ControlSession>> getAllSessions() {
        return ResponseEntity.ok(fiGuideService.getAllSessions());
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path file = Paths.get("uploads/fi_images").resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
                else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── IMAGE ASSIGNMENT ENDPOINTS ───────────────────────────────────────────

    @GetMapping("/documents/{docId}/extracted-images")
    public ResponseEntity<List<String>> getExtractedImages(@PathVariable Integer docId) {
        return ResponseEntity.ok(fiGuideService.getExtractedImages(docId));
    }

    @PostMapping("/steps/{stepId}/assign-image")
    public ResponseEntity<InstructionStep> assignImage(
            @PathVariable Integer stepId,
            @RequestBody Map<String, String> body) {
        String imageUrl = body.get("imageUrl");
        return ResponseEntity.ok(fiGuideService.assignImageToStep(stepId, imageUrl));
    }

    @PostMapping("/steps/{stepId}/remove-image")
    public ResponseEntity<InstructionStep> removeImage(
            @PathVariable Integer stepId,
            @RequestBody Map<String, String> body) {
        String imageUrl = body.get("imageUrl");
        return ResponseEntity.ok(fiGuideService.removeImageFromStep(stepId, imageUrl));
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Integer sessionId) {
        fiGuideService.deleteSession(sessionId);
        return ResponseEntity.ok().build();
    }
}
