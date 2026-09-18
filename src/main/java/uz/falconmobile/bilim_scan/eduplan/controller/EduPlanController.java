package uz.falconmobile.bilim_scan.eduplan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanRequestDto;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanResponseDto;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanTopicRequestDto;
import uz.falconmobile.bilim_scan.eduplan.dto.EduPlanTopicResponseDto;
import uz.falconmobile.bilim_scan.eduplan.service.EduPlanService;

import java.util.List;

@RestController
@RequestMapping("/api/edu-plans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN','TEACHER')")
public class EduPlanController {
    private final EduPlanService eduPlanService;

    @GetMapping
    public List<EduPlanResponseDto> getAllPlans() {
        return eduPlanService.getAllPlans();
    }

    @GetMapping("/{planId}")
    public EduPlanResponseDto getPlanById(@PathVariable String planId) {
        return eduPlanService.getPlanById(planId);
    }

    @PostMapping
    public EduPlanResponseDto createPlan(@RequestBody EduPlanRequestDto dto) {
        return eduPlanService.createPlan(dto);
    }

    @PutMapping("/{planId}")
    public EduPlanResponseDto updatePlan(@PathVariable String planId, @RequestBody EduPlanRequestDto dto) {
        return eduPlanService.updatePlan(planId, dto);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<Boolean> deletePlan(@PathVariable String planId) {
        eduPlanService.deletePlan(planId);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/{planId}/topics")
    public List<EduPlanTopicResponseDto> getTopicsByPlanId(@PathVariable String planId) {
        return eduPlanService.getTopicsByPlanId(planId);
    }

    @GetMapping("/{planId}/topics/{topicId}")
    public EduPlanTopicResponseDto getTopicById(@PathVariable String planId, @PathVariable String topicId) {
        return eduPlanService.getTopicById(planId, topicId);
    }

    @PostMapping("/{planId}/topics")
    public EduPlanTopicResponseDto createTopic(@PathVariable String planId, @RequestBody EduPlanTopicRequestDto dto) {
        return eduPlanService.createTopic(planId, dto);
    }

    @PostMapping("/{planId}/topics/bulk")
    public List<EduPlanTopicResponseDto> createTopics(
            @PathVariable String planId,
            @RequestBody List<EduPlanTopicRequestDto> topics
    ) {
        return eduPlanService.createTopics(planId, topics);
    }

    @PutMapping("/{planId}/topics/{topicId}")
    public EduPlanTopicResponseDto updateTopic(
            @PathVariable String planId,
            @PathVariable String topicId,
            @RequestBody EduPlanTopicRequestDto dto
    ) {
        return eduPlanService.updateTopic(planId, topicId, dto);
    }

    @DeleteMapping("/{planId}/topics/{topicId}")
    public ResponseEntity<Boolean> deleteTopic(@PathVariable String planId, @PathVariable String topicId) {
        eduPlanService.deleteTopic(planId, topicId);
        return ResponseEntity.ok(true);
    }
}
