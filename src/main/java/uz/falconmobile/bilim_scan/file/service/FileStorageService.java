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

            // 1. Asl fayl nomidan faqat kengaytmani ajratib olish (masalan: .jpg, .png)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 2. Original nomni qo'shmasdan, faqat UUID va kengaytmadan iborat unikal nom yaratish
            String fileName = UUID.randomUUID().toString() + extension;
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