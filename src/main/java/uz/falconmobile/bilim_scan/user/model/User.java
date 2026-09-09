package uz.falconmobile.bilim_scan.user.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;


@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String password;
    private Role role;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String profileImageUrl;
    private CatalogItemResponseDto bosqichId;
    private CatalogItemResponseDto guruhId;
    private CatalogItemResponseDto kafedraId;
    private CatalogItemResponseDto fanId;
}