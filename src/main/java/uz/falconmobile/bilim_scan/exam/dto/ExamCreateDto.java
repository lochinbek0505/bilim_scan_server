package uz.falconmobile.bilim_scan.exam.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamCreateDto {
    private String testId;
    private String name;
    private String guruhId;
    private Integer durationMinutes;
    private Integer questionCount; // Savollar soni
    private Integer maxAttempts;   // Urinishlar soni
    private String oquvOyi;
    private String oquvYili;
    private List<String> combinedTestIds;
}