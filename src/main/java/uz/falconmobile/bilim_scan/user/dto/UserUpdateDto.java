package uz.falconmobile.bilim_scan.user.dto;

import lombok.Data;
import uz.falconmobile.bilim_scan.user.model.Role;

@Data
public class UserUpdateDto {
    private String password;
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
