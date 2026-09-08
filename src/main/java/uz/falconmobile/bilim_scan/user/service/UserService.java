package uz.falconmobile.bilim_scan.user.service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.user.dto.UserCreateDto;
import uz.falconmobile.bilim_scan.user.model.User;
import uz.falconmobile.bilim_scan.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String createUser(UserCreateDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Foydalanuvchi allaqachon mavjud: " + dto.getUsername());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // Parolni shifrlash
        user.setRole(dto.getRole());

        userRepository.save(user);
        return "Foydalanuvchi muvaffaqiyatli yaratildi!";
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}