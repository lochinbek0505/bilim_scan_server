package uz.falconmobile.bilim_scan.test.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.test.model.EduTest;

public interface EduTestRepository extends MongoRepository<EduTest, String> {
}
