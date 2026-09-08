package uz.falconmobile.bilim_scan.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.auth.dto.LoginRequestDto;
import uz.falconmobile.bilim_scan.auth.dto.TokenResponseDto;
import uz.falconmobile.bilim_scan.auth.service.AuthService;
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
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        TokenResponseDto tokenResponse = authService.login(loginRequestDto);
        return ResponseEntity.ok(tokenResponse);
    }

}