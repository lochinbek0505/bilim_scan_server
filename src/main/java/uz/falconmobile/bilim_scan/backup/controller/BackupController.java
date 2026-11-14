package uz.falconmobile.bilim_scan.backup.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import uz.falconmobile.bilim_scan.backup.dto.BackupImportResultDto;
import uz.falconmobile.bilim_scan.backup.service.BackupService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    // Butun baza + rasmlar bitta .zip bo'lib yuklab olinadi
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportBackup() {
        String fileName = backupService.newBackupFileName();
        StreamingResponseBody body = backupService::exportTo;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }

    // DIQQAT: hozirgi baza to'liq o'chirilib, yuklangan backup bilan almashtiriladi
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BackupImportResultDto importBackup(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return backupService.importFrom(in);
        }
    }

    // Frontend xato sababini ko'rsata olishi uchun
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleError(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason())));
    }
}
