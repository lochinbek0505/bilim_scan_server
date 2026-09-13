package uz.falconmobile.bilim_scan.user.dto;

import lombok.Builder;
import lombok.Data;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;
import uz.falconmobile.bilim_scan.catalog.dto.FanResponseDto;
import uz.falconmobile.bilim_scan.catalog.dto.GuruhResponseDto;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.model.Guruh;
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
    private CatalogItemResponseDto bosqich;
    private Guruh guruh;
    private CatalogItemResponseDto kafedra;
    private Fan fan;
}
