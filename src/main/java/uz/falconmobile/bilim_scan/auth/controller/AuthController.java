package uz.falconmobile.bilim_scan.auth.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.security.JwtUtil;
import uz.falconmobile.bilim_scan.user.model.User;
import uz.falconmobile.bilim_scan.user.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Foydalanuvchi topilmadi"));

        if (passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        }
        return "Parol noto'g'ri!";
    }

}