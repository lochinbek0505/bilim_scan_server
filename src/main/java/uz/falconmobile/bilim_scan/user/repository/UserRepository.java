package uz.falconmobile.bilim_scan.user.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import uz.falconmobile.bilim_scan.user.model.Role;
import uz.falconmobile.bilim_scan.user.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByRole(Role role);
}