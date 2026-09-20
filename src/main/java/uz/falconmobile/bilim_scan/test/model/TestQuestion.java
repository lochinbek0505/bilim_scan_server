package uz.falconmobile.bilim_scan.test.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlanTopic;

import java.util.List;

@Data
@Document(collection = "test_questions")
public class TestQuestion {

    @Id
    private String id;
    private int tr;
    private String testId;
    private String title;
    private EduPlanTopic mavzu;
    private QuestionType type;
    private Double minimumTime;
    private List<String> relatedQuestionIds;
    private List<TestOption> options;
}
