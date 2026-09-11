package uz.falconmobile.bilim_scan.exam.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "exam_sessions")
public class ExamSession {
    @Id
    private String id;
    private String testId;       // EduTest ID
    private String guruhId;      // Guruh ID
    private Instant startTime;
    private Instant endTime;
    private Integer durationMinutes;
    private Integer questionCount; // Imtihondagi savollar soni
    private Integer maxAttempts;   // Necha marta urinish mumkinligi
    private boolean isActive = true;

    // Yakuniy imtihon uchun (bir nechta testlarni birlashtirish)
    private List<String> combinedTestIds;
}