package uz.falconmobile.bilim_scan.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.server.ResponseStatusException;
import uz.falconmobile.bilim_scan.backup.dto.BackupImportResultDto;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Butun bazani (barcha kolleksiyalar) va yuklangan rasmlarni bitta ZIP faylga eksport qiladi
 * va shu ZIP'dan tizimni aynan o'sha holatga qaytaradi.
 *
 * ZIP tuzilishi:
 *   manifest.json            - format, versiya, yaratilgan vaqt, kolleksiyalar va hujjatlar soni
 *   db/<kolleksiya>.jsonl    - har qatorda bitta hujjat (MongoDB Extended JSON, tiplar saqlanadi)
 *   files/<fayl nomi>        - file.upload-dir ichidagi rasmlar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    public static final String FORMAT = "bilim-scan-backup";
    public static final int VERSION = 1;

    private static final String MANIFEST = "manifest.json";
    private static final String DB_PREFIX = "db/";
    private static final String DB_SUFFIX = ".jsonl";
    private static final String FILES_PREFIX = "files/";
    private static final int INSERT_BATCH = 1000;

    // EXTENDED rejim ObjectId, Date, Long, Decimal kabi tiplarni yo'qotmaydi
    private static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${backup.dir:backups}")
    private String backupDir;

    // Eksport va import bir vaqtda ishlamasligi uchun
    private final Object lock = new Object();

    public String newBackupFileName() {
        return "bilim_scan_backup_" + LocalDateTime.now().format(FILE_TS) + ".zip";
    }

    public void exportTo(OutputStream out) throws IOException {
        synchronized (lock) {
            writeBackup(out);
        }
    }

    public BackupImportResultDto importFrom(InputStream in) throws IOException {
        synchronized (lock) {
            Path tempDir = Files.createTempDirectory("bilim-scan-import");
            try {
                // 1. Avval butun arxivni o'qib, tekshirib olamiz - xato bo'lsa bazaga tegmaymiz
                Map<String, List<Document>> collections = new LinkedHashMap<>();
                Path filesDir = Files.createDirectories(tempDir.resolve("files"));
                readBackup(in, collections, filesDir);

                // 2. Hozirgi holatni zaxiraga olib qo'yamiz (xato fayl yuklansa qaytarish uchun)
                String safetyBackup = saveSafetyBackup();

                // 3. Bazani almashtiramiz
                for (String name : mongoTemplate.getCollectionNames()) {
                    if (!name.startsWith("system.")) {
                        mongoTemplate.getCollection(name).deleteMany(new Document());
                    }
                }
                Map<String, Integer> counts = new LinkedHashMap<>();
                for (Map.Entry<String, List<Document>> entry : collections.entrySet()) {
                    List<Document> docs = entry.getValue();
                    for (int i = 0; i < docs.size(); i += INSERT_BATCH) {
                        mongoTemplate.getCollection(entry.getKey())
                                .insertMany(docs.subList(i, Math.min(i + INSERT_BATCH, docs.size())));
                    }
                    counts.put(entry.getKey(), docs.size());
                }

                // 4. Rasmlarni almashtiramiz
                Path uploadPath = Paths.get(uploadDir);
                FileSystemUtils.deleteRecursively(uploadPath);
                Files.createDirectories(uploadPath);
                int fileCount = 0;
                try (Stream<Path> files = Files.list(filesDir)) {
                    for (Path file : files.toList()) {
                        Files.copy(file, uploadPath.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        fileCount++;
                    }
                }

                log.warn("Baza backup'dan tiklandi: {} kolleksiya, {} fayl. Oldingi holat: {}",
                        counts.size(), fileCount, safetyBackup);
                return new BackupImportResultDto(counts, fileCount, safetyBackup);
            } finally {
                FileSystemUtils.deleteRecursively(tempDir);
            }
        }
    }

    private String saveSafetyBackup() throws IOException {
        Path dir = Files.createDirectories(Paths.get(backupDir));
        Path file = dir.resolve("pre_import_" + LocalDateTime.now().format(FILE_TS) + ".zip");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
            writeBackup(out);
        }
        return file.toAbsolutePath().toString();
    }

    private void writeBackup(OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        Map<String, Long> counts = new TreeMap<>();

        for (String name : new TreeSet<>(mongoTemplate.getCollectionNames())) {
            if (name.startsWith("system.")) {
                continue;
            }
            zip.putNextEntry(new ZipEntry(DB_PREFIX + name + DB_SUFFIX));
            Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
            long count = 0;
            for (Document doc : mongoTemplate.getCollection(name).find()) {
                writer.write(doc.toJson(JSON));
                writer.write('\n');
                count++;
            }
            writer.flush();
            zip.closeEntry();
            counts.put(name, count);
        }

        Path uploadPath = Paths.get(uploadDir);
        if (Files.isDirectory(uploadPath)) {
            try (Stream<Path> files = Files.list(uploadPath)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    zip.putNextEntry(new ZipEntry(FILES_PREFIX + file.getFileName()));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
        }

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("format", FORMAT);
        manifest.put("version", VERSION);
        manifest.put("createdAt", Instant.now().toString());
        manifest.put("collections", counts);
        zip.putNextEntry(new ZipEntry(MANIFEST));
        zip.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
        zip.closeEntry();

        zip.finish();
        zip.flush();
    }

    private void readBackup(InputStream in, Map<String, List<Document>> collections, Path filesDir) throws IOException {
        Map<?, ?> manifest = null;
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (name.equals(MANIFEST)) {
                    manifest = objectMapper.readValue(zip.readAllBytes(), Map.class);
                } else if (name.startsWith(DB_PREFIX) && name.endsWith(DB_SUFFIX)) {
                    String collection = name.substring(DB_PREFIX.length(), name.length() - DB_SUFFIX.length());
                    if (collection.isBlank() || collection.contains("/") || collection.startsWith("system.")) {
                        throw badFile("Noto'g'ri kolleksiya nomi: " + collection);
                    }
                    collections.put(collection, readDocuments(zip, collection));
                } else if (name.startsWith(FILES_PREFIX)) {
                    String fileName = name.substring(FILES_PREFIX.length());
                    // Zip-slip himoyasi: faqat oddiy fayl nomiga ruxsat
                    if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
                        throw badFile("Noto'g'ri fayl nomi: " + fileName);
                    }
                    Files.copy(zip, filesDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw badFile("Arxivni o'qib bo'lmadi: " + e.getMessage());
        }

        if (manifest == null || !FORMAT.equals(manifest.get("format"))) {
            throw badFile("Bu Bilim Scan backup fayli emas (manifest.json topilmadi)");
        }
        if (!(manifest.get("version") instanceof Integer version) || version > VERSION) {
            throw badFile("Backup versiyasi qo'llab-quvvatlanmaydi: " + manifest.get("version"));
        }
        if (collections.isEmpty()) {
            throw badFile("Backup ichida birorta ham kolleksiya yo'q");
        }
    }

    private List<Document> readDocuments(InputStream in, String collection) throws IOException {
        // ZipInputStream'ni yopib qo'ymaslik uchun reader'ni yopmaymiz
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        List<Document> docs = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                try {
                    docs.add(Document.parse(line));
                } catch (Exception e) {
                    throw badFile(collection + " kolleksiyasida buzilgan yozuv: " + e.getMessage());
                }
            }
        }
        return docs;
    }

    private ResponseStatusException badFile(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
