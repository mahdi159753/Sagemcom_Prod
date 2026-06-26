package com.alibou.security.nc;

import com.alibou.security.produit.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/nc")
@RequiredArgsConstructor
public class NonConformityController {

    private final NonConformityService service;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<NonConformity>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NonConformity> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<NonConformity> create(
            @RequestPart("nc") NonConformity nc,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(service.create(nc, file));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<NonConformity> update(
            @PathVariable Integer id,
            @RequestPart("nc") NonConformity nc,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(service.update(id, nc, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            System.out.println("Could not determine file type.");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<NonConformity> assign(@PathVariable Integer id, @RequestBody java.util.Map<String, String> payload) {
        return ResponseEntity.ok(service.assign(id, payload.get("assigneeEmail")));
    }

    @PostMapping("/{id}/treat")
    public ResponseEntity<NonConformity> treat(@PathVariable Integer id, @RequestBody java.util.Map<String, String> payload) {
        return ResponseEntity.ok(service.treat(id, payload.get("actionCorrective")));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<NonConformity> validateAndClose(@PathVariable Integer id) {
        return ResponseEntity.ok(service.validateAndClose(id));
    }

    @PostMapping("/analyze")
    public ResponseEntity<NCAnalyzeResponse> analyze(@RequestBody java.util.Map<String, String> payload) {
        String description = payload.get("description");
        String localisation = payload.get("localisation");
        return ResponseEntity.ok(service.analyzeNC(description, localisation));
    }
}
