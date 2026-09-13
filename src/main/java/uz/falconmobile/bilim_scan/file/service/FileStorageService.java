package uz.falconmobile.bilim_scan.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveProfileImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath); // Papka yo'q bo'lsa, yaratadi
            }

            // Faylga unikal nom berish
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // Faylni papkaga nusxalash
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Frontend uchun qaytadigan rasm URL manzili
            return "/uploads/profiles/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Faylni saqlashda xatolik yuz berdi!", e);
        }
    }
}
