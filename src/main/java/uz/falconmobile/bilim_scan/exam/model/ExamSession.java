package uz.falconmobile.bilim_scan.exam.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uz.falconmobile.bilim_scan.catalog.model.Guruh;
import uz.falconmobile.bilim_scan.test.model.EduTest;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "exam_sessions")
public class ExamSession {
    @Id
    private String id;
    private String name;       // Imtihon nomi
    private String test;       // EduTest ID
    private Guruh guruh;      // Guruh ID
    private Instant startTime;
    private Instant endTime;
    private Integer durationMinutes;
    private Integer questionCount; // Imtihondagi savollar soni
    private Integer maxAttempts;   // Necha marta urinish mumkinligi
    private boolean isActive = true;

    // Yakuniy imtihon uchun (bir nechta testlarni birlashtirish)
    private List<String> combinedTestIds;
}