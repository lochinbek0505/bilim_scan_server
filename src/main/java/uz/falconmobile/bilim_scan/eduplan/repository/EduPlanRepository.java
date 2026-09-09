package uz.falconmobile.bilim_scan.eduplan.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlan;

public interface EduPlanRepository extends MongoRepository<EduPlan, String> {
}
