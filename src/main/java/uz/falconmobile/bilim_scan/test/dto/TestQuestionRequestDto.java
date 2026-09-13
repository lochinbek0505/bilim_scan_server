package uz.falconmobile.bilim_scan.test.dto;

import lombok.Data;
import uz.falconmobile.bilim_scan.test.model.QuestionType;

import java.util.List;

@Data
public class TestQuestionRequestDto {
    private Integer tr;
    private String title;
    private String topicId;
    private QuestionType type;
    private List<Integer> relatedQuestionTrs; // ID lar emas, TR lar keladi
    private List<TestOptionDto> options;
}
