package uz.falconmobile.bilim_scan.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.falconmobile.bilim_scan.user.dto.UserResponseDto;
import uz.falconmobile.bilim_scan.user.model.User;

@Data
@AllArgsConstructor
public class TokenResponseDto {
    private String token;
    private UserResponseDto user; // Odatda "Bearer"
}