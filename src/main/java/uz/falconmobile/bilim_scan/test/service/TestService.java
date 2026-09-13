package uz.falconmobile.bilim_scan.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;
import uz.falconmobile.bilim_scan.catalog.repository.KafedraRepository;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlan;
import uz.falconmobile.bilim_scan.eduplan.repository.EduPlanRepository;
import uz.falconmobile.bilim_scan.eduplan.repository.EduPlanTopicRepository;
import uz.falconmobile.bilim_scan.test.dto.*;
import uz.falconmobile.bilim_scan.test.model.EduTest;
import uz.falconmobile.bilim_scan.test.model.QuestionType;
import uz.falconmobile.bilim_scan.test.model.TestOption;
import uz.falconmobile.bilim_scan.test.model.TestQuestion;
import uz.falconmobile.bilim_scan.test.repository.EduTestRepository;
import uz.falconmobile.bilim_scan.test.repository.TestQuestionRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {
    private final EduTestRepository eduTestRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final FanRepository fanRepository;
    private final KafedraRepository kafedraRepository;
    private final EduPlanRepository eduPlanRepository;
    private final EduPlanTopicRepository eduPlanTopicRepository;
    public List<TestResponseDto> getAllTests() {
        return eduTestRepository.findAll()
                .stream()
                .map(this::toTestResponse)
                .toList();
    }

    public TestResponseDto getTestById(String testId) {
        return toTestResponse(findTestByIdOrThrow(testId));
    }

    public TestResponseDto createTest(TestRequestDto dto) {
        EduTest test = new EduTest();
        Instant now = Instant.now();
        test.setName(requireNonBlank(dto.getName(), "Test nomi bo'sh bo'lishi mumkin emas"));
        test.setFan(findFanByIdOrThrow(dto.getFanId()));
        test.setKafedra(findKafedraByIdOrThrow(dto.getKafedraId()));
        test.setOquvReja(findEduPlanByIdOrThrow(dto.getEduPlanId()));
        test.setOquvYili(test.getOquvReja().getOquvYili());
        test.setOquvOyi(test.getOquvYili());
        test.setCreateAt(now);
        test.setUpdateAt(now);
        return toTestResponse(eduTestRepository.save(test));
    }

    public TestResponseDto updateTest(String testId, TestRequestDto dto) {
        EduTest test = findTestByIdOrThrow(testId);
        test.setName(requireNonBlank(dto.getName(), "Test nomi bo'sh bo'lishi mumkin emas"));
        test.setFan(findFanByIdOrThrow(dto.getFanId()));
        test.setKafedra(findKafedraByIdOrThrow(dto.getKafedraId()));
        test.setOquvReja(findEduPlanByIdOrThrow(dto.getEduPlanId()));
        test.setUpdateAt(Instant.now());
        test.setOquvYili(test.getOquvYili());
        test.setOquvOyi(test.getOquvOyi());
        return toTestResponse(eduTestRepository.save(test));
    }

    public void deleteTest(String testId) {
        findTestByIdOrThrow(testId);
        testQuestionRepository.deleteByTestId(testId);
        eduTestRepository.deleteById(testId);
    }

    public List<TestQuestionResponseDto> getAllQuestions(String testId) {
        findTestByIdOrThrow(testId);
        return testQuestionRepository.findByTestId(testId)
                .stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    public TestQuestionResponseDto getQuestionById(String testId, String questionId) {
        findTestByIdOrThrow(testId);
        return toQuestionResponse(findQuestionByIdOrThrow(testId, questionId));
    }

    public TestQuestionResponseDto createQuestion(String testId, TestQuestionRequestDto dto) {
        findTestByIdOrThrow(testId);
        TestQuestion question = new TestQuestion();
        question.setTestId(testId);
        applyQuestionDtoBasic(question, dto);

        // Avval bazaga saqlab olib, ID generatsiya qilamiz
        question = testQuestionRepository.save(question);

        // TR larni ID ga aylantirish
        Map<Integer, String> trToIdMap = buildTrToIdMap(testId);
        question.setRelatedQuestionIds(resolveTrsToIds(dto.getRelatedQuestionTrs(), trToIdMap, question.getTr()));

        return toQuestionResponse(testQuestionRepository.save(question));
    }

    public List<TestQuestionResponseDto> createQuestions(String testId, List<TestQuestionRequestDto> dtos) {
        findTestByIdOrThrow(testId);
        if (dtos == null || dtos.isEmpty()) {
            throw new RuntimeException("Savollar ro'yxati bo'sh bo'lishi mumkin emas");
        }

        List<TestQuestion> questions = new ArrayList<>();
        for (TestQuestionRequestDto dto : dtos) {
            TestQuestion question = new TestQuestion();
            question.setTestId(testId);
            applyQuestionDtoBasic(question, dto);
            questions.add(question);
        }

        // 1. Asosiy ma'lumotlarni saqlab, barcha savollar uchun ID olamiz
        questions = testQuestionRepository.saveAll(questions);

        // 2. Ushbu testga tegishli barcha savollarning TR -> ID xaritasini tuzamiz
        Map<Integer, String> trToIdMap = buildTrToIdMap(testId);

        // 3. Bog'liq savollarning TR larini bazadagi ID larga almashtiramiz
        for (int i = 0; i < dtos.size(); i++) {
            TestQuestionRequestDto dto = dtos.get(i);
            TestQuestion question = questions.get(i);
            List<String> relatedIds = resolveTrsToIds(dto.getRelatedQuestionTrs(), trToIdMap, question.getTr());
            question.setRelatedQuestionIds(relatedIds);
        }

        // 4. Bog'langan ID lar bilan yakuniy saqlash
        return testQuestionRepository.saveAll(questions)
                .stream()
                .map(this::toQuestionResponse)
                .toList();
    }

    public TestQuestionResponseDto updateQuestion(String testId, String questionId, TestQuestionRequestDto dto) {
        findTestByIdOrThrow(testId);
        TestQuestion question = findQuestionByIdOrThrow(testId, questionId);
        applyQuestionDtoBasic(question, dto);

        Map<Integer, String> trToIdMap = buildTrToIdMap(testId);
        question.setRelatedQuestionIds(resolveTrsToIds(dto.getRelatedQuestionTrs(), trToIdMap, question.getTr()));

        return toQuestionResponse(testQuestionRepository.save(question));
    }

    public void deleteQuestion(String testId, String questionId) {
        findTestByIdOrThrow(testId);
        TestQuestion question = findQuestionByIdOrThrow(testId, questionId);
        testQuestionRepository.deleteById(question.getId());
    }

    // Savolning bog'liqlikdan (related) tashqari barcha ma'lumotlarini to'ldirish
    private void applyQuestionDtoBasic(TestQuestion question, TestQuestionRequestDto dto) {
        question.setTitle(requireNonBlank(dto.getTitle(), "Savol sarlavhasi bo'sh bo'lishi mumkin emas"));
        if(eduPlanTopicRepository.existsById(dto.getTopicId()) == false){
            throw new RuntimeException("Savol mavzusi topilmadi: " + dto.getTopicId());
        }else{
            question.setMavzu(eduPlanTopicRepository.findById(dto.getTopicId()).get());
        }
        QuestionType type = requireType(dto.getType());
        question.setType(type);
        question.setTr(requirePositive(dto.getTr(), "Savol tartib raqami (tr) noto'g'ri"));
        question.setOptions(normalizeOptions(dto.getOptions(), type));
    }

    // Testdagi barcha savollarning TR larini ularning ID lariga moslovchi Map
    private Map<Integer, String> buildTrToIdMap(String testId) {
        return testQuestionRepository.findByTestId(testId).stream()
                .collect(Collectors.toMap(
                        TestQuestion::getTr,
                        TestQuestion::getId,
                        (existing, replacement) -> existing // Agar bir xil TR takrorlansa, birinchisini oladi
                ));
    }

    // DTO dan kelgan TR larni Bazadagi ID larga o'tkazish
    private List<String> resolveTrsToIds(List<Integer> relatedTrs, Map<Integer, String> trToIdMap, Integer currentTr) {
        if (relatedTrs == null || relatedTrs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> resolvedIds = new ArrayList<>();
        for (Integer tr : relatedTrs) {
            if (tr == null) continue;

            if (tr.equals(currentTr)) {
                throw new RuntimeException("Savol o'ziga o'zi bog'lanishi mumkin emas (tr: " + tr + ")");
            }

            String relatedId = trToIdMap.get(tr);
            if (relatedId == null) {
                throw new RuntimeException("Bog'langan savol topilmadi. TR: " + tr);
            }
            resolvedIds.add(relatedId);
        }

        return resolvedIds.stream().distinct().toList();
    }

    private List<TestOption> normalizeOptions(List<TestOptionDto> optionDtos, QuestionType type) {
        if (type == QuestionType.OPEN || type == QuestionType.WRITTEN) {
            return Collections.emptyList();
        }
        if (optionDtos == null || optionDtos.isEmpty()) {
            throw new RuntimeException("Yopiq savol uchun variantlar bo'sh bo'lishi mumkin emas");
        }

        List<TestOption> options = optionDtos.stream()
                .map(dto -> {
                    TestOption option = new TestOption();
                    option.setText(requireNonBlank(dto.getText(), "Variant matni bo'sh bo'lishi mumkin emas"));
                    option.setTrue(Boolean.TRUE.equals(dto.getIsTrue())); // Jackson ishlaydigan nomlarga e'tibor bering
                    return option;
                })
                .toList();

        long trueCount = options.stream().filter(TestOption::isTrue).count();
        if (type == QuestionType.SINGLE_CHOICE && trueCount != 1) {
            throw new RuntimeException("SINGLE_CHOICE savolida aynan bitta to'g'ri javob bo'lishi kerak " + trueCount);
        }
        if (type == QuestionType.MULTIPLE_CHOICE && trueCount < 1) {
            throw new RuntimeException("MULTIPLE_CHOICE savolida kamida bitta to'g'ri javob bo'lishi kerak");
        }
        return options;
    }

    private TestQuestionResponseDto toQuestionResponse(TestQuestion question) {
        List<TestOptionDto> optionDtos = question.getOptions() == null
                ? Collections.emptyList()
                : question.getOptions().stream()
                .map(option -> {
                    TestOptionDto dto = new TestOptionDto();
                    dto.setText(option.getText());
                    dto.setIsTrue(option.isTrue());
                    return dto;
                })
                .toList();
        return TestQuestionResponseDto.builder()
                .id(question.getId())
                .testId(question.getTestId())
                .title(question.getTitle())
                .mavzu(question.getMavzu())
                .type(question.getType())
                // Bazada saqlanuvchi ID lar response sifatida qaytadi
                .relatedQuestionIds(question.getRelatedQuestionIds() == null ? Collections.emptyList() : question.getRelatedQuestionIds())
                .options(optionDtos)
                .build();
    }

    private TestResponseDto toTestResponse(EduTest test) {
        CatalogItemResponseDto fanDto = test.getFan() == null ? null : CatalogItemResponseDto.builder()
                .id(test.getFan().getId())
                .name(test.getFan().getName())
                .build();
        CatalogItemResponseDto kafedraDto = test.getKafedra() == null ? null : CatalogItemResponseDto.builder()
                .id(test.getKafedra().getId())
                .name(test.getKafedra().getName())
                .build();
        EduPlanSummaryDto rejaDto = toPlanSummaryDto(test.getOquvReja());
        return TestResponseDto.builder()
                .id(test.getId())
                .name(test.getName())
                .createAt(test.getCreateAt())
                .updateAt(test.getUpdateAt())
                .fan(fanDto)
                .kafedra(kafedraDto)
                .oquvReja(rejaDto)
                .build();
    }

    private EduPlanSummaryDto toPlanSummaryDto(EduPlan plan) {
        if (plan == null) {
            return null;
        }
        CatalogItemResponseDto fanDto = plan.getFan() == null ? null : CatalogItemResponseDto.builder()
                .id(plan.getFan().getId())
                .name(plan.getFan().getName())
                .build();
        CatalogItemResponseDto kafedraDto = plan.getKafedra() == null ? null : CatalogItemResponseDto.builder()
                .id(plan.getKafedra().getId())
                .name(plan.getKafedra().getName())
                .build();
        return EduPlanSummaryDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .createAt(plan.getCreateAt())
                .updateAt(plan.getUpdateAt())
                .fan(fanDto)
                .kafedra(kafedraDto)
                .oquvYili(plan.getOquvYili())
                .build();
    }

    private EduTest findTestByIdOrThrow(String testId) {
        return eduTestRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test topilmadi: " + testId));
    }

    private TestQuestion findQuestionByIdOrThrow(String testId, String questionId) {
        return testQuestionRepository.findByIdAndTestId(questionId, testId)
                .orElseThrow(() -> new RuntimeException("Test savoli topilmadi: " + questionId));
    }

    private Fan findFanByIdOrThrow(String fanId) {
        String normalizedId = requireNonBlank(fanId, "Fan ID bo'sh bo'lishi mumkin emas");
        return fanRepository.findById(normalizedId)
                .orElseThrow(() -> new RuntimeException("Fan topilmadi: " + normalizedId));
    }

    private Kafedra findKafedraByIdOrThrow(String kafedraId) {
        String normalizedId = requireNonBlank(kafedraId, "Kafedra ID bo'sh bo'lishi mumkin emas");
        return kafedraRepository.findById(normalizedId)
                .orElseThrow(() -> new RuntimeException("Kafedra topilmadi: " + normalizedId));
    }

    private EduPlan findEduPlanByIdOrThrow(String eduPlanId) {
        String normalizedId = requireNonBlank(eduPlanId, "O'quv reja ID bo'sh bo'lishi mumkin emas");
        return eduPlanRepository.findById(normalizedId)
                .orElseThrow(() -> new RuntimeException("O'quv reja topilmadi: " + normalizedId));
    }

    private QuestionType requireType(QuestionType type) {
        if (type == null) {
            throw new RuntimeException("Savol turi bo'sh bo'lishi mumkin emas");
        }
        return type;
    }

    private Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new RuntimeException(message);
        }
        return value;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }
}