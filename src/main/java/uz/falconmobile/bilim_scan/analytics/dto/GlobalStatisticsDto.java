package uz.falconmobile.bilim_scan.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GlobalStatisticsDto {
    private String scope; // "LYCEUM", "STAGE", yoki "SUBJECT"
    private String scopeId; // Bosqich ID yoki Fan ID (Butun litsey uchun null)
    
    private Integer totalStudentsParticipated;
    private Integer totalExamsTaken;
    private Double overallAveragePercentage;
    
    // O'zlashtirish darajalari bo'yicha talabalar/imtihonlar soni
    private Integer masteredCount;     // 80-100%
    private Integer satisfactoryCount; // 60-80%
    private Integer failedCount;       // < 60%
    
    // Vaqt o'tishi bilan dinamika (Yil va Oylar kesimida)
    private List<TimeDynamicDto> timeDynamics;
    
    // Agar umumiy yoki bosqich bo'lsa, fanlar kesimidagi reyting
    private List<GroupStatisticsDto.SubjectStatsDto> subjectPerformances;

    private Integer totalSuspiciousExams; // Bosqich/Litsey bo'yicha jami shubhali holatlar

    @Data
    @Builder
    public static class TimeDynamicDto {
        private String year;
        private String month;
        private Double averagePercentage;
        private Integer examCount;
    }
}