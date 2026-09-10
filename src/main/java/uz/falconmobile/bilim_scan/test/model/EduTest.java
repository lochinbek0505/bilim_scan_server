package uz.falconmobile.bilim_scan.test.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlan;

import java.time.Instant;

@Data
@Document(collection = "tests")
public class EduTest {
    @Id
    private String id;
    private String name;
    private Instant createAt;
    private Instant updateAt;
    private Fan fan;
    private Kafedra kafedra;
    private EduPlan oquvReja;
    private Integer ajratilganVaqt;
}
