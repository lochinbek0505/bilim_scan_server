package uz.falconmobile.bilim_scan.analytics.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.exam.model.MasteryLevel;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StudentMonitoringDto {
    private String studentId;
    private Double overallPercentage; // Butun davr uchun umumiy o'zlashtirish
    private MasteryLevel overallMastery;
    private List<YearlyMonitoringDto> academicYears;

    @Data
    @Builder
    public static class YearlyMonitoringDto {
        private String year;
        private List<MonthlyMonitoringDto> months;
    }

    @Data
    @Builder
    public static class MonthlyMonitoringDto {
        private String month;
        private List<SubjectMonitoringDto> subjects;
    }

    @Data
    @Builder
    public static class SubjectMonitoringDto {
        private String subjectId;
        private String subjectName;
        private Double averagePercentage;
        private MasteryLevel subjectMastery;
        private List<ExamResultDto> exams;
    }

    @Data
    @Builder
    public static class ExamResultDto {
        private String examSessionId;
        private String examName;
        private Instant date;
        private Double percentage;
        private MasteryLevel masteryLevel;
    }
}