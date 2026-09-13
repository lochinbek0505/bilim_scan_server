package uz.falconmobile.bilim_scan.exam.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.exam.model.StudentExam;

import java.util.List;

public interface StudentExamRepository extends MongoRepository<StudentExam, String> {

    // Talabaning ma'lum bir imtihon sessiyasidagi barcha urinishlarini topish
    List<StudentExam> findByExamSessionIdAndStudentId(String examSessionId, String studentId);

    // TO'G'RILANGAN QISM: Bitta talabaning bir nechta imtihoni bo'lishi mumkinligi uchun List qaytaradi
    List<StudentExam> findByStudentId(String studentId);

}