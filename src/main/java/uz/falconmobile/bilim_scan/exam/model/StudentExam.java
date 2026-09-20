package uz.falconmobile.bilim_scan.exam.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "student_exams")
public class StudentExam {
    @Id
    private String id;
    private String examSessionId;
    private String studentId;     // User ID
    private Instant startedAt;
    private Instant finishedAt;

    private List<String> assignedQuestionIds;
    private Map<String, List<String>> presentedOptions;

    // Shubha tekshiruvi natijalari
    private Boolean isSuspicious; // Natija shubhalimi? (Vaqt yoki Tavakkal)
    private String suspicionReason; // Nima sababdan shubhali?
    private Long timeTakenSeconds; // Testni ishlashga ketgan umumiy vaqt (sekundda)
    // Natijalar
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Double percentage;
    private MasteryLevel masteryLevel; // O'zlashtirish darajasi

    // Mavzular bo'yicha o'zlashtirish holati (Mavzu nomi -> O'zlashtirildimi? true/false)
    private Map<String, Boolean> topicMastery;
}
