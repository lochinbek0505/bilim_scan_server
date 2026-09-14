package uz.falconmobile.bilim_scan.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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

    // 2. Butun guruh (bosqich) uchun fanlar kesimidagi statistika
    @GetMapping("/statistics/group/{guruhId}")
    public GroupStatisticsDto getGroupStatistics(@PathVariable String guruhId) {
        return analyticsService.getGroupStatistics(guruhId);
    }
}