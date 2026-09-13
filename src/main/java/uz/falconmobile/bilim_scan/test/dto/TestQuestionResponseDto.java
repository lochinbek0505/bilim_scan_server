package uz.falconmobile.bilim_scan.test.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlanTopic;
import uz.falconmobile.bilim_scan.test.model.QuestionType;

import java.util.List;

@Builder
@Data
public class TestQuestionResponseDto {
    private String id;
    private String testId;
    private String title;
    private EduPlanTopic mavzu;
    private QuestionType type;
    private List<String> relatedQuestionIds;
    private List<TestOptionDto> options;
}
