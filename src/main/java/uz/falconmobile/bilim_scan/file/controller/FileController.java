package uz.falconmobile.bilim_scan.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.falconmobile.bilim_scan.file.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/profile")
    public String uploadProfileImage(@RequestParam("file") MultipartFile file) {
        return fileStorageService.saveProfileImage(file);
    }
}