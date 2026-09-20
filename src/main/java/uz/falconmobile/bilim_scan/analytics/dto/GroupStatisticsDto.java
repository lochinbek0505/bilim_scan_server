package uz.falconmobile.bilim_scan.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GroupStatisticsDto {
    private String guruhId;
    private Integer totalStudents;
    private Double overallAverage; // Guruhning umumiy o'zlashtirishi
    private Integer totalSuspiciousExams; // Guruhdagi jami shubhali testlar soni
    private List<SubjectStatsDto> subjectStats;

    @Data
    @Builder
    public static class SubjectStatsDto {
        private String subjectName;
        private Double averagePercentage;
        private Integer masteredCount;     // 80-100% olganlar soni
        private Integer satisfactoryCount; // 60-80% olganlar soni
        private Integer failedCount;       // 60% dan pastlar soni
        private Integer suspiciousCount;   // Shu fandan nechta shubhali urinish bo'ldi?
    }
}