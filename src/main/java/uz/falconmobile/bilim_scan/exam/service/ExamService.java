package uz.falconmobile.bilim_scan.exam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;
import uz.falconmobile.bilim_scan.catalog.repository.GuruhRepository;
import uz.falconmobile.bilim_scan.exam.dto.ExamCreateDto;
import uz.falconmobile.bilim_scan.exam.dto.StudentAnswerSubmitDto;
import uz.falconmobile.bilim_scan.exam.dto.StudentExamStartResponseDto;
import uz.falconmobile.bilim_scan.exam.dto.StudentExamSubmitResponseDto;
import uz.falconmobile.bilim_scan.exam.model.ExamSession;
import uz.falconmobile.bilim_scan.exam.model.MasteryLevel;
import uz.falconmobile.bilim_scan.exam.model.StudentExam;
import uz.falconmobile.bilim_scan.exam.repository.ExamSessionRepository;
import uz.falconmobile.bilim_scan.exam.repository.StudentExamRepository;
import uz.falconmobile.bilim_scan.test.dto.TestOptionDto;
import uz.falconmobile.bilim_scan.test.dto.TestQuestionResponseDto;
import uz.falconmobile.bilim_scan.test.model.TestOption;
import uz.falconmobile.bilim_scan.test.model.TestQuestion;
import uz.falconmobile.bilim_scan.test.repository.TestQuestionRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamSessionRepository examSessionRepository;
    private final StudentExamRepository studentExamRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final GuruhRepository guruhRepository;

    // Barcha imtihonlarni olish
    public List<ExamSession> getAllExamSessions() {
        return examSessionRepository.findAll();
    }

    // 1. Imtihon yaratish (Guruh va Testni biriktirish)
    public ExamSession createExam(ExamCreateDto dto) {
        ExamSession session = new ExamSession();
        if (dto.getGuruhId() != null) {
            session.setGuruh(guruhRepository.findById(dto.getGuruhId())
                    .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + dto.getGuruhId())));
        }
        session.setName(dto.getName());
        session.setTest(dto.getTestId());
        session.setDurationMinutes(dto.getDurationMinutes());
        session.setQuestionCount(dto.getQuestionCount() != null ? dto.getQuestionCount() : 15);
        session.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 1);
        session.setStartTime(Instant.now());
        session.setCombinedTestIds(dto.getCombinedTestIds());
        return examSessionRepository.save(session);
    }

    public StudentExamStartResponseDto startStudentExam(String examSessionId, String studentId) {
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("Imtihon topilmadi"));

        // Urinishlar sonini tekshirish
        List<StudentExam> previousAttempts = studentExamRepository.findByExamSessionIdAndStudentId(examSessionId, studentId);
        int currentAttempt = previousAttempts.size() + 1;
        if (currentAttempt > session.getMaxAttempts()) {
            throw new RuntimeException("Urinishlar soni tugagan (Maksimal: " + session.getMaxAttempts() + " marta)");
        }

        // Agar combinedTestIds bo'lsa barcha testlardan savollar olinadi, aks holda bitta testdan
        List<TestQuestion> allQuestions;
        if (session.getCombinedTestIds() != null && !session.getCombinedTestIds().isEmpty()) {
            // Buning uchun TestQuestionRepository da findByTestIdIn() yozishingiz kerak bo'ladi.
            // allQuestions = testQuestionRepository.findByTestIdIn(session.getCombinedTestIds());

            // Hozirgi imkoniyat bilan for orqali yig'ib oldik
            allQuestions = new ArrayList<>();
            for (String tId : session.getCombinedTestIds()) {
                allQuestions.addAll(testQuestionRepository.findByTestId(tId));
            }
        } else {
            allQuestions = testQuestionRepository.findByTestId(session.getTest());
        }

        if (allQuestions.isEmpty()) {
            throw new RuntimeException("Testda savollar mavjud emas");
        }

        // Savollarni random qilib yig'ish (Related logic bilan)
        List<TestQuestion> assignedQuestions = generateRandomQuestionsWithRelations(allQuestions, session.getQuestionCount());
        List<String> assignedIds = assignedQuestions.stream().map(TestQuestion::getId).toList();

        StudentExam studentExam = new StudentExam();
        studentExam.setExamSessionId(examSessionId);
        studentExam.setStudentId(studentId);
        studentExam.setAssignedQuestionIds(assignedIds);
        studentExam.setStartedAt(Instant.now());

        studentExam = studentExamRepository.save(studentExam);

        // Talabaga yuborish uchun DTO ni yig'ish
        return StudentExamStartResponseDto.builder()
                .studentExamId(studentExam.getId())
                .examSessionId(session.getId())
                .startedAt(studentExam.getStartedAt())
                .durationMinutes(session.getDurationMinutes())
                .attemptNumber(currentAttempt)
                .questions(assignedQuestions.stream().map(this::toQuestionResponseSafe).toList())
                .build();
    }

    // Savollarni 2-3 ta bog'liq qilib random tanlash (TestQuestion obyektlarini qaytaradi)
    private List<TestQuestion> generateRandomQuestionsWithRelations(List<TestQuestion> allQuestions, int requiredCount) {
        Map<String, TestQuestion> questionMap = allQuestions.stream().collect(Collectors.toMap(TestQuestion::getId, q -> q));
        Set<String> selectedIds = new HashSet<>();
        List<TestQuestion> questionsPool = new ArrayList<>(allQuestions);
        Collections.shuffle(questionsPool);

        // 1-qadam: Bog'liqligi bor savollar
        for (TestQuestion q : questionsPool) {
            if (q.getRelatedQuestionIds() != null && !q.getRelatedQuestionIds().isEmpty()) {
                selectedIds.add(q.getId());
                int relatedToAdd = Math.min(2, q.getRelatedQuestionIds().size());
                for (int i = 0; i < relatedToAdd; i++) {
                    selectedIds.add(q.getRelatedQuestionIds().get(i));
                }
                break;
            }
        }

        // 2-qadam: Qolganlari oddiy random
        for (TestQuestion q : questionsPool) {
            if (selectedIds.size() >= requiredCount) break;
            selectedIds.add(q.getId());
        }

        List<TestQuestion> finalQuestions = selectedIds.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Collections.shuffle(finalQuestions);
        return finalQuestions;
    }

    // Talabaga ketadigan javobdan "isTrue" qismini olib tashlash
    private TestQuestionResponseDto toQuestionResponseSafe(TestQuestion question) {
        List<TestOptionDto> optionDtos = question.getOptions() == null
                ? Collections.emptyList()
                : question.getOptions().stream()
                .map(option -> {
                    TestOptionDto dto = new TestOptionDto();
                    dto.setText(option.getText());
                    dto.setIsTrue(null); // MUHIM: O'quvchiga to'g'ri javob ko'rinmasligi kerak
                    return dto;
                })
                .toList();

        return TestQuestionResponseDto.builder()
                .id(question.getId())
                .testId(question.getTestId())
                .title(question.getTitle())
                .mavzu(question.getMavzu())
                .type(question.getType())
                .relatedQuestionIds(question.getRelatedQuestionIds() == null ? Collections.emptyList() : question.getRelatedQuestionIds())
                .options(optionDtos)
                .build();
    }

    // 3. Imtihonni yakunlash va natijani hisoblash
// Imtihonni yakunlash va natijani hisoblash (Yangilangan versiya)
    public StudentExamSubmitResponseDto submitExam(String studentExamId, StudentAnswerSubmitDto dto) {
        StudentExam studentExam = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new RuntimeException("Talaba imtihoni topilmadi"));

        studentExam.setFinishedAt(Instant.now());

        int correctAnswersCount = 0;
        int totalQuestions = studentExam.getAssignedQuestionIds().size();

        Map<String, TopicStats> topicStatsMap = new HashMap<>();

        // ... (Sizdagi avvalgi tekshirish logikasi o'zgarishsiz qoladi) ...
        for (String questionId : studentExam.getAssignedQuestionIds()) {
            TestQuestion question = testQuestionRepository.findById(questionId).orElse(null);
            if (question == null) continue;

            String mavzu = question.getMavzu();
            topicStatsMap.putIfAbsent(mavzu, new TopicStats());

            List<String> studentAnswers = dto.getAnswers().getOrDefault(questionId, Collections.emptyList());
            boolean isCorrect = checkAnswerIsCorrect(question, studentAnswers);

            topicStatsMap.get(mavzu).total++;
            if (isCorrect) {
                correctAnswersCount++;
                topicStatsMap.get(mavzu).correct++;
            }
        }

        double percentage = ((double) correctAnswersCount / totalQuestions) * 100;
        studentExam.setTotalQuestions(totalQuestions);
        studentExam.setCorrectAnswers(correctAnswersCount);
        studentExam.setPercentage(percentage);
        studentExam.setMasteryLevel(calculateMasteryLevel(percentage));

        Map<String, Boolean> topicMastery = new HashMap<>();
        for (Map.Entry<String, TopicStats> entry : topicStatsMap.entrySet()) {
            topicMastery.put(entry.getKey(), ((double) entry.getValue().correct / entry.getValue().total) * 100 >= 60.0);
        }
        studentExam.setTopicMastery(topicMastery);

        // Natijani bazaga saqlaymiz
        studentExam = studentExamRepository.save(studentExam);

        // --- YANGI QO'SHILGAN QISM ---
        // Bazadan savollarni to'liq chaqirib olish (Tartibni saqlash maqsadida stream ishlatamiz)
        List<TestQuestion> fullQuestions = studentExam.getAssignedQuestionIds().stream()
                .map(id -> testQuestionRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        // Savollarni DTO ga o'girish (Agar o'quvchi to'g'ri javoblarni ko'rishi mumkin bo'lsa o'zingizning oddiy toQuestionResponse ishlating.
        // Agar yashirmoqchi bo'lsangiz biz avvalroq yozgan toQuestionResponseSafe metodidan foydalaning)
        List<TestQuestionResponseDto> questionDtos = fullQuestions.stream()
                .map(this::toQuestionResponseSafe)
                .toList();

        // Obyektni Response DTO ko'rinishida yig'ib qaytarish
        return StudentExamSubmitResponseDto.builder()
                .id(studentExam.getId())
                .examSessionId(studentExam.getExamSessionId())
                .studentId(studentExam.getStudentId())
                .startedAt(studentExam.getStartedAt())
                .finishedAt(studentExam.getFinishedAt())
                .totalQuestions(studentExam.getTotalQuestions())
                .correctAnswers(studentExam.getCorrectAnswers())
                .percentage(studentExam.getPercentage())
                .masteryLevel(studentExam.getMasteryLevel())
                .topicMastery(studentExam.getTopicMastery())
                .questions(questionDtos)
                .build();
    }

    public boolean deleteExamSession(String examSessionId) {
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("Imtihon topilmadi: " + examSessionId));
        examSessionRepository.delete(session);
        return true;
    }

    private boolean checkAnswerIsCorrect(TestQuestion question, List<String> studentAnswers) {
        // Savol variantlari ichidan isTrue qiymati true bo'lganlarini ajratamiz
        List<String> correctOptions = question.getOptions().stream()
                .filter(TestOption::isTrue)
                .map(TestOption::getText)
                .toList();

        // Talaba belgilagan javoblar bilan to'g'ri javoblar ro'yxati aynan mos kelishini tekshiramiz
        return correctOptions.size() == studentAnswers.size() && correctOptions.containsAll(studentAnswers);
    }

    private MasteryLevel calculateMasteryLevel(double percentage) {
        if (percentage < 60.0) {
            return MasteryLevel.FAILED;
        } else if (percentage < 80.0) {
            return MasteryLevel.SATISFACTORY;
        } else {
            return MasteryLevel.MASTERED;
        }
    }

    // Mavzular bo'yicha yordamchi klass
    private static class TopicStats {
        int total = 0;
        int correct = 0;
    }
}