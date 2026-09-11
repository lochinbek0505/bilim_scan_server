package uz.falconmobile.bilim_scan.exam.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.test.dto.TestQuestionResponseDto;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StudentExamStartResponseDto {
    private String studentExamId;
    private String examSessionId;
    private Instant startedAt;
    private Integer durationMinutes;
    private Integer attemptNumber; // Nechanchi urinishi ekanligi

    // Talabaga qaytariladigan to'liq savollar ro'yxati (isTrue'ni chiqarib yuborish kerak)
    private List<TestQuestionResponseDto> questions;
}