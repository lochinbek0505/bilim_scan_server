package uz.falconmobile.bilim_scan.exam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.exam.dto.*;
import uz.falconmobile.bilim_scan.exam.model.ExamSession;
import uz.falconmobile.bilim_scan.exam.model.StudentExam;
import uz.falconmobile.bilim_scan.exam.service.ExamService;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // Talaba o'ziga tegishli (guruhi bo'yicha) faol imtihonlarni va urinishlari qoldig'ini olishi
    @GetMapping("/student/{studentId}/guruh/{guruhId}")
    public List<StudentAvailableExamDto> getStudentAvailableExams(
            @PathVariable String studentId,
            @PathVariable String guruhId) {
        return examService.getAvailableExamsForStudent(guruhId, studentId);
    }
    // O'qituvchi yoki Admin imtihon ochishi uchun
    @PostMapping("/create")
    public ExamSession createExam(@RequestBody ExamCreateDto dto) {
        return examService.createExam(dto);
    }

    // Talaba o'ziga biriktirilgan imtihonni boshlashi (To'liq savollar qaytadi)
    @PostMapping("/{examSessionId}/start/{studentId}")
    public StudentExamStartResponseDto startExam(
            @PathVariable String examSessionId,
            @PathVariable String studentId) {
        // Savollar soni va urinishlar soni endi imtihon (ExamSession) ichidan olinadi
        return examService.startStudentExam(examSessionId, studentId);
    }

    // Talaba imtihonni yakunlab, javoblarni yuborishi
    @PostMapping("/student-exam/{studentExamId}/submit")
    public StudentExamSubmitResponseDto submitExam(
            @PathVariable String studentExamId,
            @RequestBody StudentAnswerSubmitDto dto) {
        return examService.submitExam(studentExamId, dto);
    }

    @GetMapping("/{examSessionId}/score/{studentId}")
    public StudentExamSubmitResponseDto getStudentExam(@PathVariable String examSessionId , @PathVariable String studentId) {
        return examService.getExamResultBySessionAndStudent(examSessionId, studentId);
    }

    @DeleteMapping("/{examSessionId}")
    public String deleteExamSession(@PathVariable String examSessionId) {
        examService.deleteExamSession(examSessionId);
        return "Imtihon sessiyasi o'chirildi";
    }
}