package uz.falconmobile.bilim_scan.exam.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.exam.model.MasteryLevel;
import uz.falconmobile.bilim_scan.test.dto.TestQuestionResponseDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class StudentExamSubmitResponseDto {
    private String id;
    private String examSessionId;
    private String studentId;
    private Instant startedAt;
    private Instant finishedAt;
    
    // Natijalar
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Double percentage;
    private MasteryLevel masteryLevel;
    private Map<String, Boolean> topicMastery;
    
    // ID'lar ro'yxati o'rniga to'liq savollar obyekti
    private List<TestQuestionResponseDto> questions; 
}