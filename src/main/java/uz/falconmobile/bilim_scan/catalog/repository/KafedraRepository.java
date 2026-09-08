package uz.falconmobile.bilim_scan.catalog.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;

public interface KafedraRepository extends MongoRepository<Kafedra, String> {
}
