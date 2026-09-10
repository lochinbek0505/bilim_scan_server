package uz.falconmobile.bilim_scan.test.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.test.model.TestQuestion;

import java.util.List;
import java.util.Optional;

public interface TestQuestionRepository extends MongoRepository<TestQuestion, String> {
    List<TestQuestion> findByTestId(String testId);

    Optional<TestQuestion> findByIdAndTestId(String id, String testId);

    boolean existsByIdAndTestId(String id, String testId);

    void deleteByTestId(String testId);
}
