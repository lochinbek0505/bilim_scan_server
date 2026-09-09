package uz.falconmobile.bilim_scan.eduplan.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;

import java.time.Instant;

@Data
@Document(collection = "edu_plans")
public class EduPlan {
    @Id
    private String id;
    private String name;
    private Instant createAt;
    private Instant updateAt;
    private Fan fan;
    private Kafedra kafedra;
    private String oquvYili;
    private String oquvOyi;
}
