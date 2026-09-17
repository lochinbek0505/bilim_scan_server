package uz.falconmobile.bilim_scan.exam.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StudentAvailableExamDto {
    private String id;
    private String name;
    private String test;
    private Instant startTime;
    private Instant endTime;
    private Integer durationMinutes;
    private Integer questionCount;
    
    private Integer maxAttempts;
    private Integer usedAttempts;
    private Integer remainingAttempts;
}