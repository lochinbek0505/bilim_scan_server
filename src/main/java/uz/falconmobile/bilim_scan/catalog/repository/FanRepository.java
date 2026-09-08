package uz.falconmobile.bilim_scan.catalog.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.catalog.model.Fan;

public interface FanRepository extends MongoRepository<Fan, String> {
}
