package uz.falconmobile.bilim_scan.user.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.user.model.Role;

@Builder
@Data
public class UserResponseDto {
    private String id;
    private String username;
    private Role role;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String profileImageUrl;
    private String bosqichId;
    private String guruhId;
    private String kafedraId;
    private String fanId;
}
