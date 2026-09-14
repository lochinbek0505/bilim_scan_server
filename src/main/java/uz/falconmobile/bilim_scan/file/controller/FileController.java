package uz.falconmobile.bilim_scan.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.falconmobile.bilim_scan.file.service.FileStorageService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload-profile")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        // Servisdan URL manzilni olamiz
        String fileUrl = fileStorageService.saveProfileImage(file);

        // Javobni JSON formatiga o'tkazish uchun Map dan foydalanamiz
        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("message", "Fayl muvaffaqiyatli yuklandi");

        // Frontendga JSON ko'rinishida { "url": "/uploads/profiles/...", "message": "..." } bo'lib boradi
        return ResponseEntity.ok(response);
    }
}