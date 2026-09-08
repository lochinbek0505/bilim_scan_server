package uz.falconmobile.bilim_scan.user.dto;

import lombok.Data;
import uz.falconmobile.bilim_scan.user.model.Role;

@Data
public class UserCreateDto {
    private String username;
    private String password;
    private Role role;
}