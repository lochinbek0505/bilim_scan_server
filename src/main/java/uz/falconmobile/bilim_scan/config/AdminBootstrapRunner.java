package uz.falconmobile.bilim_scan.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.falconmobile.bilim_scan.user.model.Role;
import uz.falconmobile.bilim_scan.user.model.User;
import uz.falconmobile.bilim_scan.user.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {
    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            return;
        }

        String username = properties.getUsername();
        String password = properties.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_ENABLED=true bo'lsa, BOOTSTRAP_ADMIN_USERNAME va BOOTSTRAP_ADMIN_PASSWORD ham berilishi shart"
            );
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin bootstrap o'tkazib yuborildi: ADMIN foydalanuvchi allaqachon mavjud");
            return;
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Admin bootstrap xatoligi: username band - " + username);
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.warn("Bootstrap orqali ADMIN foydalanuvchi yaratildi: {}. Ishga tushirgandan keyin BOOTSTRAP_ADMIN_ENABLED=false qiling.", username);
    }
}
