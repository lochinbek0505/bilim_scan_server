package uz.falconmobile.bilim_scan.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.analytics.dto.GlobalStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.GroupStatisticsDto;
import uz.falconmobile.bilim_scan.analytics.dto.StudentMonitoringDto;
import uz.falconmobile.bilim_scan.analytics.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Bitta kursantning butun davr uchun monitoringini olish
    @GetMapping("/monitoring/student/{studentId}")
    public StudentMonitoringDto getStudentMonitoring(@PathVariable String studentId) {
        return analyticsService.getStudentMonitoring(studentId);
    }

    // 2. Butun guruh uchun fanlar kesimidagi statistika
    @GetMapping("/statistics/group/{guruhId}")
    public GroupStatisticsDto getGroupStatistics(@PathVariable String guruhId) {
        return analyticsService.getGroupStatistics(guruhId);
    }

    // 3. Butun litsey bo'yicha umumiy statistika va vaqt dinamikasi
    @GetMapping("/statistics/lyceum")
    public GlobalStatisticsDto getLyceumStatistics() {
        return analyticsService.getLyceumStatistics();
    }

    // 4. Bosqich (Kurs) bo'yicha umumiy statistika va vaqt dinamikasi
    @GetMapping("/statistics/stage/{bosqichId}")
    public GlobalStatisticsDto getStageStatistics(@PathVariable String bosqichId) {
        return analyticsService.getStageStatistics(bosqichId);
    }

    // 5. Alohida fan bo'yicha umumiy statistika va vaqt dinamikasi (barcha guruhlar kesimida)
    @GetMapping("/statistics/subject/{fanId}")
    public GlobalStatisticsDto getSubjectStatistics(@PathVariable String fanId) {
        return analyticsService.getSubjectStatistics(fanId);
    }
}