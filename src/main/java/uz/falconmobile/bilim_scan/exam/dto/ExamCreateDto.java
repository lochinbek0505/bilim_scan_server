package uz.falconmobile.bilim_scan.exam.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExamCreateDto {
    private String testId;
    private String guruhId;
    private Integer durationMinutes;
    private Integer questionCount; // Savollar soni
    private Integer maxAttempts;   // Urinishlar soni
    private List<String> combinedTestIds;
}