package uz.falconmobile.bilim_scan.eduplan.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "edu_plan_topics")
public class EduPlanTopic {
    @Id
    private String id;
    private String planId;
    private Integer tr;
    private String name;
    private Integer soat;
    private String type;
}
