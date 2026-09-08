package uz.falconmobile.bilim_scan.user.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


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
    private String bosqichId;
    private String guruhId;
    private String kafedraId;
    private String fanId;
}