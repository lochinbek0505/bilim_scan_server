package uz.falconmobile.bilim_scan.test.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.test.dto.TestQuestionRequestDto;
import uz.falconmobile.bilim_scan.test.dto.TestQuestionResponseDto;
import uz.falconmobile.bilim_scan.test.dto.TestRequestDto;
import uz.falconmobile.bilim_scan.test.dto.TestResponseDto;
import uz.falconmobile.bilim_scan.test.service.TestService;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
public class TestController {
    private final TestService testService;

    @GetMapping
    public List<TestResponseDto> getAllTests(
            @RequestParam(required = false) String fanId,
            @RequestParam(required = false) String kafedraId
    ) {
        return testService.getAllTests(fanId, kafedraId);
    }
    @GetMapping("/{testId}")
    public TestResponseDto getTestById(@PathVariable String testId) {
        return testService.getTestById(testId);
    }

    @PostMapping
    public TestResponseDto createTest(@RequestBody TestRequestDto dto) {
        return testService.createTest(dto);
    }

    @PutMapping("/{testId}")
    public TestResponseDto updateTest(@PathVariable String testId, @RequestBody TestRequestDto dto) {
        return testService.updateTest(testId, dto);
    }

    @DeleteMapping("/{testId}")
    public String deleteTest(@PathVariable String testId) {
        testService.deleteTest(testId);
        return "Test o'chirildi";
    }

    @GetMapping("/{testId}/questions")
    public List<TestQuestionResponseDto> getAllQuestions(@PathVariable String testId) {
        return testService.getAllQuestions(testId);
    }

    @GetMapping("/{testId}/questions/{questionId}")
    public TestQuestionResponseDto getQuestionById(@PathVariable String testId, @PathVariable String questionId) {
        return testService.getQuestionById(testId, questionId);
    }

    @PostMapping("/{testId}/questions")
    public TestQuestionResponseDto createQuestion(@PathVariable String testId, @RequestBody TestQuestionRequestDto dto) {
        return testService.createQuestion(testId, dto);
    }

    @PostMapping("/{testId}/questions/bulk")
    public List<TestQuestionResponseDto> createQuestions(
            @PathVariable String testId,
            @RequestBody List<TestQuestionRequestDto> dtos
    ) {
        return testService.createQuestions(testId, dtos);
    }

    @PutMapping("/{testId}/questions/{questionId}")
    public TestQuestionResponseDto updateQuestion(
            @PathVariable String testId,
            @PathVariable String questionId,
            @RequestBody TestQuestionRequestDto dto
    ) {
        return testService.updateQuestion(testId, questionId, dto);
    }

    @DeleteMapping("/{testId}/questions/{questionId}")
    public ResponseEntity<Boolean> deleteQuestion(@PathVariable String testId, @PathVariable String questionId) {
        testService.deleteQuestion(testId, questionId);
        return ResponseEntity.ok(true);
    }
}
