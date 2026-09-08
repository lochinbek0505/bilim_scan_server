package uz.falconmobile.bilim_scan.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenResponseDto {
    private String token;
    private String type; // Odatda "Bearer"
}