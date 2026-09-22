package uz.falconmobile.bilim_scan.exam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;
import uz.falconmobile.bilim_scan.catalog.repository.GuruhRepository;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlanTopic;
import uz.falconmobile.bilim_scan.exam.dto.*;
import uz.falconmobile.bilim_scan.exam.model.ExamSession;
import uz.falconmobile.bilim_scan.exam.model.MasteryLevel;
import uz.falconmobile.bilim_scan.exam.model.StudentExam;
import uz.falconmobile.bilim_scan.exam.repository.ExamSessionRepository;
import uz.falconmobile.bilim_scan.exam.repository.StudentExamRepository;
import uz.falconmobile.bilim_scan.test.dto.TestOptionDto;
import uz.falconmobile.bilim_scan.test.dto.TestQuestionResponseDto;
import uz.falconmobile.bilim_scan.test.model.EduTest;
import uz.falconmobile.bilim_scan.test.model.QuestionType;
import uz.falconmobile.bilim_scan.test.model.TestOption;
import uz.falconmobile.bilim_scan.test.model.TestQuestion;
import uz.falconmobile.bilim_scan.test.repository.EduTestRepository;
import uz.falconmobile.bilim_scan.test.repository.TestQuestionRepository;

import java.time.Duration;
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
    private final EduTestRepository eduTestRepository;

    public List<StudentAvailableExamDto> getAvailableExamsForStudent(String guruhId, String studentId) {
        List<ExamSession> activeSessions = examSessionRepository.findByGuruhIdAndIsActiveTrue(guruhId);
        List<StudentAvailableExamDto> responseList = new ArrayList<>();

        for (ExamSession session : activeSessions) {
            List<StudentExam> attempts = studentExamRepository.findByExamSessionIdAndStudentId(session.getId(), studentId);
            int usedAttempts = attempts.size();
            int maxAttempts = session.getMaxAttempts() != null ? session.getMaxAttempts() : 1;
            int remainingAttempts = maxAttempts - usedAttempts;

            responseList.add(StudentAvailableExamDto.builder()
                    .id(session.getId())
                    .name(session.getName())
                    .test(session.getTest())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .durationMinutes(session.getDurationMinutes())
                    .questionCount(session.getQuestionCount())
                    .maxAttempts(maxAttempts)
                    .usedAttempts(usedAttempts)
                    .remainingAttempts(Math.max(remainingAttempts, 0))
                    .build());
        }
        return responseList;
    }

    public List<ExamSession> getAllExamSessionsForAdmin() {
        return examSessionRepository.findAll();
    }

    public ExamSession createExam(ExamCreateDto dto) {
        ExamSession session = new ExamSession();
        if (dto.getGuruhId() != null) {
            session.setGuruh(guruhRepository.findById(dto.getGuruhId())
                    .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + dto.getGuruhId())));
        }
        if (dto.getTestId() != null) {
            EduTest test = eduTestRepository.findById(dto.getTestId())
                    .orElseThrow(() -> new RuntimeException("Test topilmadi: " + dto.getTestId()));
            session.setFanId(test.getFan().getId());
        } else if (dto.getCombinedTestIds() != null && !dto.getCombinedTestIds().isEmpty()) {
            EduTest firstTest = eduTestRepository.findById(dto.getCombinedTestIds().get(0))
                    .orElseThrow(() -> new RuntimeException("Birinchi test topilmadi: " + dto.getCombinedTestIds().get(0)));
            session.setFanId(firstTest.getFan().getId());
        }
        session.setName(dto.getName());
        session.setTest(dto.getTestId());
        session.setDurationMinutes(dto.getDurationMinutes());
        session.setQuestionCount(dto.getQuestionCount() != null ? dto.getQuestionCount() : 15);
        session.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 1);
        session.setStartTime(Instant.now());
        session.setOquv_oyi(dto.getOquvOyi());
        session.setOquv_yili(dto.getOquvYili());
        session.setCombinedTestIds(dto.getCombinedTestIds());
        return examSessionRepository.save(session);
    }

    public StudentExamStartResponseDto startStudentExam(String examSessionId, String studentId) {
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("Imtihon topilmadi"));

        List<StudentExam> previousAttempts = studentExamRepository.findByExamSessionIdAndStudentId(examSessionId, studentId);
        int currentAttempt = previousAttempts.size() + 1;
        if (currentAttempt > session.getMaxAttempts()) {
            throw new RuntimeException("Urinishlar soni tugagan (Maksimal: " + session.getMaxAttempts() + " marta)");
        }

        List<TestQuestion> allQuestions = new ArrayList<>();
        if (session.getCombinedTestIds() != null && !session.getCombinedTestIds().isEmpty()) {
            for (String tId : session.getCombinedTestIds()) {
                allQuestions.addAll(testQuestionRepository.findByTestId(tId));
            }
        } else {
            allQuestions = testQuestionRepository.findByTestId(session.getTest());
        }

        if (allQuestions.isEmpty()) {
            throw new RuntimeException("Testda savollar mavjud emas");
        }

        List<TestQuestion> assignedQuestions = generateRandomQuestionsWithRelations(allQuestions, session.getQuestionCount());
        List<String> assignedIds = assignedQuestions.stream().map(TestQuestion::getId).toList();

        // Variantlarni teng taqsimlash uchun 0, 1, 2, 3 indekslar aralashtiriladi
        List<Integer> correctIndices = new ArrayList<>();
        for (int i = 0; i < assignedQuestions.size(); i++) {
            correctIndices.add(i % 4);
        }
        Collections.shuffle(correctIndices);

        Map<String, List<String>> presentedOptionsMap = new HashMap<>();
        List<TestQuestionResponseDto> questionDtos = new ArrayList<>();

        for (int i = 0; i < assignedQuestions.size(); i++) {
            TestQuestion q = assignedQuestions.get(i);
            int targetCorrectIndex = correctIndices.get(i);

            List<TestOptionDto> shuffledOptions = shuffleOptionsEvenly(q, targetCorrectIndex);
            presentedOptionsMap.put(q.getId(), shuffledOptions.stream().map(TestOptionDto::getText).toList());

            questionDtos.add(TestQuestionResponseDto.builder()
                    .id(q.getId())
                    .testId(q.getTestId())
                    .title(q.getTitle())
                    .mavzu(q.getMavzu())
                    .type(q.getType())
                    .relatedQuestionIds(q.getRelatedQuestionIds() == null ? Collections.emptyList() : q.getRelatedQuestionIds())
                    .options(shuffledOptions)
                    .build());
        }

        StudentExam studentExam = new StudentExam();
        studentExam.setExamSessionId(examSessionId);
        studentExam.setStudentId(studentId);
        studentExam.setAssignedQuestionIds(assignedIds);
        studentExam.setStartedAt(Instant.now());
        studentExam.setPresentedOptions(presentedOptionsMap);

        studentExam = studentExamRepository.save(studentExam);

        return StudentExamStartResponseDto.builder()
                .id(studentExam.getId())
                .studentExamId(studentExam.getId())
                .examSessionId(session.getId())
                .startedAt(studentExam.getStartedAt())
                .durationMinutes(session.getDurationMinutes())
                .attemptNumber(currentAttempt)
                .questions(questionDtos)
                .build();
    }

    public StudentExamSubmitResponseDto submitExam(String studentExamId, StudentAnswerSubmitDto dto) {
        StudentExam studentExam = studentExamRepository.findById(studentExamId)
                .orElseThrow(() -> new IllegalArgumentException("Imtihon topilmadi: " + studentExamId));

        Instant finishedAt = Instant.now();
        studentExam.setFinishedAt(finishedAt);

        long timeTakenSeconds = Duration.between(studentExam.getStartedAt(), finishedAt).getSeconds();

        int correctAnswersCount = 0;
        int totalQuestions = studentExam.getAssignedQuestionIds().size();

        Map<String, TopicStats> topicStatsMap = new HashMap<>();

        int maxConsecutiveSameOption = 0;
        int currentConsecutive = 1;
        Integer lastSelectedOptionIndex = null;

        // Savollarning to'g'ri/xato holatini va bog'liqliklarini saqlab borish
        Map<String, Boolean> correctnessMap = new HashMap<>();
        Map<String, List<String>> questionRelationsMap = new HashMap<>();

        for (String questionId : studentExam.getAssignedQuestionIds()) {
            TestQuestion question = testQuestionRepository.findById(questionId).orElse(null);
            if (question == null) continue;

            String topicKey = (question.getMavzu() != null && question.getMavzu().getId() != null)
                    ? question.getMavzu().getId()
                    : "unknown_topic";

            topicStatsMap.putIfAbsent(topicKey, new TopicStats());

            List<String> studentAnswers = dto.getAnswers().getOrDefault(questionId, Collections.emptyList());
            boolean isCorrect = checkAnswerIsCorrect(question, studentAnswers);

            // Natijani Map ga saqlash
            correctnessMap.put(questionId, isCorrect);
            if (question.getRelatedQuestionIds() != null && !question.getRelatedQuestionIds().isEmpty()) {
                questionRelationsMap.put(questionId, question.getRelatedQuestionIds());
            }

            topicStatsMap.get(topicKey).total++;
            if (isCorrect) {
                correctAnswersCount++;
                topicStatsMap.get(topicKey).correct++;
            }

            if (!studentAnswers.isEmpty() && question.getType() == QuestionType.SINGLE_CHOICE && studentExam.getPresentedOptions() != null) {
                String selectedText = studentAnswers.get(0);
                List<String> presented = studentExam.getPresentedOptions().get(questionId);

                if (presented != null) {
                    int selectedIndex = presented.indexOf(selectedText);
                    if (selectedIndex != -1) {
                        if (lastSelectedOptionIndex != null && lastSelectedOptionIndex == selectedIndex) {
                            currentConsecutive++;
                            if (currentConsecutive > maxConsecutiveSameOption) {
                                maxConsecutiveSameOption = currentConsecutive;
                            }
                        } else {
                            currentConsecutive = 1;
                        }
                        lastSelectedOptionIndex = selectedIndex;
                    }
                }
            }
        }

        // Bog'liq savollardagi shubhani tekshirish
        boolean isSuspiciousRelation = false;
        for (Map.Entry<String, List<String>> entry : questionRelationsMap.entrySet()) {
            String mainQuestionId = entry.getKey();
            Boolean mainIsCorrect = correctnessMap.get(mainQuestionId);

            if (mainIsCorrect == null) continue;

            for (String relatedId : entry.getValue()) {
                Boolean relatedIsCorrect = correctnessMap.get(relatedId);
                // Agar o'zaro bog'liq savollar natijasi farq qilsa (biri true, ikkinchisi false)
                if (relatedIsCorrect != null && mainIsCorrect != relatedIsCorrect) {
                    isSuspiciousRelation = true;
                    break;
                }
            }
            if (isSuspiciousRelation) break;
        }

        boolean isSuspiciousPattern = maxConsecutiveSameOption >= 5;

        studentExam.setTimeTakenSeconds(timeTakenSeconds);
        // Vaqtga oid tekshiruv (isSuspiciousTime) olib tashlandi
        studentExam.setIsSuspicious(isSuspiciousPattern || isSuspiciousRelation);

        List<String> suspicionReasons = new ArrayList<>();

        if (isSuspiciousPattern)
            suspicionReasons.add("Tavakkal ehtimoli: " + maxConsecutiveSameOption + " ta ketma-ket bir xil variant belgilangan");
        if (isSuspiciousRelation)
            suspicionReasons.add("O'zaro bog'liq savollarning nomutanosibligi : javoblarning  biri to'g'ri, ikkinchisi xato ishlangan");

        studentExam.setSuspicionReason(String.join(". ", suspicionReasons));

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

        studentExam = studentExamRepository.save(studentExam);
        return buildSubmitResponse(studentExam);
    }
    public StudentExamSubmitResponseDto getExamResultBySessionAndStudent(String examSessionId, String studentId) {
        List<StudentExam> attempts = studentExamRepository.findByExamSessionIdAndStudentId(examSessionId, studentId);
        if (attempts.isEmpty()) {
            throw new RuntimeException("Talabaning ushbu imtihon bo'yicha natijasi topilmadi");
        }

        StudentExam latestExam = attempts.stream()
                .filter(exam -> exam.getFinishedAt() != null)
                .max(Comparator.comparing(StudentExam::getFinishedAt))
                .orElseThrow(() -> new RuntimeException("Yakunlangan imtihon topilmadi"));

        return buildSubmitResponse(latestExam);
    }

    public ExamSession updateExam(String examSessionId, ExamCreateDto dto) {
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("Imtihon topilmadi: " + examSessionId));

        if (dto.getGuruhId() != null) {
            session.setGuruh(guruhRepository.findById(dto.getGuruhId())
                    .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + dto.getGuruhId())));
        }
        if (dto.getName() != null && !dto.getName().isBlank()) session.setName(dto.getName());
        if (dto.getTestId() != null && !dto.getTestId().isBlank()) session.setTest(dto.getTestId());
        if (dto.getDurationMinutes() > 0) session.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getQuestionCount() != null && dto.getQuestionCount() > 0) session.setQuestionCount(dto.getQuestionCount());
        if (dto.getMaxAttempts() != null && dto.getMaxAttempts() > 0) session.setMaxAttempts(dto.getMaxAttempts());
        if (dto.getCombinedTestIds() != null) session.setCombinedTestIds(dto.getCombinedTestIds());
        if (dto.getOquvOyi() != null && !dto.getOquvOyi().isBlank()) session.setOquv_oyi(dto.getOquvOyi());
        if (dto.getOquvYili() != null && !dto.getOquvYili().isBlank()) session.setOquv_yili(dto.getOquvYili());
        if (dto.getTestId() != null) {
            EduTest test = eduTestRepository.findById(dto.getTestId())
                    .orElseThrow(() -> new RuntimeException("Test topilmadi: " + dto.getTestId()));
            session.setFanId(test.getFan().getId());
        } else if (dto.getCombinedTestIds() != null && !dto.getCombinedTestIds().isEmpty()) {
            EduTest firstTest = eduTestRepository.findById(dto.getCombinedTestIds().get(0))
                    .orElseThrow(() -> new RuntimeException("Birinchi test topilmadi: " + dto.getCombinedTestIds().get(0)));
            session.setFanId(firstTest.getFan().getId());
        }

        return examSessionRepository.save(session);
    }

    public boolean disableExam(String examSessionId) {
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("Imtihon topilmadi: " + examSessionId));
        session.setActive(false);
        examSessionRepository.save(session);
        return true;
    }

    public boolean deleteExamSession(String examSessionId) {
        ExamSession session = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new RuntimeException("Imtihon topilmadi: " + examSessionId));
        examSessionRepository.delete(session);
        return true;
    }

    // --- YORDAMCHI METODLAR ---

    private List<TestQuestion> generateRandomQuestionsWithRelations(List<TestQuestion> allQuestions, int requiredCount) {
        Map<String, TestQuestion> questionMap = allQuestions.stream()
                .collect(Collectors.toMap(TestQuestion::getId, q -> q));
        Set<String> selectedIds = new HashSet<>();
        List<TestQuestion> questionsPool = new ArrayList<>(allQuestions);
        Collections.shuffle(questionsPool);

        for (TestQuestion q : questionsPool) {
            if (selectedIds.size() >= requiredCount) {
                break; // Yetarlicha savol yig'ildi
            }
            if (selectedIds.contains(q.getId())) {
                continue; // Bu savol avval qaysidir savolga bog'liq sifatida qo'shilgan bo'lishi mumkin
            }

            // Asosiy savol va unga bog'langan barcha savollarni yig'amiz
            Set<String> groupToAdd = new HashSet<>();
            groupToAdd.add(q.getId());

            if (q.getRelatedQuestionIds() != null && !q.getRelatedQuestionIds().isEmpty()) {
                for (String relId : q.getRelatedQuestionIds()) {
                    if (questionMap.containsKey(relId)) {
                        groupToAdd.add(relId);
                    }
                }
            }

            // Barcha bog'liq savollar bilan birga testga qo'shish
            selectedIds.addAll(groupToAdd);
        }

        List<TestQuestion> finalQuestions = selectedIds.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Collections.shuffle(finalQuestions);
        return finalQuestions;
    }
    private List<TestOptionDto> shuffleOptionsEvenly(TestQuestion question, int targetCorrectIndex) {
        if (question.getOptions() == null || question.getOptions().isEmpty() || question.getType() != QuestionType.SINGLE_CHOICE) {
            if (question.getOptions() != null) {
                List<TestOptionDto> opts = question.getOptions().stream().map(o -> {
                    TestOptionDto dto = new TestOptionDto();
                    dto.setText(o.getText());
                    dto.setIsTrue(null);
                    return dto;
                }).collect(Collectors.toList());
                Collections.shuffle(opts);
                return opts;
            }
            return Collections.emptyList();
        }

        List<TestOption> correctOptions = question.getOptions().stream().filter(TestOption::isTrue).toList();
        List<TestOption> incorrectOptions = question.getOptions().stream().filter(o -> !o.isTrue()).collect(Collectors.toList());
        Collections.shuffle(incorrectOptions);

        List<TestOptionDto> result = new ArrayList<>();
        int totalSlots = question.getOptions().size();
        int actualTargetIndex = Math.min(targetCorrectIndex, totalSlots - 1);

        int incorrectIdx = 0;
        for (int i = 0; i < totalSlots; i++) {
            TestOptionDto dto = new TestOptionDto();
            dto.setIsTrue(null);

            if (i == actualTargetIndex && !correctOptions.isEmpty()) {
                dto.setText(correctOptions.get(0).getText());
            } else if (incorrectIdx < incorrectOptions.size()) {
                dto.setText(incorrectOptions.get(incorrectIdx++).getText());
            } else if (!correctOptions.isEmpty()) {
                dto.setText(correctOptions.get(0).getText());
            }
            result.add(dto);
        }
        return result;
    }

    private TestQuestionResponseDto toQuestionResponseSafe(TestQuestion question) {
        List<TestOptionDto> optionDtos = question.getOptions() == null
                ? Collections.emptyList()
                : question.getOptions().stream()
                .map(option -> {
                    TestOptionDto dto = new TestOptionDto();
                    dto.setText(option.getText());
                    dto.setIsTrue(null);
                    return dto;
                })
                .toList();

        EduPlanTopic mavzuName = (question.getMavzu() != null) ? question.getMavzu() : null;

        return TestQuestionResponseDto.builder()
                .id(question.getId())
                .testId(question.getTestId())
                .title(question.getTitle())
                .mavzu(mavzuName)
                .type(question.getType())
                .relatedQuestionIds(question.getRelatedQuestionIds() == null ? Collections.emptyList() : question.getRelatedQuestionIds())
                .options(optionDtos)
                .build();
    }

    private StudentExamSubmitResponseDto buildSubmitResponse(StudentExam studentExam) {
        List<TestQuestion> fullQuestions = studentExam.getAssignedQuestionIds().stream()
                .map(id -> testQuestionRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        List<TestQuestionResponseDto> questionDtos = fullQuestions.stream()
                .map(this::toQuestionResponseSafe)
                .toList();

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
                .isSuspicious(studentExam.getIsSuspicious())           // <-- Shu qatorlarni qo'shing
                .suspicionReason(studentExam.getSuspicionReason())     // <--
                .timeTakenSeconds(studentExam.getTimeTakenSeconds())   //
                .build();
    }

    private boolean checkAnswerIsCorrect(TestQuestion question, List<String> studentAnswers) {
        List<String> correctOptions = question.getOptions().stream()
                .filter(TestOption::isTrue)
                .map(TestOption::getText)
                .toList();
        return correctOptions.size() == studentAnswers.size() && correctOptions.containsAll(studentAnswers);
    }

    private MasteryLevel calculateMasteryLevel(double percentage) {
        if (percentage < 60.0) return MasteryLevel.FAILED;
        else if (percentage < 80.0) return MasteryLevel.SATISFACTORY;
        else return MasteryLevel.MASTERED;
    }

    private static class TopicStats {
        int total = 0;
        int correct = 0;
    }
}