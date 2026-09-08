package uz.falconmobile.bilim_scan.catalog.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.catalog.model.Guruh;

public interface GuruhRepository extends MongoRepository<Guruh, String> {
}
