package uz.falconmobile.bilim_scan.eduplan.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.eduplan.model.EduPlanTopic;

import java.util.List;
import java.util.Optional;

public interface EduPlanTopicRepository extends MongoRepository<EduPlanTopic, String> {
    List<EduPlanTopic> findByPlanIdOrderByTrAsc(String planId);

    Optional<EduPlanTopic> findByIdAndPlanId(String id, String planId);

    void deleteByPlanId(String planId);
}
