package uz.falconmobile.bilim_scan.catalog.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "bosqichlar")
public class Bosqich {
    @Id
    private String id;
    private String name;
}
