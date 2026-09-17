package uz.falconmobile.bilim_scan.test.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.test.model.EduTest;

import java.util.List;

public interface EduTestRepository extends MongoRepository<EduTest, String> {
    List<EduTest> findByFanId(String fanId);
    List<EduTest> findByKafedraId(String kafedraId);
    List<EduTest> findByFanIdAndKafedraId(String fanId, String kafedraId);
}