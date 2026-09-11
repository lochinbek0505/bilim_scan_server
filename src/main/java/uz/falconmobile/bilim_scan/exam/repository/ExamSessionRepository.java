package uz.falconmobile.bilim_scan.exam.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uz.falconmobile.bilim_scan.exam.model.ExamSession;

public interface ExamSessionRepository extends MongoRepository<ExamSession, String> {
}
