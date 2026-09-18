package uz.falconmobile.bilim_scan.eduplan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;
import uz.falconmobile.bilim_scan.catalog.repository.KafedraRepository;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanRequestDto;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanResponseDto;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanTopicRequestDto;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanTopicResponseDto;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlan;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlanTopic;
import uz.falconmobile.bilim_scan.eduplan.repository.EduPlanRepository;
import uz.falconmobile.bilim_scan.eduplan.repository.EduPlanTopicRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EduPlanService {
    private final EduPlanRepository eduPlanRepository;
    private final EduPlanTopicRepository eduPlanTopicRepository;

    private final KafedraRepository kafedraRepository;
    private final FanRepository fanRepository;

    public List<EduPlanResponseDto> getAllPlans() {
        return eduPlanRepository.findAll()
                .stream()
                .map(this::toPlanResponse)
                .toList();
    }

    public EduPlanResponseDto getPlanById(String planId) {
        EduPlan plan = findPlanByIdOrThrow(planId);
        return toPlanResponse(plan);
    }

    public EduPlanResponseDto createPlan(EduPlanRequestDto dto) {
        EduPlan plan = new EduPlan();
        Instant now = Instant.now();
        plan.setName(requireNonBlank(dto.getName(), "Plan nomi bo'sh bo'lishi mumkin emas"));
        plan.setOquvYili(requireNonBlank(dto.getOquvYili(), "O'quv yili bo'sh bo'lishi mumkin emas"));
        plan.setCreateAt(now);
        plan.setUpdateAt(now);


        if (dto.getKafedraId() == null || dto.getKafedraId().isBlank()) {
            throw new RuntimeException("Kafedra nomi allaqachon mavjud: " + dto.getName());
        } else {
            Kafedra kafedra = kafedraRepository.findById(dto.getKafedraId())
                    .orElseThrow(() -> new RuntimeException("Kafedra topilmadi: " + dto.getKafedraId()));
            plan.setKafedra(kafedra);
        }

        if (dto.getFanId() == null || dto.getFanId().isBlank()) {
            throw new RuntimeException("Fan nomi allaqachon mavjud: " + dto.getName());
        } else {
            Fan fan = fanRepository.findById(dto.getFanId())
                    .orElseThrow(() -> new RuntimeException("Fan topilmadi: " + dto.getFanId()));
            plan.setFan(fan);
        }


        return toPlanResponse(eduPlanRepository.save(plan));
    }

    public EduPlanResponseDto updatePlan(String planId, EduPlanRequestDto dto) {
        EduPlan plan = findPlanByIdOrThrow(planId);
        plan.setName(requireNonBlank(dto.getName(), "Plan nomi bo'sh bo'lishi mumkin emas"));
        plan.setOquvYili(requireNonBlank(dto.getOquvYili(), "O'quv yili bo'sh bo'lishi mumkin emas"));
        plan.setUpdateAt(Instant.now());

        // Kafedra tekshiruvi va yangilash
        if (dto.getKafedraId() == null || dto.getKafedraId().isBlank()) {
            throw new RuntimeException("Kafedra ID bo'sh bo'lishi mumkin emas");
        } else {
            Kafedra kafedra = kafedraRepository.findById(dto.getKafedraId())
                    .orElseThrow(() -> new RuntimeException("Kafedra topilmadi: " + dto.getKafedraId()));
            plan.setKafedra(kafedra);
        }

        // Fan tekshiruvi va yangilash
        if (dto.getFanId() == null || dto.getFanId().isBlank()) {
            throw new RuntimeException("Fan ID bo'sh bo'lishi mumkin emas");
        } else {
            Fan fan = fanRepository.findById(dto.getFanId())
                    .orElseThrow(() -> new RuntimeException("Fan topilmadi: " + dto.getFanId()));
            plan.setFan(fan);
        }

        return toPlanResponse(eduPlanRepository.save(plan));
    }
    public void deletePlan(String planId) {
        findPlanByIdOrThrow(planId);
        eduPlanTopicRepository.deleteByPlanId(planId);
        eduPlanRepository.deleteById(planId);
    }

    public List<EduPlanTopicResponseDto> getTopicsByPlanId(String planId) {
        findPlanByIdOrThrow(planId);
        return eduPlanTopicRepository.findByPlanIdOrderByTrAsc(planId)
                .stream()
                .map(this::toTopicResponse)
                .toList();
    }

    public EduPlanTopicResponseDto getTopicById(String planId, String topicId) {
        findPlanByIdOrThrow(planId);
        return toTopicResponse(findTopicByIdOrThrow(planId, topicId));
    }

    public EduPlanTopicResponseDto createTopic(String planId, EduPlanTopicRequestDto dto) {
        findPlanByIdOrThrow(planId);
        EduPlanTopic topic = new EduPlanTopic();
        topic.setPlanId(planId);
        topic.setTr(requirePositive(dto.getTr(), "t/r bo'sh bo'lishi yoki 0 bo'lishi mumkin emas"));
        topic.setName(requireNonBlank(dto.getName(), "Mavzu nomi bo'sh bo'lishi mumkin emas"));
        topic.setSoat(requirePositive(dto.getSoat(), "Soat bo'sh bo'lishi yoki 0 bo'lishi mumkin emas"));
        topic.setType(requireNonBlank(dto.getType(), "Mavzu type bo'sh bo'lishi mumkin emas"));
        return toTopicResponse(eduPlanTopicRepository.save(topic));
    }

    public List<EduPlanTopicResponseDto> createTopics(String planId, List<EduPlanTopicRequestDto> topicDtos) {
        findPlanByIdOrThrow(planId);
        if (topicDtos == null || topicDtos.isEmpty()) {
            throw new RuntimeException("Mavzular ro'yxati bo'sh bo'lishi mumkin emas");
        }
        List<EduPlanTopic> topics = topicDtos.stream()
                .map(dto -> {
                    EduPlanTopic topic = new EduPlanTopic();
                    topic.setPlanId(planId);
                    topic.setTr(requirePositive(dto.getTr(), "t/r bo'sh bo'lishi yoki 0 bo'lishi mumkin emas"));
                    topic.setName(requireNonBlank(dto.getName(), "Mavzu nomi bo'sh bo'lishi mumkin emas"));
                    topic.setSoat(requirePositive(dto.getSoat(), "Soat bo'sh bo'lishi yoki 0 bo'lishi mumkin emas"));
                    topic.setType(requireNonBlank(dto.getType(), "Mavzu type bo'sh bo'lishi mumkin emas"));
                    return topic;
                })
                .toList();
        return eduPlanTopicRepository.saveAll(topics)
                .stream()
                .map(this::toTopicResponse)
                .toList();
    }

    public EduPlanTopicResponseDto updateTopic(String planId, String topicId, EduPlanTopicRequestDto dto) {
        findPlanByIdOrThrow(planId);
        EduPlanTopic topic = findTopicByIdOrThrow(planId, topicId);
        topic.setTr(requirePositive(dto.getTr(), "t/r bo'sh bo'lishi yoki 0 bo'lishi mumkin emas"));
        topic.setName(requireNonBlank(dto.getName(), "Mavzu nomi bo'sh bo'lishi mumkin emas"));
        topic.setSoat(requirePositive(dto.getSoat(), "Soat bo'sh bo'lishi yoki 0 bo'lishi mumkin emas"));
        topic.setType(requireNonBlank(dto.getType(), "Mavzu type bo'sh bo'lishi mumkin emas"));
        return toTopicResponse(eduPlanTopicRepository.save(topic));
    }

    public void deleteTopic(String planId, String topicId) {
        findPlanByIdOrThrow(planId);
        EduPlanTopic topic = findTopicByIdOrThrow(planId, topicId);
        eduPlanTopicRepository.deleteById(topic.getId());
    }

    private EduPlan findPlanByIdOrThrow(String planId) {
        return eduPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan topilmadi: " + planId));
    }

    private EduPlanTopic findTopicByIdOrThrow(String planId, String topicId) {
        return eduPlanTopicRepository.findByIdAndPlanId(topicId, planId)
                .orElseThrow(() -> new RuntimeException("Mavzu topilmadi: " + topicId));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new RuntimeException(message);
        }
        return value;
    }

    private EduPlanResponseDto toPlanResponse(EduPlan plan) {
        List<EduPlanTopicResponseDto> topics = eduPlanTopicRepository.findByPlanIdOrderByTrAsc(plan.getId())
                .stream()
                .map(this::toTopicResponse)
                .toList();
        CatalogItemResponseDto fanDto = plan.getFan() != null ? CatalogItemResponseDto.builder()
                .id(plan.getFan().getId())
                .name(plan.getFan().getName())
                .build() : null;
        CatalogItemResponseDto kafedraDto = plan.getKafedra() != null ? CatalogItemResponseDto.builder()
                .id(plan.getKafedra().getId())
                .name(plan.getKafedra().getName())
                .build() : null;
        return EduPlanResponseDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .createAt(plan.getCreateAt())
                .updateAt(plan.getUpdateAt())
                .fan(fanDto)
                .kafedra(kafedraDto)
                .oquvYili(plan.getOquvYili())
                .topics(topics)
                .build();
    }

    private EduPlanTopicResponseDto toTopicResponse(EduPlanTopic topic) {
        return EduPlanTopicResponseDto.builder()
                .planId(topic.getPlanId())
                .id(topic.getId())
                .tr(topic.getTr())
                .name(topic.getName())
                .soat(topic.getSoat())
                .type(topic.getType())
                .build();
    }
}
