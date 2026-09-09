package uz.falconmobile.bilim_scan.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.auth.dto.LoginRequestDto;
import uz.falconmobile.bilim_scan.auth.dto.TokenResponseDto;
import uz.falconmobile.bilim_scan.security.JwtUtil;
import uz.falconmobile.bilim_scan.user.dto.UserResponseDto;
import uz.falconmobile.bilim_scan.user.model.Role;
import uz.falconmobile.bilim_scan.user.model.User;
import uz.falconmobile.bilim_scan.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public TokenResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Login yoki parol noto'g'ri!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Login yoki parol noto'g'ri!");
        }


        UserResponseDto userResponse = UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .profileImageUrl(user.getProfileImageUrl())
                .bosqich(user.getBosqichId())
                .guruh(user.getGuruhId())
                .kafedra(user.getKafedraId())
                .fan(user.getFanId())
                .build();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new TokenResponseDto(token, userResponse);
    }
}